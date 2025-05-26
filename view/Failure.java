package view;

import model.GameMap;
import util.UserController;

import javax.swing.*;
import java.awt.*;

/**
 * 失败页面，显示本次用时和步数，并鼓励玩家重试。
 */
public class Failure extends JPanel {

    /**
     * 构造方法
     * @param basic     基础窗口，用于页面切换
     * @param map       当前地图（用于重试操作）
     * @param mode      游戏模式
     * @param moves     本次尝试步数
     * @param elapsed   用时毫秒
     */
    public Failure(Basic basic,
                   GameMap map,
                   Game.Mode mode,
                   int moves,
                   long elapsed) {
        setLayout(new BorderLayout(10, 10));

        // 顶部提示
        JLabel title = new JLabel("挑战失败！", JLabel.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setForeground(Color.RED);
        add(title, BorderLayout.NORTH);

        // 中央统计信息
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 步数和时间
        JLabel movesLabel = new JLabel("本次步数：" + moves, JLabel.CENTER);
        JLabel timeLabel  = new JLabel("用时：" + formatTime(elapsed), JLabel.CENTER);
        movesLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        center.add(movesLabel);
        center.add(Box.createVerticalStrut(10));
        center.add(timeLabel);

        // 鼓励提示
        JLabel hintLabel = new JLabel("再接再厉，下次一定能过！", JLabel.CENTER);
        hintLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 16));
        hintLabel.setForeground(new Color(0x8B4513));
        center.add(Box.createVerticalStrut(15));
        center.add(hintLabel);

        add(center, BorderLayout.CENTER);

        // 按钮区：重试本地图 或 退出到选单
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton retryBtn = new JButton("重试本地图");
        JButton menuBtn  = new JButton("返回选单");
        btnPanel.add(retryBtn);
        btnPanel.add(menuBtn);
        add(btnPanel, BorderLayout.SOUTH);

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
    }

    /**
     * 毫秒转 mm:ss 格式
     */
    private String formatTime(long ms) {
        int totalSec = (int) (ms / 1000);
        int mm = totalSec / 60;
        int ss = totalSec % 60;
        return String.format("%02d:%02d", mm, ss);
    }
}
