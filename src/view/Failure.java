package view;

import model.GameMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * 失败页面，展示尝试结果并鼓励玩家重试。
 */
public class Failure extends JPanel {
    private final JLabel titleLabel;
    private final JLabel movesLabel;
    private final JLabel timeLabel;
    private final JLabel hintLabel;
    private final JButton retryBtn;
    private final JButton menuBtn;

    public Failure(Basic basic,
                   GameMap map,
                   int moves,
                   long elapsed) {
        // 纯白背景，无残影
        setOpaque(true);
        setBackground(Color.WHITE);

        // GridBagLayout 布局，分区显示信息和按钮
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 顶部标题
        titleLabel = new JLabel("挑战失败！", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
        titleLabel.setForeground(Color.RED);
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0.6;
        add(titleLabel, gbc);

        // 中部统计信息
        movesLabel = new JLabel("本次步数：" + moves, SwingConstants.CENTER);
        timeLabel  = new JLabel("用时：" + formatTime(elapsed), SwingConstants.CENTER);
        movesLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        hintLabel  = new JLabel("再接再厉，下次一定能过！", SwingConstants.CENTER);
        hintLabel.setFont(new Font("微软雅黑", Font.ITALIC, 18));
        hintLabel.setForeground(new Color(0x8B4513));

        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 20, 5, 20);
        add(movesLabel, gbc);
        gbc.gridy = 2; add(timeLabel, gbc);
        gbc.gridy = 3; add(hintLabel, gbc);

        // 按钮面板
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        btnPanel.setOpaque(false);
        retryBtn = new JButton("重试本地图");
        menuBtn  = new JButton("返回选单");
        styleButton(retryBtn);
        styleButton(menuBtn);
        btnPanel.add(retryBtn);
        btnPanel.add(menuBtn);
        gbc.gridy = 4;
        gbc.weighty = 0.8;
        gbc.fill = GridBagConstraints.NONE;
        add(btnPanel, gbc);

        // 事件绑定
        retryBtn.addActionListener(e -> {
            Game game = (Game) basic.getPanel("game");
            basic.showPanel("game");
            game.replay();
        });
        menuBtn.addActionListener(e -> {
            Select select = (Select) basic.getPanel("select");
            select.updateInfoPanel(map);
            select.updateStats(map);
            basic.showPanel("select");
        });

        // 动态缩放逻辑
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int h = getHeight();
                // 标题字体：高度的12%
                titleLabel.setFont(new Font("微软雅黑", Font.BOLD, Math.max(24, h * 12 / 100)));
                // 统计字体：高度的8%
                int infoFont = Math.max(16, h * 8 / 100);
                movesLabel.setFont(new Font("微软雅黑", Font.PLAIN, infoFont));
                timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, infoFont));
                hintLabel.setFont(new Font("微软雅黑", Font.ITALIC, Math.max(14, h * 6 / 100)));
                // 按钮字体：高度的40%
                int btnFont = Math.max(14, (int)(h * 0.15) * 40 / 100);
                retryBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, btnFont));
                menuBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, btnFont));
                // 按钮尺寸
                int w = getWidth();
                int btnW = w * 30 / 100;
                int btnH = h * 15 / 100;
                Dimension size = new Dimension(btnW, btnH);
                retryBtn.setPreferredSize(size);
                menuBtn.setPreferredSize(size);
                revalidate();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 先填充背景，避免残影
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /** 统一按钮风格 */
    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBackground(new Color(0x2E8B57));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    /** 毫秒转 mm:ss */
    private String formatTime(long ms) {
        int totalSec = (int) (ms / 1000);
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }
}
