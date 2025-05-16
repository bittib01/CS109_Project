package model;

import util.Log;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * 读取和验证地图布局，并生成方块实例，同时记录胜利区
 */
public class GameMap {
    private int rows;
    private int cols;
    private int[][] layout;
    private boolean[][] victoryZone;
    private final List<Block> blocks = new ArrayList<>();
    private final Log log = Log.getInstance();

    public GameMap(String filename) {
        List<String[]> rawLines = new ArrayList<>();
        // 先读取所有行，拆分为字符串数组
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                rawLines.add(parts);
            }
        } catch (IOException e) {
            log.error("读取地图文件失败: " + e.getMessage());
        }

        // 动态确定行列数
        rows = rawLines.size();
        cols = rawLines.stream()
                .mapToInt(parts -> parts.length)
                .max().orElse(0);

        // 初始化数组
        layout = new int[rows][cols];
        victoryZone = new boolean[rows][cols];

        // 解析内容并填充布局
        for (int r = 0; r < rows; r++) {
            String[] parts = rawLines.get(r);
            for (int c = 0; c < parts.length; c++) {
                String cell = parts[c].trim();
                if (cell.endsWith("*")) {
                    victoryZone[r][c] = true;
                    cell = cell.substring(0, cell.length() - 1).trim();
                }
                if (cell.isEmpty()) {
                    layout[r][c] = 0;
                } else {
                    try {
                        layout[r][c] = Integer.parseInt(cell);
                    } catch (NumberFormatException nfe) {
                        log.error("地图文件格式错误: 行" + (r+1) + " 列" + (c+1) + " 内容='" + cell + "'");
                        layout[r][c] = 0;
                    }
                }
            }
        }

        validateLayout();
        initBlocks();
    }

    // 校验合法性
    private void validateLayout() {
        java.util.Map<Integer, List<Point>> idCells = new HashMap<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int id = layout[r][c];
                idCells.computeIfAbsent(id, k -> new ArrayList<>()).add(new Point(r, c));
            }
        }
        for (java.util.Map.Entry<Integer, List<Point>> e : idCells.entrySet()) {
            int id = e.getKey();
            if (id == 0) continue;
            List<Point> cells = e.getValue();
            int count = cells.size();
            if (count != 1 && count != 2 && count != 4) {
                log.error("ID=" + id + " 格子数非法：" + count);
            }
            int minR = cells.stream().mapToInt(p -> p.x).min().orElse(0);
            int maxR = cells.stream().mapToInt(p -> p.x).max().orElse(0);
            int minC = cells.stream().mapToInt(p -> p.y).min().orElse(0);
            int maxC = cells.stream().mapToInt(p -> p.y).max().orElse(0);
            int h = maxR - minR + 1, w = maxC - minC + 1;
            if (h * w != count || h < 1 || h > 2 || w < 1 || w > 2) {
                log.error("ID=" + id + " 形状非法: " + h + "×" + w);
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
                idCells.computeIfAbsent(id, k -> new ArrayList<>()).add(new Point(r, c));
            }
        }
        for (List<Point> cells : idCells.values()) {
            if (cells.isEmpty()) continue;
            Point start = cells.stream()
                    .min(Comparator.<Point, Integer>comparing(p -> p.x)
                            .thenComparing(p -> p.y))
                    .orElse(cells.get(0));
            Block.Type type;
            int count = cells.size();
            if (count == 1) type = Block.Type.SMALL;
            else if (count == 2) {
                Point p0 = cells.get(0), p1 = cells.get(1);
                type = (p0.x == p1.x) ? Block.Type.VERTICAL : Block.Type.HORIZONTAL;
            } else type = Block.Type.LARGE;
            blocks.add(new Block(type, start));
        }
    }

    /** 获取所有方块对象 */
    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    /** 获取所有胜利区的坐标 */
    public List<Point> getVictoryCells() {
        List<Point> cells = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (victoryZone[r][c]) {
                    cells.add(new Point(r, c));
                }
            }
        }
        return cells;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    /** 获取完整布局数组 */
    public int[][] getLayout() {
        return layout;
    }
}
