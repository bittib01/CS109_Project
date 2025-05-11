package model;

import util.Log;
import static util.Config.rows;
import static util.Config.cols;

import java.awt.Point;
import java.io.*;
import java.util.*;

/**
 * 读取和验证地图布局，并生成方块实例
 */
public class MapModel {
    private final int[][] layout;
    private final List<Block> blocks = new ArrayList<>();
    private final Log log = Log.getInstance();

    public MapModel(String filename) {
        layout = new int[rows][cols];
        loadLayout(filename);
        validateLayout();
        initBlocks();
    }

    // 从文件读取布局
    private void loadLayout(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line; int r = 0;
            while ((line = br.readLine()) != null && r < rows) {
                String[] parts = line.trim().split(",");
                for (int c = 0; c < Math.min(parts.length, cols); c++) {
                    layout[r][c] = Integer.parseInt(parts[c]);
                }
                r++;
            }
            if (r < rows) {
                log.error("地图行数不足：期望 " + rows + " 行，读取到 " + r + " 行。");
            }
        } catch (IOException e) {
            log.error("读取地图文件失败: " + e.getMessage());
        }
    }

    // 校验合法性
    private void validateLayout() {
        Map<Integer, List<Point>> idCells = new HashMap<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int id = layout[r][c];
                idCells.computeIfAbsent(id, k -> new ArrayList<>()).add(new Point(r, c));
            }
        }
        for (Map.Entry<Integer, List<Point>> e : idCells.entrySet()) {
            int id = e.getKey();
            if (id == 0) continue;
            List<Point> cells = e.getValue();
            int count = cells.size();
            if (count != 1 && count != 2 && count != 4) {
                throw new IllegalArgumentException("ID=" + id + " 格子数非法: " + count);
            }
            // 包围盒检查
            int minR = cells.stream().mapToInt(p->p.x).min().orElse(0);
            int maxR = cells.stream().mapToInt(p->p.x).max().orElse(0);
            int minC = cells.stream().mapToInt(p->p.y).min().orElse(0);
            int maxC = cells.stream().mapToInt(p->p.y).max().orElse(0);
            int h = maxR - minR + 1, w = maxC - minC + 1;
            if (h * w != count || h<1 || h>2 || w<1 || w>2) {
                throw new IllegalArgumentException("ID=" + id + " 形状非法: " + h + "×"+w);
            }
        }
    }

    // 生成方块列表
    private void initBlocks() {
        Map<Integer, List<Point>> idCells = new HashMap<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int id = layout[r][c];
                if (id == 0) continue;
                idCells.computeIfAbsent(id, k->new ArrayList<>()).add(new Point(r, c));
            }
        }
        for (List<Point> cells : idCells.values()) {
            if (cells.isEmpty()) continue;
            Point start = cells.stream().min(Comparator.<Point, Integer>comparing(p->p.x)
                    .thenComparing(p->p.y)).orElse(cells.get(0));
            Block.Type type;
            int count = cells.size();
            if (count == 1) type = Block.Type.SMALL;
            else if (count == 2) {
                Point p0 = cells.get(0), p1 = cells.get(1);
                type = (p0.x==p1.x) ? Block.Type.HORIZONTAL : Block.Type.VERTICAL;
            } else type = Block.Type.LARGE;
            blocks.add(new Block(type, start));
        }
    }

    /**
     * 获取所有方块对象
     */
    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    /**
     * 获取原始布局，用于调试
     */
    public int[][] getLayout() { return layout; }
}