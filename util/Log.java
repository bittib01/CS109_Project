package util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 支持级别控制和文件输出的轻量级日志工具类（使用枚举类实现单例）
 */
public class Log {

    /**
     * 日志级别枚举类（优先级数值越低，优先级越高）
     */
    public enum Level {
        ERROR(1), WARNING(2), INFO(3), DEBUG(4);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private Level currentLevel = Level.INFO;
    private final BufferedWriter writer;

    /**
     * 私有构造方法（仅允许枚举单例调用）
     * @param filename 日志文件路径（支持相对/绝对路径）
     * @throws IOException 文件操作异常
     */
    private Log(String filename) throws IOException {
        File file = new File(filename);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean flag = parent.mkdirs();
            if (!flag) {
                throw new IOException("无法创建目录: " + parent);
            }
        }
        writer = new BufferedWriter(new FileWriter(file, true));
    }

    /**
     * 枚举单例（全局唯一）
     */
    private enum Singleton {
        INSTANCE;

        private final Log logInstance;

        /**
         * 枚举构造方法（仅执行一次）
         */
        Singleton() {
            try {
                // 使用默认日志路径和默认级别初始化
                logInstance = new Log("HuaRongRoad.log");
                logInstance.setLevel("info");
            } catch (IOException e) {
                throw new RuntimeException("日志实例初始化失败", e);
            }
        }

        private Log getInstance() {
            return logInstance;
        }
    }

    /**
     * 获取单例实例
     * @return 日志工具实例
     */
    public static Log getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    public void setLevel(String s) {

        this.currentLevel = switch (s) {
            case "info", "INFO" -> Level.INFO;
            case "warning", "WARNING" -> Level.WARNING;
            case "error", "ERROR" -> Level.ERROR;
            case "debug", "DEBUG" -> Level.DEBUG;
            default -> throw new IllegalStateException("未知等级: " + s);
        };
    }

    public synchronized void log(Level level, String message) {
        if (level.getPriority() <= currentLevel.getPriority()) {
            String timestamp = dateFormat.format(new Date());
            String line = String.format("%s [%s] %s", timestamp, level.name(), message);

            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("日志写入失败: " + e.getMessage());
            }
        }
    }

    public void error(String msg) {log(Level.ERROR, msg);}
    public void warn(String msg) {log(Level.WARNING, msg);}
    public void info(String msg) {log(Level.INFO, msg);}
    public void detail(String msg) {log(Level.DEBUG, msg);}

}