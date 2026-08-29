package dev.cqb13.GuiPlus.gui.widgets;

import dev.cqb13.GuiPlus.gui.GuiPlusTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class WCategorySidebar extends WContainer {
    private static final int BUTTON_SPACING = 2;
    private static final int OUTLINE_THICKNESS = 2;
    private static final int BUTTON_PADDING = 6;
    private static final int EXTRA_PADDING = 4;
    private static final double ANIMATION_SPEED = 16;
    private static final int ACCENT_BAR_WIDTH = 2;
    private static final int ICON_SPACING = 2;
    private static final double ICON_SCALE_DIVISOR = 16.0;

    private final List<SidebarButton> buttons = new ArrayList<>();
    private int selected = 0;

    public Runnable onSelectionChanged;

    public void addButton(String label, Category category, int moduleCount) {
        SidebarButton button = new SidebarButton(label, category, moduleCount);
        buttons.add(button);
        super.add(button);
    }

    @Override
    public void init() {
        for (SidebarButton button : buttons) {
            button.theme = this.theme;
        }
    }

    public void setSelected(int index) {
        if (index >= 0 && index < buttons.size()) {
            selected = index;
        }
    }

    public int getSelected() {
        return selected;
    }

    private Color blendColors(Color from, Color to, double progress) {
        return new Color(
                (int) (from.r + (to.r - from.r) * progress),
                (int) (from.g + (to.g - from.g) * progress),
                (int) (from.b + (to.b - from.b) * progress),
                (int) (from.a + (to.a - from.a) * progress));
    }

    @Override
    protected void onCalculateSize() {
        width = 0;
        height = 0;

        double sp = theme.scale(BUTTON_SPACING);
        for (int i = 0; i < cells.size(); i++) {
            Cell<?> cell = cells.get(i);
            if (i > 0)
                height += sp;
            width = Math.max(width, cell.widget().width);
            height += cell.widget().height;
        }
    }

    @Override
    protected void onCalculateWidgetPositions() {
        double sp = theme.scale(BUTTON_SPACING);
        double y = this.y;

        for (int i = 0; i < cells.size(); i++) {
            Cell<?> cell = cells.get(i);
            if (i > 0)
                y += sp;

            cell.x = this.x;
            cell.y = y;
            cell.width = width;
            cell.height = cell.widget().height;
            cell.alignWidget();

            y += cell.height;
        }
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double s = theme.scale(OUTLINE_THICKNESS);

        MeteorGuiTheme mgt = (MeteorGuiTheme) theme;
        Color bgColor = mgt.backgroundColor.get();
        Color outlineColor = mgt.outlineColor.get();

        renderer.quad(x + s, y + s, width - s * 2, height - s * 2, bgColor);
        renderer.quad(x, y, width, s, outlineColor);
        renderer.quad(x, y + height - s, width, s, outlineColor);
        renderer.quad(x, y + s, s, height - s * 2, outlineColor);
        renderer.quad(x + width - s, y + s, s, height - s * 2, outlineColor);

        return super.render(renderer, mouseX, mouseY, delta);
    }

    public class SidebarButton extends WPressable {
        public final String label;
        public final Category category;
        public final int moduleCount;
        private double animProgress;
        private int cachedIndex = -1;

        public SidebarButton(String label, Category category, int moduleCount) {
            this.label = label;
            this.category = category;
            this.moduleCount = moduleCount;
        }

        private int getIndex() {
            if (cachedIndex < 0) {
                cachedIndex = buttons.indexOf(this);
            }
            return cachedIndex;
        }

        @Override
        protected void onCalculateSize() {
            double pad = theme.scale(BUTTON_PADDING);
            boolean showCounts = theme instanceof GuiPlusTheme gpt && gpt.showModuleCounts.get();

            width = theme.textWidth(label) + pad * 2 + theme.scale(EXTRA_PADDING);
            height = theme.textHeight() + pad * 2;

            if (showCounts && category != null) {
                String countText = "(" + moduleCount + ")";
                width += theme.textWidth(countText) + theme.scale(4);
            }

            if (theme.categoryIcons()) {
                width += height;
            }
        }

        @Override
        protected void onCalculateWidgetPositions() {
            super.onCalculateWidgetPositions();
            if (parent != null) {
                width = parent.width;
            }
        }

        @Override
        protected void onPressed(int button) {
            int index = getIndex();
            if (index >= 0) {
                selected = index;
                if (onSelectionChanged != null)
                    onSelectionChanged.run();
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            boolean isSelected = getIndex() == selected;

            double targetAnim = (isSelected || mouseOver) ? 1 : 0;
            animProgress += delta * ANIMATION_SPEED * (targetAnim - animProgress);
            animProgress = Mth.clamp(animProgress, 0, 1);

            if (animProgress < 0.01)
                animProgress = 0;

            MeteorGuiTheme mgt = (MeteorGuiTheme) theme;
            Color bgColor = mgt.backgroundColor.get();
            Color hoverBg = mgt.backgroundColor.get(false, true);
            Color accentColor = mgt.accentColor.get();

            if (animProgress > 0) {
                Color blended = blendColors(bgColor, hoverBg, animProgress);
                renderer.quad(x, y, width, height, blended);
            }

            if (isSelected) {
                renderer.quad(x, y, theme.scale(ACCENT_BAR_WIDTH), height, accentColor);
            }

            double pad = theme.scale(BUTTON_PADDING);
            double textX = x + pad + theme.scale(ACCENT_BAR_WIDTH);

            if (theme.categoryIcons() && category != null) {
                net.minecraft.world.item.ItemStack icon = category.icon.get();
                if (!icon.isEmpty()) {
                    double iconSize = height - pad * 2;
                    renderer.item(icon, (int) textX, (int) (y + pad), (float) (iconSize / ICON_SCALE_DIVISOR), false);
                    textX += iconSize + theme.scale(ICON_SPACING);
                }
            }

            renderer.text(label, textX, y + height / 2.0 - theme.textHeight() / 2.0,
                    theme.textColor(), false);

            boolean showCounts = theme instanceof GuiPlusTheme gpt && gpt.showModuleCounts.get();
            if (showCounts && category != null) {
                String countText = "(" + moduleCount + ")";
                double countWidth = theme.textWidth(countText);
                double countX = x + width - countWidth - pad;
                renderer.text(countText, countX, y + height / 2.0 - theme.textHeight() / 2.0,
                        theme.textSecondaryColor(), false);
            }
        }
    }
}
