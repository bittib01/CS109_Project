package util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置读取器，配置格式：
 * <p>SettingA=true;</p>
 * <p>SettingB=OptionA;</p>
 */
public class Config {
    // 存储配置键值对的 Map
    private final Map<String, String> settings = new HashMap<>();

    /**
     * 私有构造方法（仅允许枚举单例调用），从指定文件加载配置
     * @param filePath 文件路径
     * @throws IOException 如果文件读取失败则抛出异常
     */
    private Config(String filePath) throws IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 去除首尾空白符
                line = line.trim();
                // 跳过注释行（以';'开头）或不包含 '=' 的行（包括空行）
                if (line.startsWith(";") || !line.contains("=")) {
                    continue;
                }
                // 如果以分号结尾，则去除末尾分号
                if (line.endsWith(";")) {
                    line = line.substring(0, line.length() - 1).trim();
                }
                // 按第一个 '=' 分割为键和值
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();    // 键
                    String value = parts[1].trim();  // 值
                    settings.put(key, value);
                }
            }
        }
    }

    /**
     * 枚举单例（全局唯一）
     */
    private enum Singleton {
        INSTANCE;

        private final Config configInstance;

        /**
         * 枚举构造方法（仅执行一次）
         */
        Singleton() {
            try {
                // 使用默认设置路径初始化
                configInstance = new Config("config.ini");
            } catch (IOException e) {
                throw new RuntimeException("设置实例初始化失败", e);
            }
        }

        private Config getInstance() {
            return configInstance;
        }
    }

    /**
     * 获取单例实例
     * @return 设置工具实例
     */
    public static Config getInstance() {
        return Config.Singleton.INSTANCE.getInstance();
    }

    /**
     * 获取字符串类型的配置值
     * @param key 配置项名称
     * @return 配置值字符串，如不存在则返回 null
     */
    public String getString(String key) {
        return settings.get(key);
    }

    /**
     * 获取布尔类型的配置值
     * @param key 配置项名称
     * @return 配置值布尔型，如不存在或解析失败则返回 false
     */
    public boolean getBoolean(String key) {
        String val = settings.get(key);
        return Boolean.parseBoolean(val);
    }

    /**
     * 获取整型类型的配置值
     * @param key 配置项名称
     * @return 配置值整型，如不存在或解析失败则返回 0
     */
    public int getInt(String key) {
        String val = settings.get(key);
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            // 如果解析失败，返回默认值 0
            return 0;
        }
    }
}
