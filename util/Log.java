package util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 支持级别控制和文件输出的轻量级日志工具类（枚举单例优化版）
 * <p>功能特性：</p>
 * <p>- 基于枚举实现线程安全的单例模式（JVM保证唯一性）</p>
 * <p>- 支持四种日志级别（ERROR/WARNING/INFO/DETAIL）</p>
 * <p>- 自动创建日志文件父目录（若不存在）</p>
 * <p>- 支持动态调整日志级别阈值</p>
 * <p>- 日志格式包含时间戳、级别和消息内容</p>
 */
public class Log {

    /**
     * 日志级别枚举类（优先级数值越低，优先级越高）
     */
    public enum Level {
        ERROR(1), WARNING(2), INFO(3), DETAIL(4);

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
     * 枚举单例持有者（JVM保证全局唯一）
     */
    private enum Singleton {
        INSTANCE;

        private final Log logInstance;

        // 枚举构造方法（JVM保证仅执行一次）
        Singleton() {
            try {
                // 使用默认日志路径和默认级别初始化
                logInstance = new Log("application.log");
                logInstance.setLevel(Level.INFO);
            } catch (IOException e) {
                throw new RuntimeException("日志实例初始化失败", e);
            }
        }

        private Log getInstance() {
            return logInstance;
        }
    }

    /**
     * 获取日志单例实例（线程安全）
     * @return 日志工具实例
     */
    public static Log getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    public void setLevel(Level level) {
        this.currentLevel = level;
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
    public void detail(String msg) {log(Level.DETAIL, msg);}

    /**
     * Log类使用示例
     */
    public static void main(String[] args) {
        Log log = Log.getInstance();
        log.setLevel(Level.INFO);
        log.detail("detail");
        log.warn("warn");
        log.info("info");
        log.error("error");
    }
}