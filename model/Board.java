package model;

import util.Log;

import java.awt.Point;
import java.util.List;

/**
 * 棋盘模型，负责管理方块碰撞、选择与移动逻辑
 */
public class Board {
    private final Log log = Log.getInstance();
    private final int rows;
    private final int cols;
    private final List<Block> blocks;
    private final List<Point> victoryCells;
    private Block focused;

    public Board(GameMap model) {
        this.blocks = model.getBlocks();
        this.victoryCells = model.getVictoryCells();
        this.rows = model.getRows();
        this.cols = model.getCols();
    }

    public Block getBlockAt(Point cell) {
        for (Block b : blocks) {
            for (Point p : b.getOccupiedCells()) {
                if (p.equals(cell)) return b;
            }
        }
        return null;
    }

    public boolean moveBlock(Block b, Block.Direction dir) {
        Point next = b.getNextPosition(dir);
        if (next.x < 0 || next.y < 0 || next.x + b.getSize().height > rows ||
                next.y + b.getSize().width > cols) return false;
        for (Block other : blocks) {
            if (other == b) continue;
            for (Point p : other.getOccupiedCells()) {
                for (Point q : b.getOccupiedCells()) {
                    Point moved = new Point(q.x + (next.x - b.getPosition().x),
                            q.y + (next.y - b.getPosition().y));
                    if (p.equals(moved)) return false;
                }
            }
        }
        b.setPosition(next);
        if (isVictory()) {
            log.info("Victory");
        }
        return true;
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
     * 判断胜利：当仅有的一个 LARGE 方块的所有占据格
     * 完全位于胜利区时才算胜利
     */
    public boolean isVictory() {
        // 寻找 LARGE 方块
        for (Block b : blocks) {
            if (b.getType() == Block.Type.LARGE) {
                List<Point> occ = b.getOccupiedCells();
                // 检查所有 occupied 单元是否都在 victoryCells 中
                for (Point p : occ) {
                    if (!victoryCells.contains(p)) {
                        return false;
                    }
                }
                // 且 victoryCells 中不要求全被覆盖，只要大方块完全在其中
                return true;
            }
        }
        return false;
    }
}
