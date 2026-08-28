package dev.cqb13.GuiPlus.gui.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public class WModuleGridItem extends WPressable {
    private final Module module;
    private double itemHeight;

    private double animationProgress1;
    private double animationProgress2;

    public Runnable onRightClick;

    public WModuleGridItem(Module module, double itemHeight) {
        this.module = module;
        this.itemHeight = itemHeight;
        this.tooltip = module.description;

        if (module.isActive()) {
            animationProgress1 = 1;
            animationProgress2 = 1;
        }
    }

    public void setItemHeight(double itemHeight) {
        this.itemHeight = itemHeight;
        invalidate();
    }

    @Override
    protected void onCalculateSize() {
        width = itemHeight * 4;
        height = itemHeight;
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) {
            module.toggle();
        } else if (button == MOUSE_BUTTON_RIGHT) {
            if (onRightClick != null) {
                onRightClick.run();
            } else {
                mc.gui.setScreen(theme.moduleScreen(module));
            }
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        animationProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animationProgress1 = Mth.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
        animationProgress2 = Mth.clamp(animationProgress2, 0, 1);

        MeteorGuiTheme mgt = (MeteorGuiTheme) theme;
        Color bgColor = mgt.backgroundColor.get(false, mouseOver);
        Color outlineColor = mgt.outlineColor.get(false, mouseOver);
        Color moduleBgColor = mgt.moduleBackground.get();
        Color accentColor = mgt.accentColor.get();

        double s = theme.scale(2);
        renderer.quad(x + s, y + s, width - s * 2, height - s * 2, bgColor);
        renderer.quad(x, y, width, s, outlineColor);
        renderer.quad(x, y + height - s, width, s, outlineColor);
        renderer.quad(x, y + s, s, height - s * 2, outlineColor);
        renderer.quad(x + width - s, y + s, s, height - s * 2, outlineColor);

        if (module.favorite) {
            Color favColor = mgt.favoriteColor.get();
            double fs = theme.scale(1);
            double fo = s + fs;
            renderer.quad(x + fo, y + fo, width - fo * 2, fs, favColor);
            renderer.quad(x + fo, y + height - fo - fs, width - fo * 2, fs, favColor);
            renderer.quad(x + fo, y + fo + fs, fs, height - fo * 2 - fs * 2, favColor);
            renderer.quad(x + width - fo - fs, y + fo + fs, fs, height - fo * 2 - fs * 2, favColor);
        }

        if (animationProgress1 > 0) {
            renderer.quad(x, y, width * animationProgress1, height, moduleBgColor);
        }
        if (animationProgress2 > 0) {
            renderer.quad(x, y + height * (1 - animationProgress2), theme.scale(2), height * animationProgress2,
                    accentColor);
        }

        double pad = theme.scale(4);
        double maxTextWidth = width - pad * 2;

        double textY = y + height / 2.0 - theme.textHeight() / 2.0;

        String title = module.title;
        if (theme.textWidth(title) > maxTextWidth) {
            while (theme.textWidth(title + "...") > maxTextWidth && title.length() > 0) {
                title = title.substring(0, title.length() - 1);
            }
            title = title + "...";
        }

        double textWidth = theme.textWidth(title);
        double textX;

        AlignmentX alignment = mgt.moduleAlignment.get();

        if (alignment == AlignmentX.Center) {
            textX = x + (width - textWidth) / 2.0;
        } else if (alignment == AlignmentX.Right) {
            textX = x + width - pad - textWidth;
        } else {
            textX = x + pad;
        }

        renderer.text(title, textX, textY, theme.textColor(), false);
    }

    public Module getModule() {
        return module;
    }
}
