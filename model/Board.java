package model;

import util.Log;

import java.awt.Point;
import java.util.*;

/**
 * 棋盘模型，负责管理方块碰撞、选择与移动逻辑，并支持重玩/撤销
 */
public class Board {
    private final Log log = Log.getInstance();
    private final int rows;
    private final int cols;
    private final List<Block> blocks;
    private Deque<MoveEntry> history = new ArrayDeque<>();
    private  Map<Integer, Block> blockMap = new HashMap<>();
    private final List<Point> victoryCells;
    private final List<Point> initialPositions;       // 保存初始位置
    private Block focused;
    private final GameMap model;

    /** 表示一次移动记录 */
    public static class MoveEntry {
        final int blockId;
        final Block.Direction dir;
        public MoveEntry(int blockId, Block.Direction dir) {
            this.blockId = blockId;
            this.dir = dir;
        }

        public int getBlockId() {
            return blockId;
        }

        public Block.Direction getDir() {
            return dir;
        }
    }

    public Board(GameMap model) {
        this.model = model;
        this.blocks = model.getBlocks();
        this.rows = model.getRows();
        this.cols = model.getCols();
        this.victoryCells = model.getVictoryCells();
        for (Block b : blocks) {
            blockMap.put(b.getId(), b);
        }
        // 记录初始位置
        this.initialPositions = new ArrayList<>();
        for (Block b : blocks) {
            initialPositions.add(b.getPosition());
        }
    }

    /** 用来做深拷贝的构造器 */
    private Board(int rows, int cols, List<Block> blocks, List<Point> victoryCells) {
        this.model = null;
        this.rows = rows;
        this.cols = cols;
        this.blocks = blocks;
        this.victoryCells = victoryCells;
        this.history = new ArrayDeque<>();
        this.blockMap = new HashMap<>();
        for (Block b : blocks) this.blockMap.put(b.getId(), b);
        // 把 blocks 当前位置作为“初始位置”
        this.initialPositions = new ArrayList<>();
        for (Block b : blocks) this.initialPositions.add(new Point(b.getPosition()));
    }

    public Block getBlockAt(Point cell) {
        for (Block b : blocks) {
            for (Point p : b.getOccupiedCells()) {
                if (p.equals(cell)) return b;
            }
        }
        return null;
    }

    /** 获取当前所有方块的坐标列表（用于存档） */
    public List<Point> getPositions() {
        List<Point> pos = new ArrayList<>();
        for (Block b : blocks) {
            pos.add(new Point(b.getPosition()));
        }
        return pos;
    }

    /** 批量设置方块坐标（用于读档） */
    public void setPositions(List<Point> newPos) {
        for (int i = 0; i < blocks.size() && i < newPos.size(); i++) {
            blocks.get(i).setPosition(new Point(newPos.get(i)));
        }
    }

    /**
     * 移动前存历史，移动后检测胜利
     */
    public boolean moveBlock(Block b, Block.Direction dir) {

        history.push(new MoveEntry(b.getId(), dir));

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

    private Block.Direction opposite(Block.Direction dir) {
        return switch (dir) {
            case UP -> Block.Direction.DOWN;
            case DOWN -> Block.Direction.UP;
            case LEFT -> Block.Direction.RIGHT;
            case RIGHT -> Block.Direction.LEFT;
        };
    }

    public boolean undo() {
        if (history.isEmpty()) return false;
        MoveEntry last = history.pop();
        Block b = blockMap.get(last.blockId);
        if (b != null) {
            // 反方向移动以撤销
            b.setPosition(b.getNextPosition(opposite(last.dir)));
            return true;
        }
        return false;
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
        // 深拷贝所有 Block
        List<Block> cloned = new ArrayList<>();
        for (Block b : this.blocks) {
            cloned.add(b.copy());
        }
        // victoryCells 本身是只读的坐标集合，复用即可
        return new Board(this.rows, this.cols, cloned, this.victoryCells);
    }

    /**
     * 返回完整的移动历史列表
     */
    public List<MoveEntry> getHistory() {
        List<MoveEntry> list = new ArrayList<>(history);
        Collections.reverse(list);    // 把【最新→最旧】反成【最旧→最新】
        return list;
    }
}