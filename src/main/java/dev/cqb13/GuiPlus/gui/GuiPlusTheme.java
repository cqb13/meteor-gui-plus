package dev.cqb13.GuiPlus.gui;


import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import net.minecraft.client.gui.screens.Screen;

public class GuiPlusTheme extends MeteorGuiTheme {
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
