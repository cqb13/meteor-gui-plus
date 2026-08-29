package dev.cqb13.GuiPlus.gui.widgets;

import dev.cqb13.GuiPlus.gui.GuiPlusTheme;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public class WModuleGridItem extends WPressable {
    private static final int ITEM_WIDTH_MULTIPLIER = 4;
    private static final double HOVER_ANIMATION_SPEED = 4;
    private static final double ACTIVE_ANIMATION_SPEED = 6;
    private static final int OUTLINE_THICKNESS = 2;
    private static final int TEXT_PADDING = 4;
    private static final String ELLIPSIS = "...";

    private final Module module;
    private double itemHeight;

    private double hoverAnimation;
    private double activeAnimation;

    public Runnable onRightClick;

    public WModuleGridItem(Module module, double itemHeight) {
        this.module = module;
        this.itemHeight = itemHeight;
        this.tooltip = module.description;

        if (module.isActive()) {
            hoverAnimation = 1;
            activeAnimation = 1;
        }
    }

    public void setItemHeight(double itemHeight) {
        this.itemHeight = itemHeight;
        invalidate();
    }

    @Override
    protected void onCalculateSize() {
        width = itemHeight * ITEM_WIDTH_MULTIPLIER;
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
        hoverAnimation += delta * HOVER_ANIMATION_SPEED * ((module.isActive() || mouseOver) ? 1 : -1);
        hoverAnimation = Mth.clamp(hoverAnimation, 0, 1);

        activeAnimation += delta * ACTIVE_ANIMATION_SPEED * (module.isActive() ? 1 : -1);
        activeAnimation = Mth.clamp(activeAnimation, 0, 1);

        MeteorGuiTheme mgt = (MeteorGuiTheme) theme;
        Color bgColor = mgt.backgroundColor.get(false, mouseOver);
        Color outlineColor = mgt.outlineColor.get(false, mouseOver);
        Color moduleBgColor = mgt.moduleBackground.get();
        Color accentColor = mgt.accentColor.get();

        Color renderColor = bgColor;

        if (theme instanceof GuiPlusTheme gpt && gpt.categoryColors.get()) {
            Color categoryColor = null;

            if (module.addon != null && module.addon != MeteorClient.ADDON) {
                categoryColor = new Color(module.addon.color.r, module.addon.color.g, module.addon.color.b,
                        gpt.addonOpacity.get());
            } else {
                if (module.category == Categories.Combat) {
                    categoryColor = gpt.combatColor.get();
                } else if (module.category == Categories.Player) {
                    categoryColor = gpt.playerColor.get();
                } else if (module.category == Categories.Movement) {
                    categoryColor = gpt.movementColor.get();
                } else if (module.category == Categories.Render) {
                    categoryColor = gpt.renderColor.get();
                } else if (module.category == Categories.World) {
                    categoryColor = gpt.worldColor.get();
                } else if (module.category == Categories.Misc) {
                    categoryColor = gpt.miscColor.get();
                }
            }

            if (categoryColor != null) {
                renderColor = categoryColor;
            }
        }

        double s = theme.scale(OUTLINE_THICKNESS);
        renderer.quad(x + s, y + s, width - s * 2, height - s * 2, renderColor);
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

        if (hoverAnimation > 0) {
            renderer.quad(x, y, width * hoverAnimation, height, moduleBgColor);
        }
        if (activeAnimation > 0) {
            renderer.quad(x, y + height * (1 - activeAnimation), theme.scale(OUTLINE_THICKNESS),
                    height * activeAnimation,
                    accentColor);
        }

        double pad = theme.scale(TEXT_PADDING);
        double maxTextWidth = width - pad * 2;

        double textY = y + height / 2.0 - theme.textHeight() / 2.0;

        String title = module.title;
        if (theme.textWidth(title) > maxTextWidth) {
            while (theme.textWidth(title + ELLIPSIS) > maxTextWidth && !title.isEmpty()) {
                title = title.substring(0, title.length() - 1);
            }
            title = title + ELLIPSIS;
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
