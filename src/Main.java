import util.*;
import view.*;

import javax.swing.*;



/**
 * 程序入口：初始化 Basic 窗口并加载各页面。
 */
public class Main {
    public static void main(String[] args) {
        UserController userController = UserController.getInstance();
        Config config = Config.getInstance();
        Log log = Log.getInstance();
        log.setLevel(config.getString("level"));
        userController.login("Guest", "Guest");
        SwingUtilities.invokeLater(() -> {
            Basic basic = new Basic();
            // 添加各页面
            basic.addPanel("start", new Start(basic));
            basic.addPanel("login", new Login(basic));
            // 首先显示开始页面
            basic.showPanel("start");
            basic.setVisible(true);
        });
    }
}
