package dev.cqb13.GuiPlus.gui.widgets;

import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WModuleGrid extends WContainer {
    private final List<Module> modules = new ArrayList<>();
    private double itemHeight;
    private double horizontalSpacing = 6;
    private double verticalSpacing = 6;
    private ViewMode viewMode = ViewMode.Normal;
    private int maxColumns = 10;
    private int cachedColumns = -1;
    private double cachedWidth = -1;

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

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
        rebuildWidgets();
        invalidate();
    }

    public void setSpacing(double horizontal, double vertical) {
        this.horizontalSpacing = horizontal;
        this.verticalSpacing = vertical;
        invalidate();
    }

    public void setMaxColumns(int maxColumns) {
        this.maxColumns = maxColumns;
        invalidate();
    }

    @Override
    public void init() {
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clear();
        for (Module module : modules) {
            WModuleGridItem item = new WModuleGridItem(module, itemHeight, viewMode);
            item.onRightClick = () -> {
                if (onModuleRightClick != null) {
                    onModuleRightClick.accept(module);
                }
            };
            add(item);
        }
    }

    private int getItemWidthMultiplier() {
        return switch (viewMode) {
            case Compact -> 2;
            case Detailed -> 6;
            case List -> 1;
            case Normal -> 4;
        };
    }

    private int calculateColumns(double availableWidth) {
        if (availableWidth == cachedWidth && cachedColumns >= 0) {
            return cachedColumns;
        }

        double hSp = theme.scale(horizontalSpacing);

        if (viewMode == ViewMode.List) {
            cachedColumns = 1;
            cachedWidth = availableWidth;
            return 1;
        }

        double itemWidth = itemHeight * getItemWidthMultiplier();
        int maxCols = (viewMode == ViewMode.Compact) ? Math.min(8, maxColumns) : maxColumns;
        cachedColumns = Math.min(maxCols, Math.max(1, (int) ((availableWidth + hSp) / (itemWidth + hSp))));
        cachedWidth = availableWidth;
        return cachedColumns;
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
        int columns = calculateColumns(width);

        double itemWidth;
        double startX;

        if (viewMode == ViewMode.List) {
            itemWidth = width;
            startX = x;
        } else {
            itemWidth = itemHeight * getItemWidthMultiplier();
            double totalGridWidth = columns * itemWidth + (columns - 1) * hSp;
            startX = x + (width - totalGridWidth) / 2.0;
        }

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
