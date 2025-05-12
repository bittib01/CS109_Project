package model;

import java.awt.*;

/**
 * 表示游戏中一个方块的类，支持网格逻辑和动画控制
 */
public class Block {
    // 方块类型：1×1、1×2 水平、2×1 竖直、2×2
    public enum Type { SMALL, VERTICAL, HORIZONTAL, LARGE }
    // 移动方向
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final Type type;           // 方块类型
    private Point position;            // 当前方块左上角坐标（行, 列）
    private final float inertia;       // 惯性，控制动画速度
    private final Dimension size;      // 方块在网格中的尺寸（高, 宽）

    /**
     * 构造：根据类型和起始位置初始化方块
     * @param type 方块类型
     * @param start 初始坐标（行, 列）
     */
    public Block(Type type, Point start) {
        this.type = type;
        this.position = new Point(start);
        switch (type) {
            case SMALL:
                size = new Dimension(1, 1);
                inertia = 1.0f; break;
            case VERTICAL:
                size = new Dimension(2, 1);
                inertia = 1.2f; break;
            case HORIZONTAL:
                size = new Dimension(1, 2);
                inertia = 1.2f; break;
            case LARGE:
            default:
                size = new Dimension(2, 2);
                inertia = 1.5f; break;
        }
    }

    /**
     * 获取方块占据的所有网格坐标（行, 列）
     */
    public java.util.List<Point> getOccupiedCells() {
        java.util.List<Point> cells = new java.util.ArrayList<>();
        for (int dr = 0; dr < size.height; dr++) {
            for (int dc = 0; dc < size.width; dc++) {
                cells.add(new Point(position.x + dr, position.y + dc));
            }
        }
        return cells;
    }

    /**
     * 计算按指定方向移动一个格后的位置
     * @param dir 方向
     * @return 新的左上角坐标
     */
    public Point getNextPosition(Direction dir) {
        return switch (dir) {
            case UP -> new Point(position.x - 1, position.y);
            case DOWN -> new Point(position.x + 1, position.y);
            case LEFT -> new Point(position.x, position.y - 1);
            case RIGHT -> new Point(position.x, position.y + 1);
        };
    }

    // Getter / Setter
    public Type getType() { return type; }
    public Point getPosition() { return new Point(position); }
    public void setPosition(Point p) { this.position.setLocation(p); }
    public Dimension getSize() { return new Dimension(size); }
    public float getInertia() { return inertia; }
}