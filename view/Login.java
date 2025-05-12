package view;

import javax.swing.*;
import java.awt.*;

/**
 * 登录页面。
 */
public class Login extends JPanel {
    private JTextField userField;
    private JPasswordField passField;

    /**
     * 构造方法：初始化登录界面控件。
     * @param basic 基础窗口引用，用于页面切换
     */
    public Login(Basic basic) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);

        JLabel userLabel = new JLabel("用户名：");
        c.gridx = 0; c.gridy = 0;
        add(userLabel, c);

        userField = new JTextField(15);
        c.gridx = 1;
        add(userField, c);

        JLabel passLabel = new JLabel("密码：");
        c.gridx = 0; c.gridy = 1;
        add(passLabel, c);

        passField = new JPasswordField(15);
        c.gridx = 1;
        add(passField, c);

        JButton loginBtn = new JButton("登录");
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        add(loginBtn, c);

        // 登录按钮事件：验证后进入选择页面
        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            // TODO: 根据需求验证用户名和密码
            basic.showPanel("select");
        });
    }
}