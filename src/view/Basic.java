package view;

import util.Config;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础窗口类，承载所有页面，使用 CardLayout 管理不同面板。
 * 支持任意 JComponent 作为面板（如 JPanel、JLayeredPane 等）。
 */
public class Basic extends JFrame {
    /**
     * 用于卡片布局的 CardLayout 实例，切换不同页面。
     */
    private final CardLayout cardLayout;

    /**
     * 承载所有页面的容器面板，使用 CardLayout 管理。
     */
    private final JPanel cardPanel;

    /**
     * 存储所有已添加页面的映射：名称 -> 组件。
     */
    private final Map<String, JComponent> panels = new HashMap<>();

    /**
     * 构造基础窗口，初始化窗口属性及布局。
     * <ul>
     *     <li>从配置读取行列数并计算窗口大小。</li>
     *     <li>设置窗口标题、关闭行为和居中显示。</li>
     *     <li>初始化 CardLayout 并将其设置为内容面板。</li>
     * </ul>
     */
    public Basic() {
        // 获取全局配置实例
        Config config = Config.getInstance();

        // 设置窗口标题
        setTitle("华容道");

        // 点击关闭按钮时退出应用
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // 根据配置的行列数计算窗口宽高，额外预留顶部控件空间
        int width = config.getInt("cols") * 100;
        int height = config.getInt("rows") * 100 + 50;
        setSize(width, height);

        // 窗口初始时居中显示
        setLocationRelativeTo(null);

        // 初始化卡片布局管理器
        cardLayout = new CardLayout();

        // 创建使用卡片布局的面板，并设置为 JFrame 的内容面板
        cardPanel = new JPanel(cardLayout);
        setContentPane(cardPanel);
    }

    /**
     * 添加页面到基础窗口，并将其保存到内部映射中，便于后续检索和切换。
     *
     * @param name  卡片名称，用于后续调用 showPanel 或 getPanel
     * @param panel 页面组件，支持任何 JComponent（例如 JPanel、JLayeredPane）
     */
    public void addPanel(String name, JComponent panel) {
        // 将组件存入映射，便于根据名称检索
        panels.put(name, panel);

        // 将组件添加到卡片面板中，指定其名称作为卡片标识
        cardPanel.add(panel, name);
    }

    /**
     * 获取已注册的页面组件。
     *
     * @param name 卡片名称
     * @return 对应的 JComponent，如果不存在则返回 null
     */
    public JComponent getPanel(String name) {
        // 从映射中检索组件
        return panels.get(name);
    }

    /**
     * 显示指定名称的页面卡片。
     *
     * @param name 卡片名称
     */
    public void showPanel(String name) {
        // 调用 CardLayout.show 方法切换页面
        cardLayout.show(cardPanel, name);
    }
}
