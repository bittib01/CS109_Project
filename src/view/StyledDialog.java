package view;

import util.UserController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 统一风格的提示及身份对话框工具类，替代 JOptionPane。
 * 支持信息、警告、错误、确认、输入、登录和注册对话框。
 */
public class StyledDialog {
    private static final Font MESSAGE_FONT = new Font("微软雅黑", Font.PLAIN, 16);
    private static final Font TITLE_FONT = new Font("微软雅黑", Font.BOLD, 18);
    private static final Color BUTTON_BG = new Color(0x2E8B57);
    private static final Color BUTTON_FG = Color.WHITE;
    private static final Color BG_COLOR = Color.WHITE;
    private static final UserController userController = UserController.getInstance();

    /**
     * 弹出自定义消息对话框
     */
    private static JDialog createDialog(Component parent, String title, Icon icon, String message) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(MESSAGE_FONT);
        msgLabel.setIcon(icon);
        msgLabel.setIconTextGap(10);
        panel.add(msgLabel, BorderLayout.CENTER);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.addWindowListener(new WindowAdapter() {@
                Override
        public void windowClosing(WindowEvent e) {
            dialog.dispose();
        }
        });
        return dialog;
    }

    /**
     * 弹出登录对话框
     */
    public static void showLoginDialog(Component parent, Runnable onSuccess) {
        JTextField userField = new JTextField(15);
        JPasswordField pwdField = new JPasswordField(15);
        JPanel p = new JPanel(new GridLayout(2, 2, 5, 5));
        p.add(new JLabel("用户名:"));
        p.add(userField);
        p.add(new JLabel("密码:"));
        p.add(pwdField);

        int result = JOptionPane.showConfirmDialog(parent, p, "用户登录", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String u = userField.getText().trim();
        String pw = new String(pwdField.getPassword());
        if (userController.login(u, pw)) {
            JDialog dlg = createDialog(parent, "成功", UIManager.getIcon("OptionPane.informationIcon"), "登录成功，欢迎 " + u + "！");
            JButton ok = new JButton("确定");
            ok.setFont(MESSAGE_FONT);
            ok.setBackground(BUTTON_BG);
            ok.setForeground(BUTTON_FG);
            ok.addActionListener(e -> dlg.dispose());
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(BG_COLOR);
            btnPanel.add(ok);
            dlg.getContentPane().add(btnPanel, BorderLayout.SOUTH);
            dlg.pack();
            dlg.setVisible(true);
            onSuccess.run();
        } else {
            JDialog dlg = createDialog(parent, "错误", UIManager.getIcon("OptionPane.errorIcon"), "登录失败：用户名或密码错误");
            JButton ok = new JButton("确定");
            ok.setFont(MESSAGE_FONT);
            ok.setBackground(BUTTON_BG);
            ok.setForeground(BUTTON_FG);
            ok.addActionListener(e -> dlg.dispose());
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(BG_COLOR);
            btnPanel.add(ok);
            dlg.getContentPane().add(btnPanel, BorderLayout.SOUTH);
            dlg.pack();
            dlg.setVisible(true);
        }
    }

    /**
     * 弹出注册对话框
     */
    public static void showRegisterDialog(Component parent, Runnable onSuccess) {
        JTextField userField = new JTextField(15);
        JPasswordField pwdField = new JPasswordField(15);
        JPanel p = new JPanel(new GridLayout(2, 2, 5, 5));
        p.add(new JLabel("用户名:"));
        p.add(userField);
        p.add(new JLabel("密码:"));
        p.add(pwdField);

        int result = JOptionPane.showConfirmDialog(parent, p, "用户注册", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String u = userField.getText().trim();
        String pw = new String(pwdField.getPassword());
        if (userController.register(u, pw)) {
            userController.login(u, pw);
            JDialog dlg = createDialog(parent, "成功", UIManager.getIcon("OptionPane.informationIcon"), "注册成功！");
            JButton ok = new JButton("确定");
            ok.setFont(MESSAGE_FONT);
            ok.setBackground(BUTTON_BG);
            ok.setForeground(BUTTON_FG);
            ok.addActionListener(e -> dlg.dispose());
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(BG_COLOR);
            btnPanel.add(ok);
            dlg.getContentPane().add(btnPanel, BorderLayout.SOUTH);
            dlg.pack();
            dlg.setVisible(true);
            onSuccess.run();
        } else {
            JDialog dlg = createDialog(parent, "错误", UIManager.getIcon("OptionPane.errorIcon"), "用户名已存在，注册失败！");
            JButton ok = new JButton("确定");
            ok.setFont(MESSAGE_FONT);
            ok.setBackground(BUTTON_BG);
            ok.setForeground(BUTTON_FG);
            ok.addActionListener(e -> dlg.dispose());
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(BG_COLOR);
            btnPanel.add(ok);
            dlg.getContentPane().add(btnPanel, BorderLayout.SOUTH);
            dlg.pack();
            dlg.setVisible(true);
        }
    }

    /**
     * 自定义信息对话框
     */
    public static void showInfo(Component parent, String message, String title) {
        JDialog dlg = createDialog(parent, title, UIManager.getIcon("OptionPane.informationIcon"), message);
        JButton ok = new JButton("确定");
        ok.setFont(MESSAGE_FONT);
        ok.setBackground(BUTTON_BG);
        ok.setForeground(BUTTON_FG);
        ok.addActionListener(e -> dlg.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG_COLOR);
        btnPanel.add(ok);
        dlg.getContentPane().add(btnPanel, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setVisible(true);
    }

    /**
     * 自定义警告对话框
     */
    public static void showWarn(Component parent, String message, String title) {
        JDialog dlg = createDialog(parent, title, UIManager.getIcon("OptionPane.warningIcon"), message);
        JButton ok = new JButton("确定");
        ok.setFont(MESSAGE_FONT);
        ok.setBackground(BUTTON_BG);
        ok.setForeground(BUTTON_FG);
        ok.addActionListener(e -> dlg.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG_COLOR);
        btnPanel.add(ok);
        dlg.getContentPane().add(btnPanel, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setVisible(true);
    }

    /**
     * 自定义错误对话框
     */
    public static void showError(Component parent, String message, String title) {
        JDialog dlg = createDialog(parent, title, UIManager.getIcon("OptionPane.errorIcon"), message);
        JButton ok = new JButton("确定");
        ok.setFont(MESSAGE_FONT);
        ok.setBackground(BUTTON_BG);
        ok.setForeground(BUTTON_FG);
        ok.addActionListener(e -> dlg.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG_COLOR);
        btnPanel.add(ok);
        dlg.getContentPane().add(btnPanel, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setVisible(true);
    }
}