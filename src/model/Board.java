package model;

import util.Log;
import java.awt.Point;
import java.util.*;

/**
 * 棋盘模型，管理方块的移动、碰撞检测、撤销、重玩及胜利判定逻辑。
 */
public class Board {
    private final Log log = Log.getInstance();
    private final int rows;
    private final int cols;
    private final List<Block> blocks;
    private final List<Point> victoryCells;
    private final List<Point> initialPositions;
    private final Map<Integer, Block> blockMap = new HashMap<>();
    private final Deque<MoveEntry> history = new ArrayDeque<>();
    private Block focused;

    /** 单次移动记录：包含方块 ID 及移动方向 */
    public record MoveEntry(int blockId, Block.Direction dir) { }

    /**
     * 根据 GameMap 初始化棋盘，记录初始状态用于 reset
     */
    public Board(GameMap model) {
        this.blocks = model.getBlocks();
        this.rows = model.getRows();
        this.cols = model.getCols();
        this.victoryCells = model.getVictoryCells();

        // 填充 blockMap 与 initialPositions
        this.initialPositions = new ArrayList<>();
        for (Block b : blocks) {
            blockMap.put(b.getId(), b);
            initialPositions.add(new Point(b.getPosition()));
        }
    }

    /**
     * 私有构造器，用于 copy() 时的深拷贝
     */
    private Board(int rows, int cols, List<Block> clonedBlocks, List<Point> victoryCells) {
        this.rows = rows;
        this.cols = cols;
        this.blocks = clonedBlocks;
        this.victoryCells = victoryCells;
        this.initialPositions = new ArrayList<>();
        for (Block b : clonedBlocks) {
            blockMap.put(b.getId(), b);
            initialPositions.add(new Point(b.getPosition()));
        }
    }

    /**
     * 在指定单元格位置查找方块
     * @param cell 目标单元格坐标
     * @return 占据该单元格的方块，若无则返回 null
     */
    public Block getBlockAt(Point cell) {
        for (Block b : blocks) {
            for (Point p : b.getOccupiedCells()) {
                if (p.equals(cell)) {
                    return b;
                }
            }
        }
        return null;
    }

    /**
     * 根据方块 ID 查找方块实例
     * @param id 方块唯一标识
     * @return 对应的 Block 对象，若不存在返回 null
     */
    public Block findBlockById(int id) {
        return blockMap.get(id);
    }

    /**
     * 将方块 b 向指定方向移动，先校验边界与碰撞，再记录历史并检测胜利
     * @return 移动成功返回 true，否则 false
     */
    public boolean moveBlock(Block b, Block.Direction dir) {
        // 记录历史
        history.push(new MoveEntry(b.getId(), dir));

        Point next = b.getNextPosition(dir);
        // 边界检查
        if (next.x < 0 || next.y < 0 || next.x + b.getSize().height > rows ||
                next.y + b.getSize().width > cols) {
            history.pop();
            return false;
        }
        // 碰撞检查
        int dx = next.x - b.getPosition().x;
        int dy = next.y - b.getPosition().y;
        for (Block other : blocks) {
            if (other == b) continue;
            for (Point p : other.getOccupiedCells()) {
                for (Point q : b.getOccupiedCells()) {
                    Point moved = new Point(q.x + dx, q.y + dy);
                    if (p.equals(moved)) {
                        history.pop();
                        return false;
                    }
                }
            }
        }
        // 应用移动
        b.setPosition(next);
        if (isVictory()) {
            log.info("Victory");
        }
        return true;
    }

    /**
     * 撤销上一次移动，仅更新位置不触发历史记录
     * @return 撤销成功返回 true，否则 false
     */
    public boolean undo() {
        if (history.isEmpty()) return false;
        MoveEntry last = history.pop();
        Block b = findBlockById(last.blockId());
        if (b != null) {
            b.setPosition(b.getNextPosition(opposite(last.dir())));
            return true;
        }
        return false;
    }

    /**
     * 重玩：将所有方块还原到初始位置，并清空历史与焦点
     */
    public void reset() {
        history.clear();
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setPosition(new Point(initialPositions.get(i)));
        }
        focused = null;
    }

    /**
     * 设置当前焦点方块，一般用于键盘导航
     */
    public void setFocused(Block b) {
        this.focused = b;
    }

    /** 获取当前焦点方块 */
    public Block getFocused() {
        return focused;
    }

    /**
     * 在当前焦点基础上按方向切换焦点方块
     */
    public void moveFocus(Block.Direction dir) {
        if (focused == null) return;
        Point tgt = focused.getNextPosition(dir);
        Block b = getBlockAt(tgt);
        if (b != null) {
            focused = b;
        }
    }

    /** 获取所有方块列表 */
    public List<Block> getBlocks() { return blocks; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public List<Point> getVictoryCells() { return Collections.unmodifiableList(victoryCells); }

    /**
     * 判断是否胜利（LARGE 类型方块完全覆盖胜利区域）
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

    /**
     * 深拷贝当前 Board，不保留历史与焦点，仅复制方块与胜利区
     */
    public Board copy() {
        List<Block> cloned = new ArrayList<>();
        for (Block b : blocks) cloned.add(b.copy());
        return new Board(rows, cols, cloned, victoryCells);
    }

    /**
     * 获取按时间顺序排列的移动历史【最旧→最新】
     */
    public List<MoveEntry> getHistory() {
        List<MoveEntry> list = new ArrayList<>(history);
        Collections.reverse(list);
        return list;
    }

    // 获取方向相反的枚举
    private Block.Direction opposite(Block.Direction dir) {
        return switch (dir) {
            case UP    -> Block.Direction.DOWN;
            case DOWN  -> Block.Direction.UP;
            case LEFT  -> Block.Direction.RIGHT;
            case RIGHT -> Block.Direction.LEFT;
        };
    }
}
