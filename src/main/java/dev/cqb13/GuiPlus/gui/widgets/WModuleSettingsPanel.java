package dev.cqb13.GuiPlus.gui.widgets;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public class WModuleSettingsPanel extends WContainer {
    private static final int VIEW_HEIGHT_OFFSET = 128;
    private static final int DESCRIPTION_MAX_WIDTH = 600;
    private static final int MIN_PANEL_WIDTH = 300;
    private static final int MIN_PANEL_HEIGHT = 200;
    private static final int OUTLINE_THICKNESS = 2;
    private static final int PANEL_PADDING = 8;

    private final Module module;

    private WContainer settingsContainer;
    private WKeybind keybind;
    private WCheckbox active;

    private boolean subscribed = true;

    public WModuleSettingsPanel(Module module) {
        this.module = module;

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void cleanup() {
        if (subscribed) {
            MeteorClient.EVENT_BUS.unsubscribe(this);
            subscribed = false;
        }
    }

    @Override
    public void init() {
        clear();

        WView view = (WView) theme.view();
        view.theme = theme;
        view.maxHeight = getWindowHeight() - theme.scale(VIEW_HEIGHT_OFFSET);
        view.scrollOnlyWhenMouseOver = true;
        add(view).expandX();

        WVerticalList list = new WVerticalList();
        list.theme = theme;
        view.add(list).expandX().padTop(theme.scale(PANEL_PADDING)).padBottom(theme.scale(PANEL_PADDING));

        WHorizontalList titleList = new WHorizontalList();
        titleList.theme = theme;
        list.add(titleList).expandX();

        WFavorite fav = (WFavorite) theme.favorite(module.favorite);
        fav.action = () -> module.favorite = fav.checked;
        titleList.add(fav);

        titleList.add(theme.label(module.title, true));

        list.add(theme.label(module.description, DESCRIPTION_MAX_WIDTH));

        if (module.addon != null && module.addon != MeteorClient.ADDON) {
            WHorizontalList addon = list.add(theme.horizontalList()).expandX().widget();
            addon.add(theme.label("From: ").color(theme.textSecondaryColor())).widget();
            addon.add(theme.label(module.addon.name).color(module.addon.color)).widget();
        }

        if (!module.settings.groups.isEmpty()) {
            settingsContainer = list.add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(module.settings)).expandX();
        }

        WWidget widget = module.getWidget(theme);
        if (widget != null) {
            list.add(theme.horizontalSeparator()).expandX();
            Cell<WWidget> cell = list.add(widget);
            if (widget instanceof WContainer)
                cell.expandX();
        }

        WSection section = list.add(theme.section("Bind", true)).expandX().widget();

        WHorizontalList bind = section.add(theme.horizontalList()).expandX().widget();
        bind.add(theme.label("Bind: "));
        keybind = bind.add(theme.keybind(module.keybind)).expandX().widget();
        keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);

        WButton reset = bind.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
        reset.action = keybind::resetBind;
        reset.tooltip = "Reset";

        WHorizontalList tobr = section.add(theme.horizontalList()).widget();
        tobr.add(theme.label("Toggle on bind release: "));
        WCheckbox tobrC = tobr.add(theme.checkbox(module.toggleOnBindRelease)).widget();
        tobrC.action = () -> module.toggleOnBindRelease = tobrC.checked;

        WHorizontalList cf = section.add(theme.horizontalList()).widget();
        cf.add(theme.label("Chat Feedback: "));
        WCheckbox cfC = cf.add(theme.checkbox(module.chatFeedback)).widget();
        cfC.action = () -> module.chatFeedback = cfC.checked;

        list.add(theme.horizontalSeparator()).expandX();

        WHorizontalList bottom = list.add(theme.horizontalList()).expandX().widget();
        bottom.add(theme.label("Active: "));
        active = bottom.add(theme.checkbox(module.isActive())).expandCellX().widget();
        active.action = () -> {
            if (module.isActive() != active.checked)
                module.toggle();
        };

        WHorizontalList sharing = bottom.add(theme.horizontalList()).right().widget();
        WButton copy = sharing.add(theme.button(GuiRenderer.COPY)).widget();
        copy.action = () -> {
            if (toClipboard()) {
                OkPrompt.create()
                        .title("Module copied!")
                        .message("The settings for this module are now in your clipboard.")
                        .message("You can also copy settings using Ctrl+C.")
                        .message("Settings can be imported using Ctrl+V or the paste button.")
                        .id("config-sharing-guide")
                        .show();
            }
        };
        copy.tooltip = "Copy config";

        WButton paste = sharing.add(theme.button(GuiRenderer.PASTE)).widget();
        paste.action = this::fromClipboard;
        paste.tooltip = "Paste config";
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme mgt = (MeteorGuiTheme) theme;
        Color bgColor = mgt.backgroundColor.get();
        Color outlineColor = mgt.outlineColor.get();

        double s = theme.scale(OUTLINE_THICKNESS);
        renderer.quad(x + s, y + s, width - s * 2, height - s * 2, bgColor);
        renderer.quad(x, y, width, s, outlineColor);
        renderer.quad(x, y + height - s, width, s, outlineColor);
        renderer.quad(x, y + s, s, height - s * 2, outlineColor);
        renderer.quad(x + width - s, y + s, s, height - s * 2, outlineColor);

        return super.render(renderer, mouseX, mouseY, delta);
    }

    public void tick() {
        if (settingsContainer != null) {
            module.settings.tick(settingsContainer, theme);
        }
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        if (keybind != null)
            keybind.reset();
    }

    @EventHandler
    private void onActiveModulesChanged(ActiveModulesChangedEvent event) {
        if (active != null)
            active.checked = module.isActive();
    }

    public boolean toClipboard() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", module.name);
        CompoundTag settingsTag = module.settings.toTag();
        if (!settingsTag.isEmpty())
            tag.put("settings", settingsTag);
        return NbtUtils.toClipboard(tag);
    }

    public boolean fromClipboard() {
        CompoundTag tag = NbtUtils.fromClipboard();
        if (tag == null)
            return false;
        if (!tag.getStringOr("name", "").equals(module.name))
            return false;

        Optional<CompoundTag> settings = tag.getCompound("settings");
        if (settings.isPresent())
            module.settings.fromTag(settings.get());
        else
            module.settings.reset();

        return true;
    }

    @Override
    protected void onCalculateSize() {
        if (cells.isEmpty()) {
            width = 0;
            height = 0;
            return;
        }

        Cell<?> cell = cells.get(0);
        double pad = theme.scale(PANEL_PADDING);
        double minWidth = theme.scale(MIN_PANEL_WIDTH);
        double maxHeight = getWindowHeight() - theme.scale(VIEW_HEIGHT_OFFSET);

        width = Math.max(minWidth, cell.widget().width + pad * 2);
        height = Math.min(maxHeight, Math.max(theme.scale(MIN_PANEL_HEIGHT), cell.widget().height + pad * 2));
    }

    @Override
    protected void onCalculateWidgetPositions() {
        if (cells.isEmpty()) {
            return;
        }

        Cell<?> cell = cells.get(0);
        double pad = theme.scale(PANEL_PADDING);
        cell.x = x + pad;
        cell.y = y + pad;
        cell.width = width - pad * 2;
        cell.height = height - pad * 2;
        cell.alignWidget();
    }
}
