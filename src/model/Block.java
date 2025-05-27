package model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 表示游戏中一个方块的类，支持网格逻辑和动画控制
 */
public class Block {
    public enum Type { SMALL, HORIZONTAL, VERTICAL, LARGE }
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final int id;
    private final Type type;
    private Point position;
    private final float inertia;
    private final Dimension size;

    public Block(int id, Type type, Point start) {
        this.id = id;
        this.type = type;
        this.position = new Point(start);
        switch (type) {
            case SMALL:
                size = new Dimension(1, 1);
                inertia = 1.0f; break;
            case HORIZONTAL:
                size = new Dimension(2, 1);
                inertia = 1.2f; break;
            case VERTICAL:
                size = new Dimension(1, 2);
                inertia = 1.2f; break;
            case LARGE:
            default:
                size = new Dimension(2, 2);
                inertia = 1.5f; break;
        }
    }

    public List<Point> getOccupiedCells() {
        List<Point> cells = new ArrayList<>();
        for (int dr = 0; dr < size.height; dr++) {
            for (int dc = 0; dc < size.width; dc++) {
                cells.add(new Point(position.x + dr, position.y + dc));
            }
        }
        return cells;
    }

    public Point getNextPosition(Direction dir) {
        return switch (dir) {
            case UP    -> new Point(position.x - 1, position.y);
            case DOWN  -> new Point(position.x + 1, position.y);
            case LEFT  -> new Point(position.x, position.y - 1);
            case RIGHT -> new Point(position.x, position.y + 1);
        };
    }

    public int getId() {return id;}
    public Type getType() { return type; }
    public Point getPosition() { return new Point(position); }
    public void setPosition(Point p) { this.position = new Point(p); }
    public Dimension getSize() { return new Dimension(size); }
    public float getInertia() { return inertia; }

    public Block copy() {
        return new Block(this.getId(), this.getType(), new Point(this.getPosition()));
    }
}