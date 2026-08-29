package dev.cqb13.GuiPlus.gui;

import dev.cqb13.GuiPlus.gui.widgets.WCategorySidebar;
import dev.cqb13.GuiPlus.gui.widgets.WModuleGrid;
import dev.cqb13.GuiPlus.gui.widgets.WModuleSettingsPanel;
import dev.cqb13.GuiPlus.gui.widgets.SortMode;
import dev.cqb13.GuiPlus.gui.widgets.ViewMode;
import dev.cqb13.GuiPlus.util.ModuleUsageTracker;
import com.mojang.blaze3d.platform.MacosUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.NonNull;

import meteordevelopment.meteorclient.gui.utils.Cell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static com.mojang.blaze3d.platform.InputConstants.*;

public class GuiPlusModulesScreen extends TabScreen {
    private static final double SIDEBAR_WIDTH_FRACTION = 0.2;
    private static final int TOP_BAR_HEIGHT = 40;
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int MAX_RECENT_MODULES = 20;

    private static final List<String> searchHistory = new ArrayList<>();
    private static final Map<String, FilterState> tabFilterStates = new HashMap<>();

    private record FilterState(boolean active, boolean favorites, boolean unbound) {
    }

    private WCategorySidebar sidebar;
    private WTextBox searchBox;
    private String searchText = "";
    private Module selectedModule;
    private WModuleSettingsPanel settingsPanel;

    private final List<CategoryEntry> categories = new ArrayList<>();
    private boolean isSearchView = true;
    private boolean isFavoritesView = false;
    private boolean isRecentView = false;

    private WModuleGrid moduleGrid;
    private WVerticalList contentView;
    private WView gridScrollView;

    private WCheckbox activeCheckbox;
    private WCheckbox favoritesCheckbox;
    private WCheckbox unboundCheckbox;
    private WVerticalList historyDropdown;
    private List<String> currentHistorySuggestions = new ArrayList<>();
    private String currentTabKey = "search";

    public GuiPlusModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    private String getCurrentTabKey() {
        if (isSearchView)
            return "search";
        if (isFavoritesView)
            return "favorites";
        if (isRecentView)
            return "recent";
        CategoryEntry entry = getSelectedCategoryEntry();
        if (entry != null && entry.category != null)
            return entry.category.name;
        return "unknown";
    }

    private void saveCurrentFilterState() {
        if (activeCheckbox == null || favoritesCheckbox == null || unboundCheckbox == null)
            return;
        tabFilterStates.put(currentTabKey, new FilterState(
                activeCheckbox.checked, favoritesCheckbox.checked, unboundCheckbox.checked));
    }

    private void loadFilterState() {
        currentTabKey = getCurrentTabKey();
        FilterState state = tabFilterStates.get(currentTabKey);
        boolean active = state != null && state.active();
        boolean favorites = state != null && state.favorites();
        boolean unbound = state != null && state.unbound();

        if (activeCheckbox != null)
            activeCheckbox.checked = active;
        if (favoritesCheckbox != null)
            favoritesCheckbox.checked = favorites;
        if (unboundCheckbox != null)
            unboundCheckbox.checked = unbound;
    }

    private boolean matchesSearch(Module m) {
        if (searchText.isEmpty())
            return true;
        String searchLower = searchText.toLowerCase();
        return m.title.toLowerCase().contains(searchLower) || m.description.toLowerCase().contains(searchLower);
    }

    private boolean matchesTitleSearch(Module m) {
        if (searchText.isEmpty())
            return true;
        return m.title.toLowerCase().contains(searchText.toLowerCase());
    }

    private boolean passesQuickFilters(Module m) {
        boolean hasActiveFilter = (activeCheckbox != null && activeCheckbox.checked) ||
                (favoritesCheckbox != null && favoritesCheckbox.checked) ||
                (unboundCheckbox != null && unboundCheckbox.checked);

        if (!hasActiveFilter)
            return true;

        boolean matchesActive = activeCheckbox != null && activeCheckbox.checked && m.isActive();
        boolean matchesFavorites = favoritesCheckbox != null && favoritesCheckbox.checked && m.favorite;
        boolean matchesUnbound = unboundCheckbox != null && unboundCheckbox.checked && !m.keybind.isSet();

        return matchesActive || matchesFavorites || matchesUnbound;
    }

