package view;

import util.Config;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础窗口类，承载所有页面，使用 CardLayout 管理不同面板。
 * addPanel/getPanel 改为接收 JComponent，以支持不继承 JPanel 的面板（如 JLayeredPane）。
 */
public class Basic extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final Map<String, JComponent> panels = new HashMap<>();

    public Basic() {
        Config config = Config.getInstance();
        setTitle("华容道");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(
                config.getInt("cols") * 100,
                config.getInt("rows") * 100 + 50
        );
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        setContentPane(cardPanel);
    }

    /**
     * 添加页面到基础窗口，并保存到内部 Map 中。
     * @param name  卡片名称，用于切换和检索
     * @param panel 页面组件，支持任何 JComponent（如 JPanel、JLayeredPane 等）
     */
    public void addPanel(String name, JComponent panel) {
        panels.put(name, panel);
        cardPanel.add(panel, name);
    }

    /**
     * 获取已注册的页面组件。
     * @param name 卡片名称
     * @return 对应的 JComponent，如果不存在则返回 null
     */
    public JComponent getPanel(String name) {
        return panels.get(name);
    }

    /**
     * 显示指定页面。
     * @param name 卡片名称
     */
    public void showPanel(String name) {
        cardLayout.show(cardPanel, name);
    }
}