import util.*;
import view.*;

import javax.swing.*;



/**
 * 程序入口：初始化 Basic 窗口并加载各页面。
 */
public class Main {
    public static void main(String[] args) {
        Config config = Config.getInstance();
        Log log = Log.getInstance();
        log.setLevel(config.getString("level"));
        SwingUtilities.invokeLater(() -> {
            Basic basic = new Basic();
            // 添加各页面
            basic.addPanel("start", new Start(basic));
            basic.addPanel("login", new Login(basic));
            basic.addPanel("select", new Select(basic));
            basic.addPanel("victory", new Victory(basic));
            // 首先显示开始页面
            basic.showPanel("start");
            basic.setVisible(true);
        });
    }
}
