package dev.cqb13.GuiPlus;

import com.mojang.logging.LogUtils;

import dev.cqb13.GuiPlus.gui.GuiPlusTheme;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiThemes;
import org.slf4j.Logger;

public class GuiPlus extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing GUI+");

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
