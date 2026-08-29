package dev.cqb13.GuiPlus;

import com.mojang.logging.LogUtils;

import dev.cqb13.GuiPlus.gui.GuiPlusTheme;
import dev.cqb13.GuiPlus.util.ModuleUsageTracker;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;

public class GuiPlus extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing GUI+");

        File configDir = Minecraft.getInstance().gameDirectory;
        ModuleUsageTracker.init(configDir);

        GuiThemes.add(new GuiPlusTheme());
    }

    @Override
    public String getPackage() {
        return "dev.cqb13.GuiPlus";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("cqb13", "meteor-gui-plus");
    }
}
