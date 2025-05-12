package model;

import util.Log;
import util.Config;

import java.awt.Point;
import java.util.List;

/**
 * 棋盘模型，负责管理方块碰撞、选择与移动逻辑
 */
public class Board {
    Log log = Log.getInstance();
    Config config = Config.getInstance();
    private final int rows = config.getInt("rows");
    private final int cols = config.getInt("cols");
    private final List<Block> blocks;
    private final List<Point> victoryCells;
    private Block focused;

    public Board(Map model) {
        this.blocks = model.getBlocks();
        this.victoryCells = model.getVictoryCells();
    }

    /**
     * 返回指定坐标上的方块，若为空返回 null
     */
    public Block getBlockAt(Point cell) {
        for (Block b : blocks) {
            for (Point p : b.getOccupiedCells()) {
                if (p.equals(cell)) return b;
            }
        }
        return null;
    }

    /**
     * 尝试将指定方块沿方向移动一格，若可行则更新位置并返回 true
     */
    public boolean moveBlock(Block b, Block.Direction dir) {
        Point next = b.getNextPosition(dir);
        // 越界检测
        if (next.x < 0 || next.y < 0 || next.x + b.getSize().height > rows ||
                next.y + b.getSize().width > cols) return false;
        // 碰撞检测
        for (Block other : blocks) {
            if (other == b) continue;
            for (Point p : other.getOccupiedCells()) {
                for (Point q : b.getOccupiedCells()) {
                    Point moved = new Point(q.x + (next.x - b.getPosition().x), q.y + (next.y - b.getPosition().y));
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

    /**
     * 设置当前焦点方块
     */
    public void setFocused(Block b) { this.focused = b; }
    public Block getFocused() { return focused; }

    /**
     * 切换焦点到相邻方向的方块
     */
    public void moveFocus(Block.Direction dir) {
        if (focused == null) return;
        Point base = focused.getPosition();
        Point targetCell = focused.getNextPosition(dir);
        Block b = getBlockAt(targetCell);
        if (b != null) setFocused(b);
    }

    public List<Block> getBlocks() { return blocks; }

    /**
     * 判断是否胜利：
     * 当前关卡的大方块完全覆盖了所有胜利区格子
     */
    public boolean isVictory() {
        // 找到主目标方块（假设只有一个 LARGE 类型）
        Block main = null;
        for (Block b : blocks) {
            if (b.getType() == Block.Type.LARGE) {
                main = b;
                break;
            }
        }
        if (main == null) return false;

        List<Point> occ = main.getOccupiedCells();
        // 每个胜利格子都要被 main 占据
        for (Point v : victoryCells) {
            boolean covered = occ.stream().anyMatch(p -> p.equals(v));
            if (!covered) {
                return false;
            }
        }
        return true;
    }
}