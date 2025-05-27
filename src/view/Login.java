package view;

import util.UserController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * 登录页面，统一视觉风格：纯白背景、深绿色按钮、无残影绘制、动态缩放。
 */
public class Login extends JPanel {
    private final JLabel titleLabel;
    private final JLabel userLabel;
    private final JTextField userField;
    private final JLabel passLabel;
    private final JPasswordField passField;
    private final JButton loginBtn;
    private final JButton registerBtn;
    private final UserController userController = UserController.getInstance();

    public Login(Basic basic) {
        // 纯白背景，清屏重绘
        setOpaque(true);
        setBackground(Color.WHITE);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 标题
        titleLabel = new JLabel("用户登录", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0x2E8B57));
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 0.2;
        add(titleLabel, gbc);

        // 用户名标签
        userLabel = new JLabel("用户名：");
        userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weighty = 0;
        add(userLabel, gbc);

        // 用户名输入框
        userField = new JTextField();
        userField.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        gbc.gridx = 1;
        add(userField, gbc);

        // 密码标签
        passLabel = new JLabel("密码：");
        passLabel.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(passLabel, gbc);

        // 密码输入框
        passField = new JPasswordField();
        passField.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        gbc.gridx = 1;
        add(passField, gbc);

        // 按钮区域
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        btnPanel.setOpaque(false);
        loginBtn = new JButton("登录");
        registerBtn = new JButton("注册");
        styleButton(loginBtn);
        styleButton(registerBtn);
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weighty = 0.3;
        gbc.fill = GridBagConstraints.NONE;
        add(btnPanel, gbc);

        // 事件绑定
        loginBtn.addActionListener(e -> onLogin(basic));
        registerBtn.addActionListener(e -> onRegister(basic));

        // 动态调整字体与控件尺寸
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                // 标题字体：高度的10%
                titleLabel.setFont(new Font("微软雅黑", Font.BOLD, Math.max(20, h / 10)));
                // 标签与输入框字体：高度的6%
                int labelFont = Math.max(14, h * 6 / 100);
                userLabel.setFont(new Font("微软雅黑", Font.PLAIN, labelFont));
                passLabel.setFont(new Font("微软雅黑", Font.PLAIN, labelFont));
                userField.setFont(new Font("微软雅黑", Font.PLAIN, labelFont));
                passField.setFont(new Font("微软雅黑", Font.PLAIN, labelFont));
                // 调整输入框高度
                int fieldH = h * 8 / 100;
                userField.setPreferredSize(new Dimension(w * 40 / 100, fieldH));
                passField.setPreferredSize(new Dimension(w * 40 / 100, fieldH));
                // 按钮字体 & 尺寸
                int btnFont = Math.max(14, h * 6 / 100);
                Font bf = new Font(Font.SANS_SERIF, Font.BOLD, btnFont);
                loginBtn.setFont(bf);
                registerBtn.setFont(bf);
                Dimension btnSize = new Dimension(w * 30 / 100, h * 12 / 100);
                loginBtn.setPreferredSize(btnSize);
                registerBtn.setPreferredSize(btnSize);
                revalidate();
            }
        });
    }

    /** 登录逻辑 */
    private void onLogin(Basic basic) {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            StyledDialog.showInfo(this, "用户名或密码不能为空！", "提示");
            return;
        }
        boolean success = userController.login(username, password);
        if (success) {
            StyledDialog.showInfo(this, "登录成功，欢迎 " + username + "！", "成功");
            userField.setText(""); passField.setText("");
            basic.addPanel("select", new Select(basic));
            basic.showPanel("select");
        } else {
            StyledDialog.showError(this, "登录失败：用户名或密码不正确。", "错误");
        }
    }

    /** 注册逻辑 */
    private void onRegister(Basic basic) {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            StyledDialog.showWarn(this, "用户名或密码不能为空！", "提示");
            return;
        }
        boolean registered = userController.register(username, password);
        if (registered) {
            StyledDialog.showInfo(this, "注册成功！", "成功");
            userController.login(username, password);
            userField.setText(""); passField.setText("");
            basic.addPanel("select", new Select(basic));
            basic.showPanel("select");
        } else {
            StyledDialog.showError(this, "注册失败：用户名已存在。", "错误");
        }
    }

    /** 清屏消除残影 */
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /** 统一按钮样式 */
    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBackground(new Color(0x2E8B57));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }
}
