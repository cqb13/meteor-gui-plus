package dev.cqb13.GuiPlus.gui;

import dev.cqb13.GuiPlus.gui.widgets.WCategorySidebar;
import dev.cqb13.GuiPlus.gui.widgets.WModuleGrid;
import dev.cqb13.GuiPlus.gui.widgets.WModuleSettingsPanel;
import com.mojang.blaze3d.platform.MacosUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.NonNull;

import meteordevelopment.meteorclient.gui.utils.Cell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static com.mojang.blaze3d.platform.InputConstants.*;

public class GuiPlusModulesScreen extends TabScreen {
    private WCategorySidebar sidebar;
    private WTextBox searchBox;
    private String currentSearch = "";
    private Module selectedModule;
    private WModuleSettingsPanel settingsPanel;

    private final List<CategoryEntry> categories = new ArrayList<>();
    private boolean isSearchView = true;
    private boolean isFavoritesView = false;

    private WModuleGrid moduleGrid;
    private WView contentView;
    private WVerticalList contentList;

    private static final int moduleHeight = 30;

    public GuiPlusModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        categories.clear();

        categories.add(new CategoryEntry("Search", null, true, false));

        for (Category category : Modules.loopCategories()) {
            List<Module> moduleList = new ArrayList<>();
            for (Module module : Modules.get().getGroup(category)) {
                if (!Config.get().hiddenModules.get().contains(module)) {
                    moduleList.add(module);
                }
            }
            if (!moduleList.isEmpty()) {
                categories.add(new CategoryEntry(category.name, category, false, false));
            }
        }

        categories.add(new CategoryEntry("Favorites", null, false, true));

        WLayout layout = new WLayout();
        addDirect(layout).expandX().expandWidgetY();
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

        if (contentView == null) {
            return;
        }

        contentView.visible = true;

        WContainer parentLayout = (WContainer) contentView.parent;
        if (parentLayout != null && parentLayout.cells.size() > 2) {
            parentLayout.remove(parentLayout.cells.get(2));
        }

        contentView.clear();

        contentList = new WVerticalList();
        contentView.add(contentList).expandX().expandWidgetY();

        double scale;
        if (theme instanceof GuiPlusTheme gpt) {
            scale = gpt.scale.get();
        } else {
            scale = 1.0;
        }
        double itemHeight = theme.scale(moduleHeight * scale);

        searchBox = contentList.add(theme.textBox(currentSearch, "Search...")).expandX().widget();
        searchBox.action = () -> {
            currentSearch = searchBox.get();
            refreshGrid();
        };

        moduleGrid = new WModuleGrid(itemHeight);
        moduleGrid.onModuleRightClick = (module) -> {
            selectModule(module);
        };
        contentList.add(moduleGrid).expandX();

        refreshGrid();

        if (isSearchView) {
            searchBox.setFocused(true);
        }
    }

    private void refreshGrid() {
        if (moduleGrid == null)
            return;

        List<Module> modules = getFilteredModules();
        moduleGrid.setModules(modules);
    }

    private List<Module> getFilteredModules() {
        List<Module> result = new ArrayList<>();

        if (isSearchView) {
            if (currentSearch.isEmpty()) {
                result.addAll(Modules.get().getAll());
            } else {
                String searchLower = currentSearch.toLowerCase();
                for (Module m : Modules.get().getAll()) {
                    if (m.title.toLowerCase().contains(searchLower) ||
                            m.description.toLowerCase().contains(searchLower)) {
                        result.add(m);
                    }
                }
            }
        } else if (isFavoritesView) {
            for (Module m : Modules.get().getAll()) {
                if (m.favorite)
                    result.add(m);
            }
            if (!currentSearch.isEmpty()) {
                result.removeIf(m -> !m.title.toLowerCase().contains(currentSearch.toLowerCase()));
            }
        } else {
            CategoryEntry entry = getSelectedCategoryEntry();
            if (entry != null && entry.category != null) {
                for (Module m : Modules.get().getGroup(entry.category)) {
                    if (!Config.get().hiddenModules.get().contains(m)) {
                        result.add(m);
                    }
                }
                if (!currentSearch.isEmpty()) {
                    result.removeIf(m -> !m.title.toLowerCase().contains(currentSearch.toLowerCase()));
                }
            }
        }

        result.sort(Comparator.comparing(m -> m.title.toLowerCase()));
        return result;
    }

    private CategoryEntry getSelectedCategoryEntry() {
        if (sidebar == null)
            return null;
        int idx = sidebar.getSelected();
        if (idx >= 0 && idx < categories.size())
            return categories.get(idx);
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
                CategoryEntry entry = getSelectedCategoryEntry();
                if (entry != null) {
                    isSearchView = entry.isSearch;
                    isFavoritesView = entry.isFavorites;
                    currentSearch = "";
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

            contentView = (WView) theme.view();
            contentView.maxHeight = Double.MAX_VALUE;
            contentView.scrollOnlyWhenMouseOver = true;
            contentView.hasScrollBar = true;
            add(contentView).expandX();

            buildContent();
        }

        @Override
        protected void onCalculateSize() {
            width = getWindowWidth();
            height = getWindowHeight();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double sidebarFraction = 0.2;
            double sidebarW = Math.round(width * sidebarFraction);
            double contentW = width - sidebarW;

            double topBarHeight = theme.scale(40);
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

    private record CategoryEntry(String name, Category category, boolean isSearch, boolean isFavorites) {
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
            if (cells.isEmpty()) {
                return;
            }

            Cell<?> cell = cells.get(0);

            double widgetX = x + (width - cell.widget().width) / 2.0;
            double widgetY = y + (height - cell.widget().height) / 2.0;

            cell.widget().x = widgetX;
            cell.widget().y = widgetY;
        }
    }
}