    @Override
    public void initWidgets() {
        categories.clear();

        categories.add(new CategoryEntry("Search", null, true, false, false));

        List<Category> defaultCategories = List.of(
                Categories.Combat, Categories.Player, Categories.Movement,
                Categories.Render, Categories.World, Categories.Misc);

        for (Category category : defaultCategories) {
            if (hasVisibleModules(category)) {
                categories.add(new CategoryEntry(category.name, category, false, false, false));
            }
        }

        List<Category> addonCategories = new ArrayList<>();
        for (Category category : Modules.loopCategories()) {
            if (!defaultCategories.contains(category)) {
                if (hasVisibleModules(category)) {
                    addonCategories.add(category);
                }
            }
        }

        addonCategories.sort(this::compareCategoriesByAddon);

        for (Category category : addonCategories) {
            categories.add(new CategoryEntry(category.name, category, false, false, false));
        }

        categories.add(new CategoryEntry("Favorites", null, false, true, false));
        categories.add(new CategoryEntry("Recent", null, false, false, true));

        WLayout layout = new WLayout();
        addDirect(layout).expandX().expandWidgetY();
    }

    private int compareCategoriesByAddon(Category c1, Category c2) {
        String addon1 = getAddonNameForCategory(c1);
        String addon2 = getAddonNameForCategory(c2);
        int addonCompare = addon1.compareToIgnoreCase(addon2);
        if (addonCompare != 0)
            return addonCompare;
        return c1.name.compareToIgnoreCase(c2.name);
    }

    private boolean hasVisibleModules(Category category) {
        for (Module module : Modules.get().getGroup(category)) {
            if (!Config.get().hiddenModules.get().contains(module)) {
                return true;
            }
        }
        return false;
    }

    private String getAddonNameForCategory(Category category) {
        for (Module module : Modules.get().getGroup(category)) {
            if (module.addon != null) {
                return module.addon.name;
            }
        }
        return "";
    }

    private void buildContent() {
        if (contentView == null)
            return;

        if (selectedModule != null) {
            buildSettingsView();
        } else {
            buildGridView();
        }
    }

    private void buildGridView() {
        if (settingsPanel != null) {
            settingsPanel.cleanup();
        }
        selectedModule = null;
        settingsPanel = null;

        if (contentView == null)
            return;

        contentView.visible = true;

        WContainer parentLayout = (WContainer) contentView.parent;
        if (parentLayout != null && parentLayout.cells.size() > 2) {
            parentLayout.remove(parentLayout.cells.get(2));
        }

        contentView.clear();

        GuiPlusTheme gpt = theme instanceof GuiPlusTheme g ? g : null;
        double scale = gpt != null ? gpt.scale.get() : 1.0;
        int moduleHeight = gpt != null ? gpt.moduleHeight.get() : 30;
        ViewMode viewMode = gpt != null ? gpt.viewMode.get() : ViewMode.Normal;
        int hSpacing = gpt != null ? gpt.horizontalSpacing.get() : 6;
        int vSpacing = gpt != null ? gpt.verticalSpacing.get() : 6;
        int maxColumns = gpt != null ? gpt.maxColumns.get() : 10;

        double itemHeight = theme.scale(moduleHeight * scale);

        searchBox = contentView.add(theme.textBox(searchText, "Search...")).expandX().widget();
        searchBox.action = () -> {
            searchText = searchBox.get();
            updateHistoryDropdown();
            refreshGrid();
        };

        WHorizontalList filterRow = contentView.add(theme.horizontalList()).expandX().widget();

        filterRow.add(theme.label("Active"));
        activeCheckbox = filterRow.add(theme.checkbox(false)).widget();
        activeCheckbox.action = this::refreshGrid;

        filterRow.add(theme.label("Favorites"));
        favoritesCheckbox = filterRow.add(theme.checkbox(false)).widget();
        favoritesCheckbox.action = this::refreshGrid;

        filterRow.add(theme.label("Unbound"));
        unboundCheckbox = filterRow.add(theme.checkbox(false)).widget();
        unboundCheckbox.action = this::refreshGrid;

        loadFilterState();

        filterRow.add(theme.button("Clear")).widget().action = () -> {
            activeCheckbox.checked = false;
            favoritesCheckbox.checked = false;
            unboundCheckbox.checked = false;
            refreshGrid();
        };

        historyDropdown = new WVerticalList();
        historyDropdown.visible = false;
        contentView.add(historyDropdown).expandX();

        gridScrollView = (WView) theme.view();
        gridScrollView.maxHeight = Double.MAX_VALUE;
        gridScrollView.scrollOnlyWhenMouseOver = true;
        gridScrollView.hasScrollBar = true;
        contentView.add(gridScrollView).expandX().expandWidgetY();

        double windowWidth = getWindowWidth();
        double sidebarWidth = Math.round(windowWidth * SIDEBAR_WIDTH_FRACTION);
        double contentWidth = windowWidth - sidebarWidth;

        moduleGrid = new WModuleGrid(itemHeight);
        moduleGrid.width = contentWidth;
        moduleGrid.setViewMode(viewMode);
        moduleGrid.setSpacing(theme.scale(hSpacing), theme.scale(vSpacing));
        moduleGrid.setMaxColumns(maxColumns);
        moduleGrid.onModuleRightClick = this::selectModule;
        gridScrollView.add(moduleGrid).expandX();

        refreshGrid();

        if (isSearchView) {
            searchBox.setFocused(true);
        }
    }

