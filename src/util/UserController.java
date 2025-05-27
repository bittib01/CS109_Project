package util;

import model.User;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


/**
 * 用户控制器（使用枚举实现单例）
 */
public class UserController {
    // 日志
    Log log = Log.getInstance();
    // 用户文件地址
    private static final String USER_FILE = "users.txt";
    // 用户表：用户名 -> User 对象
    private final Map<String, User> users = new HashMap<>();
    private User currentUser;

    /**
     * 私有构造方法（仅允许枚举单例调用），从指定文件加载用户配置
     */
    private UserController() {
        loadUsers();
    }

    /**
     * 加载用户文件
     */
    private void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], new User(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 保存所有用户到文件
     */
    private void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE))) {
            for (User u : users.values()) {
                writer.write(u.getUsername() + ":" + u.getPasswordHash());
                writer.newLine();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 枚举单例（全局唯一）
     */
    private enum Singleton {
        INSTANCE;

        private final UserController userControllerInstance;

        /**
         * 枚举构造方法（仅执行一次）
         */
        Singleton() {
            userControllerInstance = new UserController();
        }

        private UserController getInstance() {
            return userControllerInstance;
        }
    }

    /**
     * 获取单例实例
     * @return 设置工具实例
     */
    public static UserController getInstance() {
        return UserController.Singleton.INSTANCE.getInstance();
    }

    /**
     * 计算字符串的 MD5 值
     * @param input 输入字符串
     * @return 输入字符串的MD5值
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @return 注册成功返回<code>true</code>，否则返回<code>false</code>
     */
    public boolean register(String username, String password) {
        if (users.containsKey(username)) {
            log.warn(username + " 已存在，注册失败！");
            return false;
        }
        String hash = md5(password);
        users.put(username, new User(username, hash));
        saveUsers();
        log.info(username + " 注册成功！");
        return true;
    }

    /**
     * 用户登录
     */
    public boolean login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.checkPassword(md5(password))) {
            if (currentUser != null) {
                logout();
            }
            user.setLoggedIn(true);
            currentUser = user;
            log.info(username + ", 欢迎你！");
            return true;
        }
        log.warn(username + " 登录失败：用户名或密码输入错误");
        return false;
    }

    /**
     * 用户退出
     */
    public void logout() {
        if (currentUser != null) {
            log.info(currentUser.getUsername() + " 已退出");
            currentUser.setLoggedIn(false);
            currentUser = null;
        } else {
            log.warn("无法退出没有登录的用户");
        }
    }

    /**
     * 切换用户
     */
    public void switchUser(String username, String password) {
        log.info("正在切换用户到：" + username);
        logout();
        login(username, password);
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
