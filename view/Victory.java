package view;

import javax.swing.*;
import java.awt.*;

/**
 * 胜利页面。
 */
public class Victory extends JPanel {

    /**
     * 构造方法：显示胜利信息并提供继续或退出选项。
     * @param basic 基础窗口引用，用于页面切换
     */
    public Victory(Basic basic) {
        setLayout(new BorderLayout());

        JLabel message = new JLabel("恭喜您，通关成功！", JLabel.CENTER);
        message.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        add(message, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton retryBtn = new JButton("重玩");
        JButton exitBtn = new JButton("退出");
        btnPanel.add(retryBtn);
        btnPanel.add(exitBtn);
        add(btnPanel, BorderLayout.SOUTH);

        retryBtn.addActionListener(e -> basic.showPanel("select"));
        exitBtn.addActionListener(e -> System.exit(0));
    }
}