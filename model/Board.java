package model;

import util.Log;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 棋盘模型，负责管理方块碰撞、选择与移动逻辑，并支持重玩/撤销
 */
public class Board {
    private final Log log = Log.getInstance();
    private final int rows;
    private final int cols;
    private final List<Block> blocks;
    private final List<Point> victoryCells;
    private final List<Point> initialPositions;       // 保存初始位置
    private final Deque<List<Point>> history = new ArrayDeque<>(); // 操作历史
    private Block focused;
    private final GameMap model;

    public Board(GameMap model) {
        this.model = model;
        this.blocks = model.getBlocks();
        this.victoryCells = model.getVictoryCells();
        this.rows = model.getRows();
        this.cols = model.getCols();
        // 记录初始位置
        this.initialPositions = new ArrayList<>();
        for (Block b : blocks) {
            initialPositions.add(b.getPosition());
        }
    }

    public Block getBlockAt(Point cell) {
        for (Block b : blocks) {
            for (Point p : b.getOccupiedCells()) {
                if (p.equals(cell)) return b;
            }
        }
        return null;
    }

    /**
     * 移动前存历史，移动后检测胜利
     */
    public boolean moveBlock(Block b, Block.Direction dir) {
        // 记录快照
        List<Point> snapshot = new ArrayList<>();
        for (Block blk : blocks) {
            snapshot.add(blk.getPosition());
        }
        history.push(snapshot);

        Point next = b.getNextPosition(dir);
        if (next.x < 0 || next.y < 0 || next.x + b.getSize().height > rows ||
                next.y + b.getSize().width > cols) {
            history.pop(); // 越界则不计入历史
            return false;
        }
        for (Block other : blocks) {
            if (other == b) continue;
            for (Point p : other.getOccupiedCells()) {
                for (Point q : b.getOccupiedCells()) {
                    Point moved = new Point(
                            q.x + (next.x - b.getPosition().x),
                            q.y + (next.y - b.getPosition().y)
                    );
                    if (p.equals(moved)) {
                        history.pop(); // 碰撞则不计入历史
                        return false;
                    }
                }
            }
        }
        b.setPosition(next);
        if (isVictory()) {
            log.info("Victory");
        }
        return true;
    }

    /** 撤销上一步 */
    public boolean undo() {
        if (history.isEmpty()) return false;
        List<Point> prev = history.pop();
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setPosition(prev.get(i));
        }
        return true;
    }

    /** 重玩：回到初始状态，清空历史 */
    public void reset() {
        history.clear();
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setPosition(initialPositions.get(i));
        }
        focused = null;
    }

    public void setFocused(Block b) { this.focused = b; }
    public Block getFocused() { return focused; }

    public void moveFocus(Block.Direction dir) {
        if (focused == null) return;
        Point target = focused.getNextPosition(dir);
        Block b = getBlockAt(target);
        if (b != null) focused = b;
    }

    public List<Block> getBlocks() { return blocks; }
    public int getCols() { return cols; }
    public int getRows() { return rows; }
    public List<Point> getVictoryCells() { return victoryCells; }

    /**
     * 判断胜利
     */
    public boolean isVictory() {
        for (Block b : blocks) {
            if (b.getType() == Block.Type.LARGE) {
                for (Point p : b.getOccupiedCells()) {
                    if (!victoryCells.contains(p)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public Board copy() {
        return new Board(model);
    }
}