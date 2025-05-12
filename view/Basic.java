package view;

import util.Config;

import javax.swing.*;
import java.awt.*;

/**
 * 基础窗口类，承载所有页面，使用 CardLayout 管理不同面板。
 */
public class Basic extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardPanel;

    /**
     * 构造方法：初始化基础窗口和卡片布局。
     */
    public Basic() {
        Config config = Config.getInstance();
        setTitle("华容道");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize( Config.getInstance().getInt("cols") * 100, Config.getInstance().getInt("rows") * 100 + 50 );
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        setContentPane(cardPanel);
    }

    /**
     * 添加页面到基础窗口。
     * @param name 卡片名称，用于切换
     * @param panel 页面面板
     */
    public void addPanel(String name, JPanel panel) {
        cardPanel.add(panel, name);
    }

    /**
     * 显示指定页面。
     * @param name 卡片名称
     */
    public void showPanel(String name) {
        cardLayout.show(cardPanel, name);
    }
}