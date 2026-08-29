package dev.cqb13.GuiPlus.gui;

import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screens.Screen;

public class GuiPlusTheme extends MeteorGuiTheme {
    private final SettingGroup sgGuiPlus = settings.createGroup("GUI Plus");
    private final SettingGroup sgCategoryColors = settings.createGroup("Category Colors");

    public final Setting<Boolean> enableSearchHistory = sgGuiPlus.add(new BoolSetting.Builder()
            .name("enable-search-history")
            .description("Enable search history suggestions.")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> categoryColors = sgCategoryColors.add(new BoolSetting.Builder()
            .name("category-colors")
            .description("Color code modules by their category.")
            .defaultValue(false)
            .build());

    public final Setting<Integer> addonOpacity = sgCategoryColors.add(new IntSetting.Builder()
            .name("addon-opacity")
            .description("Opacity for addon module backgrounds.")
            .defaultValue(200)
            .min(0)
            .max(255)
            .sliderMin(0)
            .sliderMax(255)
            .build());

    public final Setting<SettingColor> combatColor = sgCategoryColors.add(new ColorSetting.Builder()
            .name("combat-color")
            .description("Color for Combat modules.")
            .defaultValue(new SettingColor(255, 50, 50, 150))
            .build());

    public final Setting<SettingColor> playerColor = sgCategoryColors.add(new ColorSetting.Builder()
            .name("player-color")
            .description("Color for Player modules.")
            .defaultValue(new SettingColor(50, 50, 255, 150))
            .build());

    public final Setting<SettingColor> movementColor = sgCategoryColors.add(new ColorSetting.Builder()
            .name("movement-color")
            .description("Color for Movement modules.")
            .defaultValue(new SettingColor(255, 165, 0, 150))
            .build());

    public final Setting<SettingColor> renderColor = sgCategoryColors.add(new ColorSetting.Builder()
            .name("render-color")
            .description("Color for Render modules.")
            .defaultValue(new SettingColor(255, 255, 255, 150))
            .build());

    public final Setting<SettingColor> worldColor = sgCategoryColors.add(new ColorSetting.Builder()
            .name("world-color")
            .description("Color for World modules.")
            .defaultValue(new SettingColor(50, 255, 50, 150))
            .build());

    public final Setting<SettingColor> miscColor = sgCategoryColors.add(new ColorSetting.Builder()
            .name("misc-color")
            .description("Color for Misc modules.")
            .defaultValue(new SettingColor(139, 69, 19, 150))
            .build());

    public GuiPlusTheme() {
        super();
        ((GuiThemeAccessor) this).guiplus$setName("GUI+");
    }

    @Override
    public TabScreen modulesScreen() {
        return new GuiPlusModulesScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof GuiPlusModulesScreen;
    }
}
