package util;

import model.User;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class UserController {
    // 日志
    Log log = Log.getInstance();
    // 用户文件地址
    private static final String USER_FILE = "users.txt";
    // 用户表：用户名 -> User 对象
    private Map<String, User> users = new HashMap<>();
    private User currentUser;

    public UserController() {
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
            System.out.println(username + " 已存在，注册失败！");
            return false;
        }
        String hash = md5(password);
        users.put(username, new User(username, hash));
        saveUsers();
        System.out.println(username + " 注册成功！");
        return true;
    }

    /**
     * 修改当前用户密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改成功返回<code>true</code>，否则返回<code>false</code>
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        if (currentUser == null) {
            System.out.println("修改密码前请先登录！");
            return false;
        }
        String username = currentUser.getUsername();
        if (!currentUser.checkPassword(md5(oldPassword))) {
            System.out.println(username + " 修改密码失败：旧密码输入不正确");
            return false;
        }
        currentUser.setPasswordHash(md5(newPassword));
        saveUsers();
        System.out.println(username + " 修改密码成功");
        return true;
    }

    /**
     * 修改当前登录用户的用户名
     * @param newUsername 新用户名
     * @return 修改成功返回<code>true</code>，否则返回<code>false</code>
     */
    public boolean changeUsername(String newUsername) {
        if (currentUser == null) {
            System.out.println("修改用户名前请先登录！");
            return false;
        }
        String oldUsername = currentUser.getUsername();
        if (users.containsKey(newUsername)) {
            System.out.println(newUsername + " 已存在，修改失败！");
            return false;
        }
        // 从 map 中移除旧键
        users.remove(oldUsername);
        // 更新 User 对象中的用户名
        currentUser.setUsername(newUsername);
        // 将用户对象以新用户名重新加入 map
        users.put(newUsername, currentUser);
        // 保存到文件
        saveUsers();
        System.out.println(oldUsername + " 已修改用户名为 " + newUsername);
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
            System.out.println(username + ", 欢迎你！");
            return true;
        }
        System.out.println(username + " 登录失败：用户名或密码输入错误");
        return false;
    }

    /**
     * 用户退出
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println(currentUser.getUsername() + " 已退出");
            currentUser.setLoggedIn(false);
            currentUser = null;
        } else {
            System.out.println("无法退出没有登录的用户");
        }
    }

    /**
     * 切换用户
     */
    public boolean switchUser(String username, String password) {
        System.out.println("正在切换用户到：" + username);
        logout();
        return login(username, password);
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
