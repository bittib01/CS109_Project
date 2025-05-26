package util;

import model.Board;
import model.GameMap;
import model.Block;
import model.Board.MoveEntry;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * 存档工具类：提供存档保存、读取、校验和自动修正功能。
 */
public class Saver {
    private static final String SAVE_DIR = "saves";
    private static final String SAVE_EXT = ".sav";
    private static final Log log = Log.getInstance();

    /**
     * 存档条目统计信息：完成次数、最佳时间、最佳步数
     */
    public static class Stats {
        private final int completedCount;
        private final long bestTime;
        private final int bestMoves;
        public Stats(int c, long t, int m) {
            this.completedCount = c;
            this.bestTime = t;
            this.bestMoves = m;
        }
        public int getCompletedCount() { return completedCount; }
        public long getBestTime() { return bestTime; }
        public int getBestMoves() { return bestMoves; }
    }

    /**
     * 基于已读入的行集合校验存档格式是否合法；
     * 不会做文件 I/O，仅针对内存列表进行校验。
     * @param lines 文件全内容行列表
     * @return true 格式合法，否则弹窗提示并返回 false
     */
    private static boolean checkFormatLines(List<String> lines) {
        if (lines.isEmpty()) {
            showError("存档文件为空或格式不正确");
            return false;
        }
        // 跳过第一行（recentMap 名称或 null），从第 2 行开始校验
        for (int i = 1; i < lines.size(); i++) {
            String ln = lines.get(i).trim();
            // 要求格式：[mapName,MD5]{body}
            if (!ln.matches("^\\[[^,]+,[0-9a-fA-F]{32}\\]\\{.+}$")) {
                showError("存档第 " + (i + 1) + " 行结构不符合要求");
                return false;
            }
            int c1 = ln.indexOf('{'), c2 = ln.lastIndexOf('}');
            String[] parts = ln.substring(c1 + 1, c2).split(",", 9);
            if (parts.length != 9) {
                showError("存档第 " + (i + 1) + " 行字段数量不正确");
                return false;
            }
            // 检查数值字段非负：parts[1]=completedCount, [2]=bestTime, [3]=bestMoves, [6]=movesSoFar, [7]=elapsedSoFar
            try {
                if (Integer.parseInt(parts[1]) < 0
                        || Long.parseLong(parts[2]) < 0
                        || Integer.parseInt(parts[3]) < 0
                        || Integer.parseInt(parts[6]) < 0
                        || Long.parseLong(parts[7]) < 0L) {
                    showError("存档第 " + (i + 1) + " 行数值字段非法(必须非负)");
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("存档第 " + (i + 1) + " 行数值字段格式错误");
                return false;
            }
        }
        return true;
    }

    /**
     * 弹窗显示错误信息
     */
    private static void showError(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, msg, "存档格式错误", JOptionPane.ERROR_MESSAGE)
        );
    }

    /**
     * 弹窗询问，是否强制继续（用于 MD5 校验等警告）
     */
    private static boolean confirmForce(String msg) {
        int choice = JOptionPane.showConfirmDialog(
                null, msg, "MD5校验警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * 获取指定用户、指定地图的存档统计信息；
     * 全程只读取一次文件，后续基于内存列表处理。
     */
    public static Optional<Stats> getStats(String username, GameMap map) {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        if (!Files.exists(file)) return Optional.empty();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (!checkFormatLines(lines) || lines.size() < 2) return Optional.empty();

        // 遍历行，查找目标地图条目
        for (int i = 1; i < lines.size(); i++) {
            String ln = lines.get(i);
            int b1 = ln.indexOf('['), b2 = ln.indexOf(']');
            String[] head = ln.substring(b1 + 1, b2).split(",", 2);
            if (!head[0].equals(map.getName())) continue;
            String body = ln.substring(ln.indexOf('{') + 1, ln.lastIndexOf('}'));
            Entry e;
            try {
                e = Entry.fromBody(head[0], body);
            } catch (Exception ex) {
                showError("解析存档时出错：" + ex.getMessage());
                return Optional.empty();
            }
            String storedMd5 = head[1], calcMd5;
            try {
                calcMd5 = e.lineMd5();
            } catch (Exception ex) {
                showError("计算行 MD5 时出错：" + ex.getMessage());
                return Optional.empty();
            }

            // ←—— 这是新增的部分 ——→
            if (!storedMd5.equals(calcMd5)) {
                if (!confirmForce("统计读取时，MD5 校验失败，是否继续？")) {
                    return Optional.empty();
                }
                // 用户选择忽略：自动把新 MD5 写回存档，避免下次再报
                Map<String, Entry> all = loadAllEntries(username);
                // 重写所有条目，里面会自动用 Entry.lineMd5()（即 calcMd5）更新这一行
                rewriteAllEntries(username, lines.get(0), all);
            }
            // ←—— 分支结束 ——→

            return Optional.of(new Stats(e.completedCount, e.bestTime, e.bestMoves));
        }
        return Optional.empty();
    }


    /**
     * 读取并恢复存档；遇到任何校验失败都会弹窗询问。
     * 全流程只对磁盘执行一次读取。
     * @return 已用时长（毫秒）
     */
    public static long load(Board board, GameMap map, String username) throws Exception {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        if (!Files.exists(file)) {
            throw new IOException("存档文件不存在");
        }
        // 一次性读取文件所有行
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (!checkFormatLines(lines) || lines.size() < 2) {
            throw new IOException("存档结构或内容不正确");
        }

        String recentMapLine = lines.get(0);
        Map<String, Entry> all = new LinkedHashMap<>();
        Entry target = null;
        String storedMd5 = null;
        // 解析所有 Entry 到内存 Map，并标记目标 Entry
        for (int i = 1; i < lines.size(); i++) {
            String ln = lines.get(i);
            int b1 = ln.indexOf('['), b2 = ln.indexOf(']');
            String[] head = ln.substring(b1 + 1, b2).split(",", 2);
            String name = head[0], lineMd5 = head[1];
            String body = ln.substring(ln.indexOf('{') + 1, ln.lastIndexOf('}'));
            Entry e = Entry.fromBody(name, body);
            all.put(name, e);
            if (name.equals(map.getName())) {
                target = e;
                storedMd5 = lineMd5;
            }
        }
        if (target == null) {
            throw new IOException("存档中无当前地图记录");
        }

        // 校验行 MD5
        String calcLineMd5 = target.lineMd5();
        if (!storedMd5.equals(calcLineMd5)) {
            if (!confirmForce("行 MD5 不匹配，是否继续？")) {
                throw new SecurityException("读取取消：行 MD5 不匹配");
            }
            // 用户选择强制继续，更新内存里这条记录的 MD5 并写回
            // 注意 all 已经包含了 mapName -> target
            // rewriteAllEntries 会根据 Entry.lineMd5() 重新生成最新的 MD5
            rewriteAllEntries(username, /* recentMapLine */ lines.get(0), all);
        }

        // 校验地图文件 MD5
        if (!map.getMd5().equals(target.mapMd5)) {
            if (!confirmForce("地图 MD5 不匹配，是否继续？")) {
                throw new SecurityException("读取取消：地图 MD5 不匹配");
            }
            // 用户强制继续，更新 Entry.mapMd5，再写回
            target.mapMd5 = map.getMd5();
            rewriteAllEntries(username, lines.get(0), all);
        }

        // 校验移动链合理性
        int valid = validateHistory(board, target.history);
        if (valid < target.history.size()) {
            if (!confirmForce("历史链校验失败，是否截断继续？")) {
                throw new SecurityException("读取取消：历史链校验失败");
            }
            target.history = target.history.subList(0, valid);
            target.movesSoFar = valid;
            target.elapsedSoFar = valid * 1000L;
            // 修正存档到文件
            rewriteAllEntries(username, recentMapLine, all);
        }

        // 重放移动记录到 board
        board.reset();
        for (MoveEntry me : target.history) {
            Block b = board.getBlocks().stream()
                    .filter(x -> x.getId() == me.getBlockId())
                    .findFirst().get();
            board.moveBlock(b, me.getDir());
        }
        return target.elapsedSoFar;
    }

    /**
     * 用内存中的 allEntries 重写存档文件，避免重复 I/O。
     */
    private static void rewriteAllEntries(String username,
                                          String recentMapLine,
                                          Map<String, Entry> allEntries) {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        try {
            List<String> out = new ArrayList<>();
            out.add(recentMapLine);
            for (Entry e : allEntries.values()) {
                String body = e.toBody();
                String md5  = e.lineMd5();
                out.add("[" + e.mapName + "," + md5 + "]{" + body + "}");
            }
            Files.write(file, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) {
            log.error("自动修正存档 MD5 失败: " + ex.getMessage());
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                            "修正存档 MD5 失败：" + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE)
            );
        }
    }

    /**
     * 开始新游戏或胜利失败时的自动存档（inGame=false）
     */
    public static void saveResult(Board board,
                                  GameMap map,
                                  String username,
                                  int completedCount,
                                  long bestTime,
                                  int bestMoves,
                                  String mode,
                                  List<MoveEntry> history,
                                  long elapsed) {
        saveInternal(board, map, username,
                completedCount, bestTime, bestMoves,
                false, mode, history, elapsed, null);
    }

    /**
     * 手动存档（inGame=true）
     */
    public static void saveManual(Board board,
                                  GameMap map,
                                  String username,
                                  int completedCount,
                                  long bestTime,
                                  int bestMoves,
                                  String mode,
                                  List<MoveEntry> history,
                                  long elapsed) {
        saveInternal(board, map, username,
                completedCount, bestTime, bestMoves,
                true, mode, history, elapsed, map.getName());
    }

    /**
     * 内部存档实现：统一写入文件
     */
    private static void saveInternal(Board board,
                                     GameMap map,
                                     String username,
                                     int completedCount,
                                     long bestTime,
                                     int bestMoves,
                                     boolean inGame,
                                     String mode,
                                     List<MoveEntry> history,
                                     long elapsed,
                                     String recentMapName) {
        try {
            Path dir = Paths.get(SAVE_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path file = dir.resolve(username + SAVE_EXT);
            List<String> out = new ArrayList<>();
            // 第一行：recentMapName 或 "null"
            out.add(recentMapName == null ? "null" : recentMapName);

            // 读取已有所有条目
            Map<String, Entry> all = loadAllEntries(username);
            Entry e = all.getOrDefault(map.getName(), new Entry(map.getName(), map.getMd5()));
            e.mapMd5 = map.getMd5();
            e.completedCount = completedCount;
            e.bestTime = bestTime;
            e.bestMoves = bestMoves;
            e.inGame = inGame;
            e.mode = mode;
            e.history = history;
            e.movesSoFar = history.size();
            e.elapsedSoFar = elapsed;
            all.put(map.getName(), e);

            // 写回所有条目
            for (Entry en : all.values()) {
                String body = en.toBody();
                String md5  = en.lineMd5();
                out.add("[" + en.mapName + "," + md5 + "]{" + body + "}");
            }
            Files.write(file, out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) {
            log.error("保存存档失败: " + ex.getMessage());
        }
    }

    /**
     * 读取所有条目；内部读取一次磁盘用于 saveInternal
     */
    private static Map<String, Entry> loadAllEntries(String username) {
        Path file = Paths.get(SAVE_DIR, username + SAVE_EXT);
        if (!Files.exists(file)) return new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Map<String, Entry> all = new LinkedHashMap<>();
            for (int i = 1; i < lines.size(); i++) {
                String ln = lines.get(i);
                int b1 = ln.indexOf('['), b2 = ln.indexOf(']');
                String head = ln.substring(b1 + 1, b2);
                String[] parts = head.split(",", 2);
                String name = parts[0];
                String body = ln.substring(ln.indexOf('{') + 1, ln.lastIndexOf('}'));
                Entry e = Entry.fromBody(name, body);
                all.put(name, e);
            }
            return all;
        } catch (IOException | RuntimeException ex) {
            log.error("加载所有存档条目失败: " + ex.getMessage());
            return new LinkedHashMap<>();
        } catch (Exception ex) {
            log.error("解析存档条目失败: " + ex.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * 内存中的单地图存档条目模型
     */
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
            this.mapMd5 = mapMd5;
        }

        /**
         * 序列化为存档行的 body 部分
         */
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
                sb.append(me.getBlockId())
                        .append(":")
                        .append(me.getDir().name())
                        .append(";");
            }
            return sb.toString();
        }

        /**
         * 计算 body 部分的 MD5
         */
        String lineMd5() throws Exception {
            byte[] data = toBody().getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        }

        /**
         * 从 body 字符串解析出 Entry 对象
         */
        static Entry fromBody(String mapName, String body) throws Exception {
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
    }

    /**
     * 校验移动链能否依次应用到 board；
     * 返回可应用的最大步数。
     */
    private static int validateHistory(Board board, List<MoveEntry> history) {
        board.reset();
        Board copy = board.copy();
        copy.reset();
        int valid = 0;
        for (MoveEntry me : history) {
            Block b = copy.getBlocks().stream()
                    .filter(x -> x.getId() == me.getBlockId())
                    .findFirst().orElse(null);
            boolean ok = copy.moveBlock(b, me.getDir());
            if (!ok) {
                continue;
            }
            valid++;
        }
        return valid;
    }

    /**
     * 删除指定用户的所有存档文件（彻底清空 SAVE_DIR 下的 *.sav）
     */
    public static void resetAllSaves(String username) throws IOException {
        Path dir = Paths.get(SAVE_DIR);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;
        // 只删当前用户的存档
        Path file = dir.resolve(username + SAVE_EXT);
        Files.deleteIfExists(file);
    }

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

}
