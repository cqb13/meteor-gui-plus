package dev.cqb13.GuiPlus.gui.widgets;

import dev.cqb13.GuiPlus.gui.GuiPlusTheme;
import dev.cqb13.GuiPlus.util.ModuleUsageTracker;
import meteordevelopment.meteorclient.MeteorClient;
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
    private static final double HOVER_ANIMATION_SPEED = 4;
    private static final double ACTIVE_ANIMATION_SPEED = 6;
    private static final int OUTLINE_THICKNESS = 2;
    private static final int TEXT_PADDING = 4;
    private static final String ELLIPSIS = "...";

    private final Module module;
    private double itemHeight;
    private ViewMode viewMode;

    private double fillAnimation;
    private double activeBarAnimation;

    public Runnable onRightClick;

    public WModuleGridItem(Module module, double itemHeight, ViewMode viewMode) {
        this.module = module;
        this.itemHeight = itemHeight;
        this.viewMode = viewMode;
        this.tooltip = module.description;

        if (module.isActive()) {
            fillAnimation = 1;
            activeBarAnimation = 1;
        }
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
        fillAnimation += delta * HOVER_ANIMATION_SPEED * ((module.isActive() || mouseOver) ? 1 : -1);
        fillAnimation = Mth.clamp(fillAnimation, 0, 1);

        activeBarAnimation += delta * ACTIVE_ANIMATION_SPEED * (module.isActive() ? 1 : -1);
        activeBarAnimation = Mth.clamp(activeBarAnimation, 0, 1);

        MeteorGuiTheme mgt = (MeteorGuiTheme) theme;
        Color defaultBgColor = mgt.backgroundColor.get(false, mouseOver);
        Color outlineColor = mgt.outlineColor.get(false, mouseOver);
        Color moduleBgColor = mgt.moduleBackground.get();
        Color accentColor = mgt.accentColor.get();

        Color backgroundColor = getBackgroundColor(defaultBgColor, mgt);

        double s = theme.scale(OUTLINE_THICKNESS);
        renderer.quad(x + s, y + s, width - s * 2, height - s * 2, backgroundColor);
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

        if (fillAnimation > 0) {
            renderer.quad(x, y, width * fillAnimation, height, moduleBgColor);
        }
        if (activeBarAnimation > 0) {
            renderer.quad(x, y + height * (1 - activeBarAnimation), theme.scale(OUTLINE_THICKNESS),
                    height * activeBarAnimation,
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

    private Color getBackgroundColor(Color defaultBgColor, MeteorGuiTheme mgt) {
        if (!(theme instanceof GuiPlusTheme gpt) || !gpt.categoryColors.get()) {
            return defaultBgColor;
        }

        if (module.addon != null && module.addon != MeteorClient.ADDON) {
            return new Color(module.addon.color.r, module.addon.color.g, module.addon.color.b, gpt.addonOpacity.get());
        }

        return switch (module.category.name) {
            case "Combat" -> gpt.combatColor.get();
            case "Player" -> gpt.playerColor.get();
            case "Movement" -> gpt.movementColor.get();
            case "Render" -> gpt.renderColor.get();
            case "World" -> gpt.worldColor.get();
            case "Misc" -> gpt.miscColor.get();
            default -> defaultBgColor;
        };
    }

    private void renderStandardCard(GuiRenderer renderer, MeteorGuiTheme mgt, double pad, double maxTextWidth) {
        double textY = y + height / 2.0 - theme.textHeight() / 2.0;

        String title = truncateText(module.title, maxTextWidth);
        double textWidth = theme.textWidth(title);
        double textX = calculateTextX(textWidth, pad, mgt.moduleAlignment.get());

        renderer.text(title, textX, textY, theme.textColor(), false);
    }

    private void renderDetailedCard(GuiRenderer renderer, double pad, double maxTextWidth) {
        double textHeight = theme.textHeight();
        double lineHeight = textHeight + theme.scale(2);

        String title = truncateText(module.title, maxTextWidth);
        renderer.text(title, x + pad, y + pad, theme.textColor(), false);

        if (module.description == null || module.description.isEmpty())
            return;

        double descY = y + pad + lineHeight;
        double availableHeight = height - pad * 2 - lineHeight;
        int maxLines = Math.min((int) (availableHeight / lineHeight), 2);

        String[] words = module.description.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int linesRendered = 0;
        int wordIndex = 0;

        while (wordIndex < words.length && linesRendered < maxLines) {
            String word = words[wordIndex];
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;

            if (theme.textWidth(testLine) > maxTextWidth) {
                String line = currentLine.toString();
                boolean isLastLine = linesRendered == maxLines - 1;
                boolean hasMoreWords = wordIndex < words.length;

                if (isLastLine && hasMoreWords) {
                    line = truncateText(line, maxTextWidth);
                }

                renderer.text(line, x + pad, descY + linesRendered * lineHeight, theme.textSecondaryColor(), false);
                linesRendered++;
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
            wordIndex++;
        }

        if (currentLine.length() > 0 && linesRendered < maxLines) {
            renderer.text(currentLine.toString(), x + pad, descY + linesRendered * lineHeight,
                    theme.textSecondaryColor(), false);
        }
    }

    private String truncateText(String text, double maxWidth) {
        if (theme.textWidth(text) <= maxWidth)
            return text;

        while (theme.textWidth(text + ELLIPSIS) > maxWidth && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ELLIPSIS;
    }

    private double calculateTextX(double textWidth, double pad, AlignmentX alignment) {
        return switch (alignment) {
            case Center -> x + (width - textWidth) / 2.0;
            case Right -> x + width - pad - textWidth;
            default -> x + pad;
        };
    }
}
