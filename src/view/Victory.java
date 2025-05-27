package view;

import model.GameMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * 胜利页面，展示用时和步数，并在打破记录时给予特殊提示。
 */
public class Victory extends JPanel {
    private final JLabel titleLabel;
    private final JLabel movesLabel;
    private final JLabel timeLabel;
    private final JLabel recordLabel;
    private final JButton replayBtn;
    private final JButton menuBtn;

    public Victory(Basic basic,
                   GameMap map,
                   int moves,
                   long elapsed,
                   boolean isNewTimeRecord,
                   boolean isNewMovesRecord) {
        // 设置面板背景为纯色并在 paintComponent 中绘制
        setOpaque(true);
        setBackground(new Color(255, 255, 255));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 标题
        titleLabel = new JLabel("恭喜通关！", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
        titleLabel.setForeground(new Color(0x2E8B57));
        gbc.gridy = 0;
        gbc.weightx = 1;
        add(titleLabel, gbc);

        // 步数
        movesLabel = new JLabel("完成步数：" + moves, SwingConstants.CENTER);
        movesLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        gbc.gridy = 1;
        add(movesLabel, gbc);

        // 用时
        timeLabel = new JLabel("用时：" + formatTime(elapsed), SwingConstants.CENTER);
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        gbc.gridy = 2;
        add(timeLabel, gbc);

        // 纪录提示
        String recMsg = null;
        if (isNewTimeRecord || isNewMovesRecord) {
            StringBuilder sb = new StringBuilder("您创造了新的");
            if (isNewTimeRecord) sb.append("最快用时");
            if (isNewTimeRecord && isNewMovesRecord) sb.append("、");
            if (isNewMovesRecord) sb.append("最少步数");
            sb.append("记录！");
            recMsg = sb.toString();
        }
        recordLabel = new JLabel(recMsg != null ? recMsg : "", SwingConstants.CENTER);
        recordLabel.setFont(new Font("微软雅黑", Font.ITALIC, 18));
        recordLabel.setForeground(new Color(0xDAA520));
        gbc.gridy = 3;
        if (recMsg != null) add(recordLabel, gbc);

        // 按钮面板
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        btnPanel.setOpaque(false);
        replayBtn = new JButton("重玩本地图");
        menuBtn = new JButton("返回选单");
        styleButton(replayBtn);
        styleButton(menuBtn);
        btnPanel.add(replayBtn);
        btnPanel.add(menuBtn);
        gbc.gridy = 4;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        add(btnPanel, gbc);

        // 事件绑定
        replayBtn.addActionListener(e -> {
            basic.showPanel("game");
            Game game = (Game) basic.getPanel("game");
            game.replay();
        });
        menuBtn.addActionListener(e -> {
            Select select = (Select) basic.getPanel("select");
            select.updateInfoPanel(map);
            select.updateStats(map);
            basic.showPanel("select");
        });

        // 动态调整字体大小，确保无残影
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int h = getHeight();
                titleLabel.setFont(new Font("微软雅黑", Font.BOLD, Math.max(24, h / 15)));
                Font font = new Font("微软雅黑", Font.PLAIN, Math.max(16, h / 30));
                movesLabel.setFont(font);
                timeLabel.setFont(font);
             recordLabel.setFont(new Font("微软雅黑", Font.ITALIC, Math.max(14, h / 35)));
                int btnFont = Math.max(14, h / 35);
             replayBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, btnFont));
             menuBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, btnFont));
            }
        });
    }

    /** 在自定义绘制前清除背景*/
    @Override
    protected void paintComponent(Graphics g) {
        // 先填充背景
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /** 统一按钮样式 */
    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBackground(new Color(0x2E8B57));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    /** 毫秒转 mm:ss */
    private String formatTime(long ms) {
        int totalSec = (int)(ms / 1000);
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }
}