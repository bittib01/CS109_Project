package util;

import model.Board;
import model.Board.MoveEntry;
import model.GameMap;
import model.Block;
import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * 存档工具类：
 * 提供用户存档的读写、校验、统计与自动修正功能。
 * 存档文件格式：
 * 第一行为最近游戏地图名称或 "null"；
 * 后续每行格式为 [mapName, md5]{body}，
 * body 含各字段及历史移动记录。
 */
public class Saver {
    private static final String SAVE_DIR = "saves";
    private static final String SAVE_EXT = ".sav";
    private static final Log log = Log.getInstance();

    /** 存档统计信息：完成次数、最佳用时与最佳步数 */
        public record Stats(int completedCount, long bestTime, int bestMoves) {}

    /**
     * 获取指定用户在指定地图上的存档统计信息
     * @param username 用户名
     * @param map      地图模型，用于匹配 mapName
     * @return 包含统计信息的 Optional，若无有效条目则为空
     */
    public static Optional<Stats> getStats(String username, GameMap map) {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        if (!Files.exists(file)) return Optional.empty();

        // 读取并验证文件格式
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (!checkFormat(lines) || lines.size() < 2) {
            return Optional.empty();
        }

        // 遍历每条记录，查找目标地图
        String header = lines.get(0);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            // 解析 [mapName,md5]{body}
            int b1 = line.indexOf('['), b2 = line.indexOf(']');
            String[] meta = line.substring(b1 + 1, b2).split(",", 2);
            if (!meta[0].equals(map.getName())) continue;

            String body = line.substring(line.indexOf('{') + 1, line.lastIndexOf('}'));
            Entry entry;
            try {
                entry = Entry.fromBody(meta[0], body);
            } catch (Exception ex) {
                showError("解析存档出错：" + ex.getMessage());
                return Optional.empty();
            }

            // 校验 MD5，如不匹配可自动修正或放弃
            String storedMd5 = meta[1];
            try {
                String actualMd5 = entry.calculateMd5();
                if (!storedMd5.equals(actualMd5)) {
                    if (!confirm("统计读取时 MD5 校验失败，是否继续？")) {
                        return Optional.empty();
                    }
                    // 自动修正并写回
                    Map<String, Entry> all = loadEntries(username);
                    rewriteEntries(username, header, all);
                }
            } catch (Exception ex) {
                showError("MD5 校验出错：" + ex.getMessage());
                return Optional.empty();
            }

            return Optional.of(
                    new Stats(entry.completedCount, entry.bestTime, entry.bestMoves)
            );
        }
        return Optional.empty();
    }

    /**
     * 读取并应用存档到 Board，返回已用时
     * @throws Exception 校验失败或 I/O 异常时抛出
     */
    public static long load(Board board, GameMap map, String username) throws Exception {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        if (!Files.exists(file)) {
            throw new IOException("存档文件不存在");
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (!checkFormat(lines) || lines.size() < 2) {
            throw new IOException("存档格式不正确");
        }

        Map<String, Entry> all = new LinkedHashMap<>();
        Entry target = null;
        String storedMd5 = null;

        // 解析所有条目
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            int b1 = line.indexOf('['), b2 = line.indexOf(']');
            String[] meta = line.substring(b1 + 1, b2).split(",", 2);
            String mapName = meta[0];
            String body = line.substring(line.indexOf('{') + 1, line.lastIndexOf('}'));
            Entry e = Entry.fromBody(mapName, body);
            all.put(mapName, e);
            if (mapName.equals(map.getName())) {
                target = e;
                storedMd5 = meta[1];
            }
        }
        if (target == null) {
            throw new IOException("找不到对应地图的存档");
        }

        // MD5 校验并可修正
        validateMd5(username, lines.get(0), all, target, storedMd5, map);

        // 验证并修正移动历史
        int validSteps = validateHistory(board, target.history);
        if (validSteps < target.history.size()) {
            if (!confirm("历史链校验失败，是否截断继续？")) {
                throw new SecurityException("历史链校验失败");
            }
            target.truncateHistory(validSteps);
            rewriteEntries(username, lines.get(0), all);
        }

        // 重放历史
        board.reset();
        for (MoveEntry me : target.history) {
            Block b = board.findBlockById(me.blockId());
            board.moveBlock(b, me.dir());
        }
        return target.elapsedSoFar;
    }

    /** 统一格式校验 */
    private static boolean checkFormat(List<String> lines) {
        if (lines.isEmpty()) {
            showError("存档为空或格式不正确");
            return false;
        }
        for (int i = 1; i < lines.size(); i++) {
            String ln = lines.get(i).trim();
            if (!ln.matches("^\\[[^,]+,[0-9A-Fa-f]{32}]\\{.+}$")) {
                showError("第" + (i+1) + "行格式不符");
                return false;
            }
        }
        return true;
    }

    /** 弹窗错误提示 */
    private static void showError(String msg) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg, "存档错误", JOptionPane.ERROR_MESSAGE));
    }

    /** 弹窗确认对话框 */
    private static boolean confirm(String msg) {
        int r = JOptionPane.showConfirmDialog(null, msg, "提示", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    /** 将 allEntries 写回磁盘 */
    private static void rewriteEntries(String username, String recentMap, Map<String, Entry> allEntries) {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        try {
            List<String> out = new ArrayList<>();
            out.add(recentMap);
            for (Entry e : allEntries.values()) {
                String body = e.toBody();
                String md5  = e.calculateMd5();
                out.add("[" + e.mapName + "," + md5 + "]{" + body + "}");
            }
            Files.write(file, out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) {
            log.error("修正存档失败：" + ex.getMessage());
            showError("修正存档失败：" + ex.getMessage());
        }
    }

    /** 加载所有条目到内存 */
    private static Map<String, Entry> loadEntries(String username) {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        if (!Files.exists(file)) return new LinkedHashMap<>();
        Map<String, Entry> all = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String ln = lines.get(i);
                int b1 = ln.indexOf('['), b2 = ln.indexOf(']');
                String[] meta = ln.substring(b1+1, b2).split(",",2);
                String body = ln.substring(ln.indexOf('{')+1, ln.lastIndexOf('}'));
                all.put(meta[0], Entry.fromBody(meta[0], body));
            }
        } catch (Exception ex) {
            log.error("加载存档条目失败：" + ex.getMessage());
        }
        return all;
    }

    /** 校验 MD5 并提示/修正 */
    private static void validateMd5(String username, String recentMap, Map<String, Entry> all,
                                    Entry target, String storedMd5, GameMap map) throws Exception {
        String currentMd5 = target.calculateMd5();
        if (!storedMd5.equals(currentMd5)) {
            if (!confirm("行 MD5 不匹配，是否继续？")) {
                throw new SecurityException("行 MD5 不匹配");
            }
            rewriteEntries(username, recentMap, all);
        }
        // 地图 MD5 校验
        if (!map.getMd5().equals(target.mapMd5)) {
            if (!confirm("地图 MD5 不匹配，是否继续？")) {
                throw new SecurityException("地图 MD5 不匹配");
            }
            target.mapMd5 = map.getMd5();
            rewriteEntries(username, recentMap, all);
        }
    }

    /** 验证移动历史有效步数 */
    private static int validateHistory(Board board, List<MoveEntry> history) {
        board.reset();
        Board copy = board.copy();
        int count = 0;
        for (MoveEntry me : history) {
            Block b = copy.getBlocks().stream()
                    .filter(x -> x.getId() == me.blockId())
                    .findFirst().orElse(null);
            if (b != null && copy.moveBlock(b, me.dir())) {
                count++;
            }
        }
        return count;
    }

    /** 保存游戏结果或进度
     * @param inGame true 表示手动存档，否则为关卡完成后保存
     */
    private static void saveInternal(GameMap map, String username,
                                     int completedCount, long bestTime, int bestMoves,
                                     boolean inGame, String mode,
                                     List<MoveEntry> history, long elapsed,
                                     String recentMapName) {
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
            Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
            Map<String, Entry> all = loadEntries(username);
            Entry e = all.getOrDefault(map.getName(), new Entry(map.getName(), map.getMd5()));
            e.update(completedCount, bestTime, bestMoves, inGame, mode, history, elapsed);
            all.put(map.getName(), e);

            // 写入
            List<String> out = new ArrayList<>();
            out.add(recentMapName == null ? "null" : recentMapName);
            for (Entry en : all.values()) {
                String body = en.toBody();
                String md5;
                try {
                    md5 = en.calculateMd5();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                out.add("[" + en.mapName + "," + md5 + "]{" + body + "}");
            }
            Files.write(file, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            log.error("保存存档失败：" + ex.getMessage());
        }
    }

    public static void saveResult(GameMap map, String username,
                                  int completedCount, long bestTime, int bestMoves,
                                  String mode, List<MoveEntry> history, long elapsed) {
        saveInternal(map, username, completedCount, bestTime, bestMoves,
                false, mode, history, elapsed, null);
    }

    public static void saveManual(GameMap map, String username,
                                  int completedCount, long bestTime, int bestMoves,
                                  String mode, List<MoveEntry> history, long elapsed) {
        saveInternal(map, username, completedCount, bestTime, bestMoves,
                true, mode, history, elapsed, map.getName());
    }

    /** 删除用户全部存档 */
    public static void resetAllSaves(String username) throws IOException {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        Files.deleteIfExists(file);
    }

    /** 获取最近打开的地图名称 */
    public static String getRecentMapName(String username) {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        if (!Files.exists(file)) return null;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return lines.isEmpty() ? null : lines.get(0);
        } catch (IOException e) {
            return null;
        }
    }

    /** 单条存档模型 */
    private static class Entry {
        String mapName;
        String mapMd5;
        int completedCount;
        long bestTime;
        int bestMoves;
        boolean inGame;
        String mode;
        int movesSoFar;
        long elapsedSoFar;
        List<MoveEntry> history = new ArrayList<>();

        Entry(String mapName, String mapMd5) {
            this.mapName = mapName;
            this.mapMd5  = mapMd5;
        }

        void update(int c, long t, int m, boolean inGame, String mode,
                    List<MoveEntry> history, long elapsed) {
            this.completedCount = c;
            this.bestTime       = t;
            this.bestMoves      = m;
            this.inGame         = inGame;
            this.mode           = mode;
            this.history        = new ArrayList<>(history);
            this.movesSoFar     = history.size();
            this.elapsedSoFar   = elapsed;
        }

        String toBody() {
            StringBuilder sb = new StringBuilder();
            sb.append(mapMd5).append(",")
                    .append(completedCount).append(",")
                    .append(bestTime).append(",")
                    .append(bestMoves).append(",")
                    .append(inGame).append(",")
                    .append(mode).append(",")
                    .append(movesSoFar).append(",")
                    .append(elapsedSoFar).append(",");
            for (MoveEntry me : history) {
                sb.append(me.blockId()).append(":")
                        .append(me.dir().name()).append(";");
            }
            return sb.toString();
        }

        String calculateMd5() throws Exception {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = toBody().getBytes(StandardCharsets.UTF_8);
            byte[] digest = md.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }

        static Entry fromBody(String mapName, String body) {
            String[] ps = body.split(",", 9);
            Entry e = new Entry(mapName, ps[0]);
            e.completedCount = Integer.parseInt(ps[1]);
            e.bestTime       = Long.parseLong(ps[2]);
            e.bestMoves      = Integer.parseInt(ps[3]);
            e.inGame         = Boolean.parseBoolean(ps[4]);
            e.mode           = ps[5];
            e.movesSoFar     = Integer.parseInt(ps[6]);
            e.elapsedSoFar   = Long.parseLong(ps[7]);
            String hist      = ps[8];
            if (!hist.isEmpty()) {
                for (String token : hist.split(";")) {
                    if (token.isEmpty()) continue;
                    String[] part = token.split(":");
                    int id = Integer.parseInt(part[0]);
                    Block.Direction dir = Block.Direction.valueOf(part[1]);
                    e.history.add(new MoveEntry(id, dir));
                }
            }
            return e;
        }

        void truncateHistory(int validCount) {
            this.history = this.history.subList(0, validCount);
            this.movesSoFar   = validCount;
            this.elapsedSoFar = validCount * 1000L;
        }
    }
}
