package view;

import util.UserController;

import javax.swing.*;
import java.awt.*;

/**
 * 开始页面（欢迎界面）。
 */
public class Start extends JPanel {

    /**
     * 构造方法：欢迎玩家并提供进入登录的入口。
     * @param basic 基础窗口引用，用于页面切换
     */
    public Start(Basic basic) {
        UserController userController = UserController.getInstance();
        setLayout(new BorderLayout());

        JLabel welcome = new JLabel("欢迎来到华容道游戏", JLabel.CENTER);
        welcome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        add(welcome, BorderLayout.CENTER);

        // 创建底部面板（使用FlowLayout）
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // 按钮居中排列

        // 添加按钮到底部面板
        JButton button1 = new JButton("用户账号");
        JButton button2 = new JButton("游客登录");

        bottomPanel.add(button1);
        bottomPanel.add(button2);

        add(bottomPanel, BorderLayout.SOUTH);

        button1.addActionListener(e -> basic.showPanel("login"));
        button2.addActionListener(e -> {
            basic.addPanel("select", new Select(basic));
            basic.showPanel("select");
        });
    }
}