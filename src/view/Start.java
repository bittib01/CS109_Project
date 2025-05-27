package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * 开始页面（欢迎界面）
 */
public class Start extends JPanel {
    private final JLabel welcomeLabel;
    private final JButton loginBtn;
    private final JButton guestBtn;

    public Start(Basic basic) {
        // 不透明背景，统一白色
        setOpaque(true);
        setBackground(Color.WHITE);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.fill = GridBagConstraints.BOTH;

        // 欢迎标题
        welcomeLabel = new JLabel("欢迎来到华容道", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
        welcomeLabel.setForeground(new Color(0x2E8B57));
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0.7;
        add(welcomeLabel, gbc);

        // 按钮面板
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        btnPanel.setOpaque(false);
        loginBtn = new JButton("用户登录");
        guestBtn = new JButton("游客登录");
        styleButton(loginBtn);
        styleButton(guestBtn);
        btnPanel.add(loginBtn);
        btnPanel.add(guestBtn);
        gbc.gridy = 1;
        gbc.weighty = 0.3;
        gbc.fill = GridBagConstraints.NONE;
        add(btnPanel, gbc);

        // 事件绑定
        loginBtn.addActionListener(e -> basic.showPanel("login"));
        guestBtn.addActionListener(e -> {
            if (basic.getPanel("select") == null) {
                basic.addPanel("select", new Select(basic));
            }
            basic.showPanel("select");
        });

        // 动态调整字体与按钮大小
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                // 标题字体：高度的12%
                int titleFont = Math.max(24, h * 12 / 100);
                welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, titleFont));
                // 按钮尺寸：宽度的30%，高度的15%
                int btnW = w * 30 / 100;
                int btnH = h * 15 / 100;
                Dimension size = new Dimension(btnW, btnH);
                loginBtn.setPreferredSize(size);
                guestBtn.setPreferredSize(size);
                // 按钮字体：高度的40%
                int btnFont = Math.max(14, btnH * 40 / 100);
                Font bf = new Font(Font.SANS_SERIF, Font.BOLD, btnFont);
                loginBtn.setFont(bf);
                guestBtn.setFont(bf);
                revalidate();
            }
        });
    }

    /** 自定义绘制，先清除背景避免残影 */
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /** 按钮统一样式 */
    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBackground(new Color(0x2E8B57));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }
}
