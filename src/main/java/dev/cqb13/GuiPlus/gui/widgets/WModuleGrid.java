package dev.cqb13.GuiPlus.gui.widgets;

import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WModuleGrid extends WContainer {
    private static final double DEFAULT_SPACING = 6;
    private static final int MAX_COLUMNS = 5;
    private static final int ITEM_WIDTH_MULTIPLIER = 4;

    private final List<Module> modules = new ArrayList<>();
    private double itemHeight;
    private double horizontalSpacing = DEFAULT_SPACING;
    private double verticalSpacing = DEFAULT_SPACING;

    public Consumer<Module> onModuleRightClick;

    public WModuleGrid(double itemHeight) {
        this.itemHeight = itemHeight;
    }

    public void setModules(List<Module> modules) {
        this.modules.clear();
        this.modules.addAll(modules);
        rebuildWidgets();
        invalidate();
    }

    public void setItemHeight(double itemHeight) {
        this.itemHeight = itemHeight;
        for (Cell<?> cell : cells) {
            if (cell.widget() instanceof WModuleGridItem item) {
                item.setItemHeight(itemHeight);
            }
        }
        invalidate();
    }

    @Override
    public void init() {
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clear();
        for (Module module : modules) {
            WModuleGridItem item = new WModuleGridItem(module, itemHeight);
            item.onRightClick = () -> {
                if (onModuleRightClick != null) {
                    onModuleRightClick.accept(module);
                }
            };
            add(item);
        }
    }

    private int calculateColumns(double availableWidth) {
        double hSp = theme.scale(horizontalSpacing);
        double itemWidth = itemHeight * ITEM_WIDTH_MULTIPLIER;
        return Math.min(MAX_COLUMNS, Math.max(1, (int) ((availableWidth + hSp) / (itemWidth + hSp))));
    }

    private int calculateRows(int itemCount, int columns) {
        return (int) Math.ceil((double) itemCount / columns);
    }

    private double calculateHeight(int rows) {
        double vSp = theme.scale(verticalSpacing);
        return rows * itemHeight + Math.max(0, rows - 1) * vSp;
    }

    @Override
    protected void onCalculateSize() {
        if (cells.isEmpty()) {
            width = 0;
            height = 0;
            return;
        }

        double availableWidth = width;
        if (availableWidth <= 0) {
            WWidget p = parent;
            while (p != null && p.width <= 0) {
                p = p.parent;
            }
            if (p != null) {
                availableWidth = p.width;
            }
        }

        if (availableWidth <= 0) {
            availableWidth = 800;
        }

        int columns = calculateColumns(availableWidth);
        int rows = calculateRows(cells.size(), columns);

        width = availableWidth;
        height = calculateHeight(rows);
    }

    @Override
    protected void onCalculateWidgetPositions() {
        if (cells.isEmpty())
            return;

        double hSp = theme.scale(horizontalSpacing);
        double vSp = theme.scale(verticalSpacing);
        double itemWidth = itemHeight * ITEM_WIDTH_MULTIPLIER;

        int columns = calculateColumns(width);

        double totalGridWidth = columns * itemWidth + (columns - 1) * hSp;
        double startX = x + (width - totalGridWidth) / 2.0;
        double startY = y;

        for (int i = 0; i < cells.size(); i++) {
            int row = i / columns;
            int col = i % columns;

            Cell<?> cell = cells.get(i);

            cell.x = startX + col * (itemWidth + hSp);
            cell.y = startY + row * (itemHeight + vSp);
            cell.width = itemWidth;
            cell.height = itemHeight;

            WWidget widget = cell.widget();
            widget.x = cell.x;
            widget.y = cell.y;
            widget.width = cell.width;
            widget.height = cell.height;
        }

        int rows = calculateRows(cells.size(), columns);
        height = calculateHeight(rows);
    }
}