    private void refreshGrid() {
        if (moduleGrid == null)
            return;
        moduleGrid.setModules(getFilteredModules());
    }

    private List<Module> getFilteredModules() {
        List<Module> result = new ArrayList<>();

        if (isSearchView) {
            for (Module m : Modules.get().getAll()) {
                if (matchesSearch(m))
                    result.add(m);
            }
        } else if (isFavoritesView) {
            for (Module m : Modules.get().getAll()) {
                if (m.favorite && matchesTitleSearch(m))
                    result.add(m);
            }
        } else if (isRecentView) {
            for (Module m : ModuleUsageTracker.getRecentlyUsedModules(MAX_RECENT_MODULES)) {
                if (matchesTitleSearch(m))
                    result.add(m);
            }
        } else {
            CategoryEntry entry = getSelectedCategoryEntry();
            if (entry != null && entry.category != null) {
                for (Module m : Modules.get().getGroup(entry.category)) {
                    if (!Config.get().hiddenModules.get().contains(m) && matchesTitleSearch(m)) {
                        result.add(m);
                    }
                }
            }
        }

        result.removeIf(m -> !passesQuickFilters(m));

        SortMode sortMode = SortMode.Alphabetical;
        if (theme instanceof GuiPlusTheme gpt) {
            sortMode = gpt.sortMode.get();
        }

        switch (sortMode) {
            case MostUsed:
                result.sort(Comparator.comparingInt((Module m) -> ModuleUsageTracker.getUseCount(m)).reversed());
                break;
            case RecentlyUsed:
                result.sort(Comparator.comparingLong((Module m) -> ModuleUsageTracker.getLastUsed(m)).reversed());
                break;
            case Alphabetical:
            default:
                result.sort(Comparator.comparing(m -> m.title.toLowerCase()));
                break;
        }

        return result;
    }

    private void updateHistoryDropdown() {
        if (historyDropdown == null)
            return;

        boolean historyEnabled = theme instanceof GuiPlusTheme gpt && gpt.enableSearchHistory.get();

        if (!historyEnabled || searchText.isEmpty()) {
            historyDropdown.visible = false;
            historyDropdown.clear();
            currentHistorySuggestions.clear();
            return;
        }

        String searchLower = searchText.toLowerCase();
        currentHistorySuggestions = searchHistory.stream()
                .filter(h -> h.toLowerCase().contains(searchLower))
                .limit(5)
                .collect(Collectors.toList());

        historyDropdown.clear();

        if (currentHistorySuggestions.isEmpty()) {
            historyDropdown.visible = false;
            return;
        }

        for (String suggestion : currentHistorySuggestions) {
            var button = historyDropdown.add(theme.button(suggestion)).expandX().widget();
            button.action = () -> {
                searchText = suggestion;
                searchBox.set(suggestion);
                addToHistory(suggestion);
                historyDropdown.visible = false;
                refreshGrid();
            };
        }

        historyDropdown.visible = true;
    }

