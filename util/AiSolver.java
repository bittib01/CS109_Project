package util;

import model.Board;
import model.Block;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AiSolver {

    /**
     * 表示一次移动：将某个方块从某位置沿某方向滑动一次
     */
    public static class Move {
        public final Point from;            // 起始坐标（行, 列）
        public final Block.Direction dir;   // 移动方向

        public Move(Point from, Block.Direction dir) {
            this.from = from;
            this.dir = dir;
        }

        @Override
        public String toString() {
            return String.format("Move block at (%d,%d) %s",
                    from.x, from.y, dir);
        }
    }

    /**
     * 求解给定 Board 的最短移动序列（若无解则返回 null）
     */
    public static List<Move> solve(Board initBoard) {
        // 目标板：我们只关心胜利与否，因此直接用原板 clone 然后反复乱动至胜利不现实。
        // 所以在扩展时，凡 board.isVictory() 即为目标
        String initKey = getStateKey(initBoard);

        // 双向队列 & 访问记录
        Queue<Board> q1 = new LinkedList<>(), q2 = new LinkedList<>();
        Map<String, List<Move>> d1 = new HashMap<>(), d2 = new HashMap<>();

        q1.offer(initBoard.copy());
        d1.put(initKey, new ArrayList<>());

        // 为后向搜索准备：找到任意一个胜利状态
        Board goalBoard = null;
        if (initBoard.isVictory()) {
            return new ArrayList<>();  // 已经胜利，无需移动
        }
        // 后向队列一开始留空，直到在前向过程中发现某个状态胜利时启动
        boolean goalDiscovered = false;

        while (!q1.isEmpty() && !q2.isEmpty() || !q1.isEmpty()) {
            // 扩展前向一层
            if (!q1.isEmpty()) {
                int size = q1.size();
                for (int i = 0; i < size; i++) {
                    Board cur = q1.poll();
                    String curKey = getStateKey(cur);
                    List<Move> curPath = d1.get(curKey);

                    // 遍历所有可能的移动
                    for (Block b : cur.getBlocks()) {
                        Point pos = b.getPosition();
                        for (Block.Direction dir : Block.Direction.values()) {
                            Board next = cur.copy();
                            Block onNext = next.getBlockAt(pos);
                            if (onNext == null) continue;
                            if (!next.moveBlock(onNext, dir)) continue;
                            String nextKey = getStateKey(next);
                            if (d1.containsKey(nextKey)) continue;

                            List<Move> path1 = new ArrayList<>(curPath);
                            path1.add(new Move(pos, dir));
                            d1.put(nextKey, path1);
                            q1.offer(next);

                            // 如果到达胜利态，初始化后向搜索
                            if (next.isVictory() && !goalDiscovered) {
                                goalDiscovered = true;
                                goalBoard = next.copy();
                                q2.offer(goalBoard);
                                d2.put(nextKey, new ArrayList<>());
                            }
                            // 如果双向相遇
                            if (d2.containsKey(nextKey)) {
                                return mergePaths(path1, d2.get(nextKey));
                            }
                        }
                    }
                }
            }

            // 扩展后向一层（反向移动）
            if (goalDiscovered && !q2.isEmpty()) {
                int size2 = q2.size();
                for (int i = 0; i < size2; i++) {
                    Board cur = q2.poll();
                    String curKey = getStateKey(cur);
                    List<Move> curPath = d2.get(curKey);

                    for (Block b : cur.getBlocks()) {
                        Point pos = b.getPosition();
                        for (Block.Direction dir : Block.Direction.values()) {
                            Board next = cur.copy();
                            Block onNext = next.getBlockAt(pos);
                            if (onNext == null) continue;
                            // 反向：尝试将方块往相反方向「退回」
                            Block.Direction rev = reverse(dir);
                            if (!next.moveBlock(onNext, rev)) continue;
                            String nextKey = getStateKey(next);
                            if (d2.containsKey(nextKey)) continue;

                            List<Move> path2 = new ArrayList<>(curPath);
                            // 记录回溯步伐：从 next 状态沿 dir 前进即可回到 cur
                            path2.add(new Move(onNext.getPosition(), dir));
                            d2.put(nextKey, path2);
                            q2.offer(next);

                            if (d1.containsKey(nextKey)) {
                                return mergePaths(d1.get(nextKey), path2);
                            }
                        }
                    }
                }
            }
        }
        return null;  // 无解
    }

    /** 将前向路径 + 后向路径（需反转顺序并取反方向）拼接成完整解 */
    private static List<Move> mergePaths(List<Move> p1, List<Move> p2) {
        List<Move> res = new ArrayList<>(p1);
        // p2 中每一步实际上是从 goal 往 init 方向的「回退」，所以要倒序，并将方向取反
        for (int i = p2.size() - 1; i >= 0; i--) {
            Move m = p2.get(i);
            res.add(new Move(m.from, reverse(m.dir)));
        }
        return res;
    }

    /** 取相反方向 */
    private static Block.Direction reverse(Block.Direction dir) {
        return switch (dir) {
            case UP    -> Block.Direction.DOWN;
            case DOWN  -> Block.Direction.UP;
            case LEFT  -> Block.Direction.RIGHT;
            case RIGHT -> Block.Direction.LEFT;
        };
    }

    /**
     * 将 Board 当前所有 Block 的类型/尺寸/位置编码成一个字符串，用于哈希判重
     */
    private static String getStateKey(Board b) {
        List<String> parts = new ArrayList<>();
        for (Block blk : b.getBlocks()) {
            Point p = blk.getPosition();
            Dimension d = blk.getSize();
            parts.add(String.format("%s@%d,%d#%dx%d",
                    blk.getType(), p.x, p.y, d.width, d.height));
        }
        Collections.sort(parts);
        return String.join("|", parts);
    }

    // List<AiSolver.Move> moves = AiSolver.solve(board);
    //if (moves != null) {
    //    moves.forEach(System.out::println);
    //} else {
    //    System.out.println("无解");
    //}
}
