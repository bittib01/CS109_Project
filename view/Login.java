package view;

import util.UserController;
import javax.swing.*;
import java.awt.*;

/**
 * 登录页面。
 */
public class Login extends JPanel {
    private JTextField userField;
    private JPasswordField passField;
    private UserController userController = UserController.getInstance();

    /**
     * 构造方法：初始化登录界面控件。
     * @param basic 基础窗口引用，用于页面切换
     */
    public Login(Basic basic) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);  // 全局控件间距

        // 用户名标签
        JLabel userLabel = new JLabel("用户名：");
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;  // 标签右对齐
        add(userLabel, c);

        // 用户名输入框
        userField = new JTextField(15);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;  // 输入框左对齐
        add(userField, c);

        // 密码标签
        JLabel passLabel = new JLabel("密码：");
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        add(passLabel, c);

        // 密码输入框
        passField = new JPasswordField(15);
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        add(passField, c);

        // 创建按钮容器（使用FlowLayout自动居中）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));  // 按钮间距15px
        JButton loginBtn = new JButton("登录");
        JButton registerBtn = new JButton("注册");
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        // 将按钮容器添加到主布局（跨两列居中）
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;  // 跨用户名/密码两列
        c.anchor = GridBagConstraints.CENTER;  // 容器居中
        c.fill = GridBagConstraints.NONE;  // 不填充空间
        add(buttonPanel, c);

        // 登录按钮事件
        loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名或密码不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean success = userController.login(username, password);
            if (success) {
                // 登录成功，跳转到功能选择页面
                JOptionPane.showMessageDialog(this, "登录成功，欢迎 " + username + "！", "成功", JOptionPane.INFORMATION_MESSAGE);
                // 清空输入
                userField.setText("");
                passField.setText("");
                basic.addPanel("select", new Select(basic));
                basic.showPanel("select");
            } else {
                // 登录失败，提示错误
                JOptionPane.showMessageDialog(this, "登录失败：用户名或密码不正确。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 注册按钮事件
        registerBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名或密码不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean registered = userController.register(username, password);
            if (registered) {
                JOptionPane.showMessageDialog(this, "注册成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                userController.login(username, password);
                userField.setText("");
                passField.setText("");
                basic.addPanel("select", new Select(basic));
                basic.showPanel("select");
            } else {
                JOptionPane.showMessageDialog(this, "注册失败：用户名已存在。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