    private void addToHistory(String search) {
        if (search.isEmpty())
            return;

        searchHistory.remove(search);
        searchHistory.add(0, search);

        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            searchHistory.remove(searchHistory.size() - 1);
        }
    }

    private CategoryEntry getSelectedCategoryEntry() {
        if (sidebar == null)
            return null;

        int idx = sidebar.getSelected();
        if (idx >= 0 && idx < categories.size()) {
            return categories.get(idx);
        }

        return null;
    }

    private void buildSettingsView() {
        if (contentView == null || selectedModule == null)
            return;

        contentView.clear();
        contentView.visible = false;

        settingsPanel = new WModuleSettingsPanel(selectedModule);

        WCenteredContainer centered = new WCenteredContainer(settingsPanel);

        WContainer parentLayout = (WContainer) contentView.parent;
        if (parentLayout != null) {
            parentLayout.add(centered).expandX().expandWidgetY();
        }
    }

    public void selectModule(Module module) {
        if (settingsPanel != null) {
            settingsPanel.cleanup();
        }
        selectedModule = module;
        ModuleUsageTracker.recordUsage(module);
        buildContent();
    }

    @Override
    public void tick() {
        if (settingsPanel != null)
            settingsPanel.tick();
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent value) {
        if (locked)
            return false;

        boolean cntrl = MacosUtil.IS_MACOS ? value.modifiers() == MOD_SUPER : value.modifiers() == MOD_CONTROL;

        if (cntrl && value.key() == KEY_F) {
            if (searchBox != null) {
                searchBox.setFocused(true);
                searchBox.setCursorMax();
            }
            return true;
        }

        if (value.key() == KEY_RETURN && searchBox != null && searchBox.isFocused()) {
            if (!currentHistorySuggestions.isEmpty()) {
                searchText = currentHistorySuggestions.get(0);
                searchBox.set(searchText);
                addToHistory(searchText);
                historyDropdown.visible = false;
                refreshGrid();
                return true;
            } else if (!searchText.isEmpty()) {
                addToHistory(searchText);
                historyDropdown.visible = false;
                return true;
            }
        }

        if (value.key() == KEY_ESCAPE && selectedModule != null) {
            if (settingsPanel != null) {
                settingsPanel.cleanup();
            }
            selectedModule = null;
            settingsPanel = null;
            buildContent();
            return true;
        }

        return super.keyPressed(value);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {
        clear();
        initWidgets();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        reload();
    }

    @Override
    protected void init() {
        boolean wasFirstInit = firstInit;
        super.init();

        if (!wasFirstInit) {
            reload();
        }
    }

    @Override
    protected void onClosed() {
        if (settingsPanel != null) {
            settingsPanel.cleanup();
            settingsPanel = null;
        }
    }

    private class WLayout extends WContainer {
        @Override
        public void init() {
            sidebar = new WCategorySidebar();
            sidebar.onSelectionChanged = () -> {
                saveCurrentFilterState();
                CategoryEntry entry = getSelectedCategoryEntry();
                if (entry != null) {
                    isSearchView = entry.isSearch;
                    isFavoritesView = entry.isFavorites;
                    isRecentView = entry.isRecent;
                    searchText = "";
                    selectedModule = null;
                    settingsPanel = null;
                    buildContent();
                }
            };

            for (CategoryEntry cat : categories) {
                sidebar.addButton(cat.name, cat.category);
            }
            sidebar.setSelected(0);
            add(sidebar);

            contentView = new WVerticalList();
            contentView.theme = theme;
            add(contentView).expandX().expandWidgetY();

            buildContent();
        }

        @Override
        protected void onCalculateSize() {
            width = getWindowWidth();
            height = getWindowHeight();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double sidebarW = Math.round(width * SIDEBAR_WIDTH_FRACTION);
            double contentW = width - sidebarW;

            double topBarHeight = theme.scale(TOP_BAR_HEIGHT);
            double layoutY = y + topBarHeight;
            double layoutHeight = height - topBarHeight;

            if (cells.size() >= 2) {
                Cell<?> sidebarCell = cells.get(0);
                sidebarCell.x = x;
                sidebarCell.y = layoutY;
                sidebarCell.width = sidebarW;
                sidebarCell.height = layoutHeight;
                sidebarCell.alignWidget();

                Cell<?> secondCell = cells.get(1);
                secondCell.x = x + sidebarW;
                secondCell.y = layoutY;
                secondCell.width = contentW;
                secondCell.height = layoutHeight;
                secondCell.alignWidget();

                if (cells.size() >= 3) {
                    Cell<?> centeredCell = cells.get(2);
                    centeredCell.x = x;
                    centeredCell.y = layoutY;
                    centeredCell.width = width;
                    centeredCell.height = layoutHeight;
                    centeredCell.alignWidget();
                }
            }
        }
    }

    private record CategoryEntry(String name, Category category, boolean isSearch, boolean isFavorites,
            boolean isRecent) {
    }

    private class WCenteredContainer extends WContainer {
        private final WWidget child;

        public WCenteredContainer(WWidget child) {
            this.child = child;
        }

        @Override
        public void init() {
            add(child);
        }

        @Override
        protected void onCalculateWidgetPositions() {
            if (cells.isEmpty())
                return;

            Cell<?> cell = cells.get(0);

            double widgetX = x + (width - cell.widget().width) / 2.0;
            double widgetY = y + (height - cell.widget().height) / 2.0;

            cell.widget().x = widgetX;
            cell.widget().y = widgetY;
        }
    }
}
