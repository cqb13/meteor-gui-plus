package dev.cqb13.GuiPlus.gui.widgets;

import dev.cqb13.GuiPlus.gui.GuiPlusTheme;
import dev.cqb13.GuiPlus.util.ModuleUsageTracker;
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
    private static final double HOVER_ANIMATION_SPEED = 4;
    private static final double ACTIVE_ANIMATION_SPEED = 6;
    private static final int OUTLINE_THICKNESS = 2;
    private static final int TEXT_PADDING = 4;
    private static final String ELLIPSIS = "...";

    private final Module module;
    private double itemHeight;
    private ViewMode viewMode;

    private double hoverAnimation;
    private double activeAnimation;

    public Runnable onRightClick;

    public WModuleGridItem(Module module, double itemHeight, ViewMode viewMode) {
        this.module = module;
        this.itemHeight = itemHeight;
        this.viewMode = viewMode;
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
        width = parent != null ? parent.width : itemHeight * 4;
        height = itemHeight;
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) {
            module.toggle();
            ModuleUsageTracker.recordUsage(module);
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

        if (viewMode == ViewMode.Detailed) {
            renderDetailedCard(renderer, pad, maxTextWidth);
        } else {
            renderStandardCard(renderer, mgt, pad, maxTextWidth);
        }
    }

    private void renderStandardCard(GuiRenderer renderer, MeteorGuiTheme mgt, double pad, double maxTextWidth) {
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

    private void renderDetailedCard(GuiRenderer renderer, double pad, double maxTextWidth) {
        double textHeight = theme.textHeight();
        double lineHeight = textHeight + theme.scale(2);

        String title = module.title;
        if (theme.textWidth(title) > maxTextWidth) {
            while (theme.textWidth(title + ELLIPSIS) > maxTextWidth && !title.isEmpty()) {
                title = title.substring(0, title.length() - 1);
            }
            title = title + ELLIPSIS;
        }
        renderer.text(title, x + pad, y + pad, theme.textColor(), false);

        if (module.description != null && !module.description.isEmpty()) {
            double descY = y + pad + lineHeight;
            double availableHeight = height - pad * 2 - lineHeight;
            int maxLines = (int) (availableHeight / lineHeight);
            maxLines = Math.min(maxLines, 2);

            String[] words = module.description.split(" ");
            StringBuilder currentLine = new StringBuilder();
            int linesRendered = 0;

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (theme.textWidth(testLine) > maxTextWidth) {
                    if (linesRendered >= maxLines)
                        break;

                    String line = currentLine.toString();
                    if (linesRendered == maxLines - 1 && words.length > 0) {
                        int remainingWords = 0;
                        for (String w : words) {
                            if (!currentLine.toString().contains(w))
                                remainingWords++;
                        }
                        if (remainingWords > 0) {
                            while (theme.textWidth(line + ELLIPSIS) > maxTextWidth && !line.isEmpty()) {
                                line = line.substring(0, line.length() - 1);
                            }
                            line = line + ELLIPSIS;
                        }
                    }
                    renderer.text(line, x + pad, descY + linesRendered * lineHeight, theme.textSecondaryColor(), false);
                    linesRendered++;
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }

            if (currentLine.length() > 0 && linesRendered < maxLines) {
                String line = currentLine.toString();
                renderer.text(line, x + pad, descY + linesRendered * lineHeight, theme.textSecondaryColor(), false);
            }
        }
    }

    public Module getModule() {
        return module;
    }
}
