package view;

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
        setLayout(new BorderLayout());

        JLabel welcome = new JLabel("欢迎来到华容道游戏", JLabel.CENTER);
        welcome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        add(welcome, BorderLayout.CENTER);

        JButton enterBtn = new JButton("进入登录");
        add(enterBtn, BorderLayout.SOUTH);

        enterBtn.addActionListener(e -> basic.showPanel("login"));
    }
}