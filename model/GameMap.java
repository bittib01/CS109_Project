package model;

import util.Log;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;

/**
 * 表示游戏地图对象，负责以下核心功能：
 * - 读取指定路径的地图文件
 * - 解析并验证地图布局合法性
 * - 生成游戏所需的方块实例集合
 * - 记录胜利区域坐标信息
 */
public class GameMap {
    /** 地图行数 */
    private int rows;
    /** 地图列数 */
    private int cols;
    /** 时间限制（秒） */
    private int timeLimit;
    /** 步数限制 */
    private int moveLimit;
    /** 布局矩阵（存储各位置的方块ID） */
    private int[][] layout;
    /** 胜利区域标记矩阵（true表示该位置为胜利区） */
    private boolean[][] victoryZone;
    /** 游戏方块实例集合（不可变列表） */
    private final List<Block> blocks = new ArrayList<>();
    /** 日志记录器实例 */
    private final Log log = Log.getInstance();
    /** 地图有效性标记（布局验证通过后为true） */
    private boolean isValid = true;
    /** 地图文件名（完整路径），用于标识地图名称 */
    private String mapName;

    /**
     * 通过指定地图文件路径初始化地图对象
     * @param filename 地图文件路径（包含文件名）
     */
    public GameMap(String filename) {
        this.mapName = filename;
        List<String[]> rawLines = new ArrayList<>();

        // 读取文件原始内容
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String firstLine = br.readLine();
            if (firstLine == null) {
                log.warn("地图文件 " + filename + " 不能为空");
                isValid = false;
                return;
            }
            String[] limits = firstLine.trim().split("\\s+");
            try {
                timeLimit = Integer.parseInt(limits[0]);
                moveLimit = Integer.parseInt(limits[1]);
            } catch (NumberFormatException e) {
                log.warn("解析 TimeLimit/MoveLimit 失败: " + firstLine);
                isValid = false;
            }
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                rawLines.add(parts);
            }
        } catch (IOException e) {
            log.warn("读取地图文件失败: " + e.getMessage());
            isValid = false;
        }

        // 动态确定行列数
        rows = rawLines.size();
        cols = rawLines.stream()
                .mapToInt(parts -> parts.length)
                .max().orElse(0);

        if (rows == 0 || cols == 0 ) {
            log.warn("地图文件 "+ filename + " 不能为空");
            isValid = false;
        }

        // 初始化矩阵
        layout = new int[rows][cols];
        victoryZone = new boolean[rows][cols];

        // 解析并填充布局数据
        for (int r = 0; r < rows; r++) {
            String[] parts = rawLines.get(r);
            for (int c = 0; c < parts.length; c++) {
                String cell = parts[c].trim();
                // 处理胜利区标记（以*结尾的单元格）
                if (cell.endsWith("*")) {
                    victoryZone[r][c] = true;
                    cell = cell.substring(0, cell.length() - 1).trim();
                }
                // 解析单元格数值（空内容视为0）
                if (cell.isEmpty()) {
                    layout[r][c] = 0;
                } else {
                    try {
                        layout[r][c] = Integer.parseInt(cell);
                    } catch (NumberFormatException nfe) {
                        log.warn("地图文件格式错误: 行" + (r+1) + " 列" + (c+1) + " 内容='" + cell + "'");
                        isValid = false;
                        layout[r][c] = 0;
                    }
                }
            }
        }

        validateLayout();  // 验证布局合法性
        initBlocks();      // 初始化方块实例

        // 胜利区数量校验（至少4个）
        if (getVictoryCells().size() < 4) {
            log.warn(filename + " 胜利区小于四个！");
            isValid = false;
        }
    }

    /**
     * 校验地图布局合法性（私有方法）
     * 验证规则：
     * - 每个非 0 方块 ID 对应的单元格数量必须为 1/2/4 个
     * - 方块形状必须为 1x1、1x2、2x1 或 2x2 的矩形
     */
    private void validateLayout() {
        Map<Integer, List<Point>> idCells = new HashMap<>();
        // 按ID收集所有单元格坐标
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int id = layout[r][c];
                idCells.computeIfAbsent(id, k -> new ArrayList<>()).add(new Point(r, c));
            }
        }
        // 遍历每个ID进行校验
        for (Map.Entry<Integer, List<Point>> e : idCells.entrySet()) {
            int id = e.getKey();
            if (id == 0) continue;  // 0表示空白区域，无需校验
            List<Point> cells = e.getValue();
            int count = cells.size();

            // 校验单元格数量
            if (count != 1 && count != 2 && count != 4) {
                log.error("ID=" + id + " 格子数非法：" + count);
                isValid = false;
            }

            // 校验形状（计算包围盒）
            int minR = cells.stream().mapToInt(p -> p.x).min().orElse(0);
            int maxR = cells.stream().mapToInt(p -> p.x).max().orElse(0);
            int minC = cells.stream().mapToInt(p -> p.y).min().orElse(0);
            int maxC = cells.stream().mapToInt(p -> p.y).max().orElse(0);
            int height = maxR - minR + 1;
            int width = maxC - minC + 1;

            // 形状必须满足：包围盒面积=单元格数量，且尺寸在1-2范围内
            if (height * width != count || height < 1 || height > 2 || width < 1 || width > 2) {
                log.error("ID=" + id + " 形状非法: " + height + "×" + width);
                isValid = false;
            }
        }
    }

    /**
     * 初始化游戏方块实例（私有方法）
     * 根据布局数据生成对应的Block对象，并校验是否存在关键方块（曹操）
     */
    private void initBlocks() {
        Map<Integer, List<Point>> idCells = new HashMap<>();
        // 收集坐标
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int id = layout[r][c];
                if (id == 0) continue;
                idCells.computeIfAbsent(id, k -> new ArrayList<>()).add(new Point(r, c));
            }
        }

        for (Map.Entry<Integer, List<Point>> entry : idCells.entrySet()) {
            int id = entry.getKey();
            List<Point> cells = entry.getValue();
            if (cells.isEmpty()) continue;

            // 计算起始点
            Point start = cells.stream()
                    .min(Comparator.<Point, Integer>comparing(p -> p.x)
                            .thenComparing(p -> p.y))
                    .orElse(cells.get(0));

            // 确定类型
            Block.Type type;
            int count = cells.size();
            if (count == 1) type = Block.Type.SMALL;
            else if (count == 2) {
                Point p0 = cells.get(0), p1 = cells.get(1);
                type = (p0.x == p1.x) ? Block.Type.HORIZONTAL : Block.Type.VERTICAL;
            } else type = Block.Type.LARGE;

            // 使用新的 Block 构造器
            blocks.add(new Block(id, type, start));
        }

        // 校验是否存在关键方块（曹操）
        boolean hasLargeBlock = false;
        for (Block block : blocks) {
            if (block.getType() == Block.Type.LARGE) {
                hasLargeBlock = true;
                break;
            }
        }
        if (!hasLargeBlock) {
            isValid = false;
            log.warn(mapName + "非法：无曹操（2x2大方块）");
        }
    }

    /**
     * 获取所有方块对象（不可修改的列表）
     * @return 方块实例的不可变列表
     */
    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    /**
     * 获取所有胜利区坐标集合
     * @return 胜利区坐标的List集合（Point对象包含行、列索引）
     */
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

    /**
     * 获取地图行数
     * @return 地图的行数
     */
    public int getRows() {
        return rows;
    }

    /**
     * 获取地图列数
     * @return 地图的列数
     */
    public int getCols() {
        return cols;
    }

    /**
     * 获取完整布局矩阵
     * @return 二维数组形式的布局数据（行优先）
     */
    public int[][] getLayout() {
        return layout;
    }

    /**
     * 获取地图有效性状态
     * @return true表示地图布局合法，false表示存在错误
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * 获取地图名称（即初始化时指定的文件名）
     * @return 地图文件名（包含路径）
     */
    public String getName() {
        return mapName;
    }

    /**
     * 计算并获取地图文件的MD5哈希值
     * @return 32位十六进制格式的MD5哈希字符串（计算失败时返回null）
     */
    public String getMd5() {
        try (FileInputStream fis = new FileInputStream(mapName)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();

            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException e) {
            log.error("读取地图文件失败，无法计算MD5: " + e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取时间限制（秒）
     * @return 时间限制，单位秒
     */
    public int getTimeLimit() {
        return timeLimit;
    }

    /**
     * 获取步数限制
     * @return 最大可移动步数
     */
    public int getMoveLimit() {
        return moveLimit;
    }
}