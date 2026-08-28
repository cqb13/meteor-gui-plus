package dev.cqb13.GuiPlus.gui;

import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import net.minecraft.client.gui.screens.Screen;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class GuiPlusTheme extends MeteorGuiTheme {
    private static final Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GuiPlusTheme() {
        super();
        // everything is fine...
        try {
            Field nameField = meteordevelopment.meteorclient.gui.GuiTheme.class.getDeclaredField("name");
            long offset = unsafe.objectFieldOffset(nameField);
            unsafe.putObject(this, offset, "GUI+");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
