package dev.cqb13.GuiPlus.gui.widgets;

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
    private final List<SidebarButton> buttons = new ArrayList<>();
    private int selected = 0;

    public Runnable onSelectionChanged;
    public Runnable onSettingsClicked;
    private SettingsButton settingsButton;

    public void addButton(String label, Category category) {
        SidebarButton button = new SidebarButton(label, category);
        buttons.add(button);
        super.add(button);
    }

    @Override
    public void init() {
        for (SidebarButton button : buttons) {
            button.theme = this.theme;
        }
        if (settingsButton != null) {
            settingsButton.theme = this.theme;
        }
    }

    public void addSettingsButton() {
        settingsButton = new SettingsButton();
        super.add(settingsButton);
    }

    public void setSelected(int index) {
        if (index >= 0 && index < buttons.size()) {
            selected = index;
        }
    }

    public int getSelected() {
        return selected;
    }

    @Override
    protected void onCalculateSize() {
        width = 0;
        height = 0;

        double sp = theme.scale(2);
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
        double sp = theme.scale(2);
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
        double s = theme.scale(2);

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
        private double animProgress;

        public SidebarButton(String label, Category category) {
            this.label = label;
            this.category = category;
        }

        @Override
        protected void onCalculateSize() {
            double pad = theme.scale(6);
            width = theme.textWidth(label) + pad * 2 + theme.scale(4);
            height = theme.textHeight() + pad * 2;

            if (theme.categoryIcons()) {
                width += height;
            }
        }

        @Override
        protected void onPressed(int button) {
            int index = buttons.indexOf(this);
            if (index >= 0) {
                selected = index;
                if (onSelectionChanged != null)
                    onSelectionChanged.run();
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            boolean isSelected = buttons.indexOf(this) == selected;

            double targetAnim = (isSelected || mouseOver) ? 1 : 0;
            animProgress += delta * 8 * (targetAnim - animProgress);
            animProgress = Mth.clamp(animProgress, 0, 1);

            meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme mgt = (meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme) theme;
            Color bgColor = mgt.backgroundColor.get();
            Color hoverBg = mgt.backgroundColor.get(false, true);
            Color accentColor = mgt.accentColor.get();

            if (animProgress > 0) {
                Color blended = new Color(
                        (int) (bgColor.r + (hoverBg.r - bgColor.r) * animProgress),
                        (int) (bgColor.g + (hoverBg.g - bgColor.g) * animProgress),
                        (int) (bgColor.b + (hoverBg.b - bgColor.b) * animProgress),
                        (int) (bgColor.a + (hoverBg.a - bgColor.a) * animProgress));
                renderer.quad(x, y, width, height, blended);
            }

            if (isSelected) {
                renderer.quad(x, y, theme.scale(2), height, accentColor);
            }

            double pad = theme.scale(6);
            double textX = x + pad + theme.scale(2);

            if (theme.categoryIcons() && category != null) {
                net.minecraft.world.item.ItemStack icon = category.icon.get();
                if (!icon.isEmpty()) {
                    double iconSize = height - pad * 2;
                    renderer.item(icon, (int) textX, (int) (y + pad), (float) (iconSize / 16.0), false);
                    textX += iconSize + theme.scale(2);
                }
            }

            renderer.text(label, textX, y + height / 2.0 - theme.textHeight() / 2.0,
                    theme.textColor(), false);
        }
    }

    public class SettingsButton extends WPressable {
        private double animProgress;

        public SettingsButton() {
            this.tooltip = "GUI+ Settings";
        }

        @Override
        protected void onCalculateSize() {
            double pad = theme.scale(6);
            String label = "Settings";
            width = theme.textWidth(label) + pad * 2 + theme.scale(4);
            height = theme.textHeight() + pad * 2;
        }

        @Override
        protected void onPressed(int button) {
            if (onSettingsClicked != null)
                onSettingsClicked.run();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double targetAnim = mouseOver ? 1 : 0;
            animProgress += delta * 8 * (targetAnim - animProgress);
            animProgress = Mth.clamp(animProgress, 0, 1);

            meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme mgt = (meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme) theme;
            Color bgColor = mgt.backgroundColor.get();
            Color hoverBg = mgt.backgroundColor.get(false, true);
            Color accentColor = mgt.accentColor.get();

            if (animProgress > 0) {
                Color blended = new Color(
                        (int) (bgColor.r + (hoverBg.r - bgColor.r) * animProgress),
                        (int) (bgColor.g + (hoverBg.g - bgColor.g) * animProgress),
                        (int) (bgColor.b + (hoverBg.b - bgColor.b) * animProgress),
                        (int) (bgColor.a + (hoverBg.a - bgColor.a) * animProgress));
                renderer.quad(x, y, width, height, blended);
            }

            renderer.quad(x, y, theme.scale(2), height, accentColor);

            double pad = theme.scale(6);
            String label = "Settings";
            renderer.text(label, x + pad + theme.scale(2), y + height / 2.0 - theme.textHeight() / 2.0,
                    theme.textColor(), false);
        }
    }
}
