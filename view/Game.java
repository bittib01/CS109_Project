package view;

import model.GameMap;
import model.Board;
import model.Block;
import util.Log;
import util.UserController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 游戏主面板（包含顶部导航栏）
 */
public class Game extends JPanel {
    private final UserController userController = UserController.getInstance();
    private JPanel topBar;  // 保存顶部导航栏引用用于刷新

    private Board board;
    private InteractiveBoardPanel panel;
    private Basic basic;
    private final Log log = Log.getInstance();

    // 计时标签与定时器
    private JLabel timeLabel;
    private long startTime;
    private Timer clockTimer;

    // 右侧控制窗口相关组件
    private JPanel controlPanel;
    private boolean isControlPanelVisible = false;

    public Game(Basic basic, GameMap map) {
        this.basic = basic;
        this.board = new Board(map);
        initUI();
    }

    /**
     * 初始化游戏主面板及顶部导航栏
     */
    private void initUI() {
        setLayout(new BorderLayout());

        // 主游戏面板
        panel = new InteractiveBoardPanel(board);
        panel.setBackground(Color.white);
        panel.setFocusable(true);
        add(panel, BorderLayout.CENTER);
        SwingUtilities.invokeLater(panel::requestFocusInWindow);

        // 创建并保存顶部导航栏
        topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        // 初始化并启动计时器
        startTime = System.currentTimeMillis();
        clockTimer = new Timer(1000, e -> updateTimeLabel());
        clockTimer.start();
    }

    /**
     * 刷新顶部导航栏
     */
    private void refreshTopBar() {
        remove(topBar);
        topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    /**
     * 创建顶部导航栏
     */
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topBar.setBackground(new Color(0xF0F0F0));

        // 左侧用户状态
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        leftPanel.add(new JLabel(userController.getCurrentUser().getUsername()));
        if (!"Guest".equals(userController.getCurrentUser().getUsername())) {
            JButton logoutButton = new JButton("退出登录");
            logoutButton.addActionListener(e -> {
                userController.switchUser("Guest", "Guest");
                refreshTopBar();
            });
            leftPanel.add(logoutButton);

            JButton saveButton = new JButton("存档");
            leftPanel.add(saveButton);
        } else {
            JButton loginButton = new JButton("登录");
            loginButton.addActionListener(e -> showLoginDialog());
            leftPanel.add(loginButton);
            JButton registerButton = new JButton("注册");
            registerButton.addActionListener(e -> showRegisterDialog());
            leftPanel.add(registerButton);
        }

        // 右侧控制按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        // “选单”按钮
        JButton selectButton = new JButton("返回选单");
        selectButton.addActionListener(e -> {
            basic.showPanel("select");
        });
        rightPanel.add(selectButton);

        // “重玩”按钮
        JButton replayButton = new JButton("重玩");
        replayButton.addActionListener(e -> {
            board.reset();               // 重置到初始状态
            panel.repaint();             // 刷新界面
            startTime = System.currentTimeMillis();  // 重置计时
            if (!clockTimer.isRunning()) clockTimer.start();
            panel.requestFocusInWindow();
        });
        rightPanel.add(replayButton);

        // “撤销”按钮
        JButton undoButton = new JButton("撤销");
        undoButton.addActionListener(e -> {
            if (board.undo()) {
                panel.repaint();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
            panel.requestFocusInWindow();
        });
        rightPanel.add(undoButton);

        JButton controlButton = new JButton("按钮");
        controlButton.addActionListener(e -> toggleControlPanel());
        rightPanel.add(controlButton);

        timeLabel = new JLabel("00:00");
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        rightPanel.add(timeLabel);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        return topBar;
    }

    /**
     * 显示登录对话框（关键修改点4：实现登录功能）
     */
    private void showLoginDialog() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        JPanel loginPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        loginPanel.add(new JLabel("用户名:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("密码:"));
        loginPanel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(
                this,
                loginPanel,
                "用户登录",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (userController.login(username, password)) {  // 调用UserController登录方法
                refreshTopBar();  // 登录成功后刷新导航栏
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "登录失败：用户名或密码错误",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * 显示注册对话框（关键修改点4：实现登录功能）
     */
    private void showRegisterDialog() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        JPanel registerPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        registerPanel.add(new JLabel("用户名:"));
        registerPanel.add(usernameField);
        registerPanel.add(new JLabel("密码:"));
        registerPanel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(
                this,
                registerPanel,
                "用户注册",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (userController.register(username, password)) {  // 调用UserController登录方法
                userController.login(username, password);
                refreshTopBar();  // 登录成功后刷新导航栏
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "用户名已存在，注册失败！",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * 切换右侧控制窗口显示状态
     */
    private void toggleControlPanel() {
        if (isControlPanelVisible) {
            remove(controlPanel);
        } else {
            controlPanel = createControlPanel();
            add(controlPanel, BorderLayout.EAST);
        }
        isControlPanelVisible = !isControlPanelVisible;
        revalidate();
        repaint();
    }

    /**
     * 创建右侧控制窗口（包含方向按钮）
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setPreferredSize(new Dimension(180, 0));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlPanel.setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        JButton up = new JButton("↑");
        c.gridx = 1; c.gridy = 0; controlPanel.add(up, c);

        JButton left = new JButton("←");
        c.gridx = 0; c.gridy = 1; controlPanel.add(left, c);

        JButton right = new JButton("→");
        c.gridx = 2; c.gridy = 1; controlPanel.add(right, c);

        JButton down = new JButton("↓");
        c.gridx = 1; c.gridy = 2; controlPanel.add(down, c);

        // 绑定方向按钮事件（复用原侧边栏逻辑）
        up.addActionListener(e -> panel.handleDirection(Block.Direction.UP));
        down.addActionListener(e -> panel.handleDirection(Block.Direction.DOWN));
        left.addActionListener(e -> panel.handleDirection(Block.Direction.LEFT));
        right.addActionListener(e -> panel.handleDirection(Block.Direction.RIGHT));

        return controlPanel;
    }

    /**
     * 更新计时显示
     */
    private void updateTimeLabel() {
        long elapsed = System.currentTimeMillis() - startTime;
        int sec = (int) (elapsed / 1000) % 60;
        int min = (int) (elapsed / 1000) / 60;
        timeLabel.setText(String.format("%02d:%02d", min, sec));
    }

    /**
     * 带键鼠交互与动画的棋盘面板（未修改核心逻辑）
     */
    private class InteractiveBoardPanel extends BoardPanelBase {
        private Timer animTimer;
        private Block animBlock;
        private Point animStart, animEnd;
        private int animStep, animSteps;
        private Point dragStartPoint;
        private Block dragBlock;
        private long pressTime;
        private static final int CLICK_THRESH = 100;

        public InteractiveBoardPanel(Board board) {
            super(board);
            // 键盘监听
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    Block focus = board.getFocused();
                    Block.Direction dir = switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP    -> Block.Direction.UP;
                        case KeyEvent.VK_DOWN  -> Block.Direction.DOWN;
                        case KeyEvent.VK_LEFT  -> Block.Direction.LEFT;
                        case KeyEvent.VK_RIGHT -> Block.Direction.RIGHT;
                        default -> null;
                    };
                    if (dir == null) return;
                    if (e.isControlDown()) {
                        board.moveFocus(dir);
                        repaint();
                    } else if (focus != null) {
                        startAnimation(focus, dir);
                    }
                }
            });
            // 鼠标监听
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    pressTime = System.currentTimeMillis();
                    dragStartPoint = e.getPoint();
                    Point cell = pointToCell(dragStartPoint);
                    dragBlock = board.getBlockAt(cell);
                    board.setFocused(dragBlock);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    long dur = System.currentTimeMillis() - pressTime;
                    if (dragBlock != null && dur >= CLICK_THRESH) {
                        int dx = e.getX() - dragStartPoint.x;
                        int dy = e.getY() - dragStartPoint.y;
                        Block.Direction dir = Math.abs(dy) > Math.abs(dx)
                                ? (dy > 0 ? Block.Direction.DOWN : Block.Direction.UP)
                                : (dx > 0 ? Block.Direction.RIGHT : Block.Direction.LEFT);
                        startAnimation(dragBlock, dir);
                    }
                    dragBlock = null;
                }
            });
        }

        public void handleDirection(Block.Direction dir) {
            Block focus = board.getFocused();
            if (focus != null) startAnimation(focus, dir);
            requestFocusInWindow();
        }

        private Point pointToCell(Point p) {
            int rows = board.getRows(), cols = board.getCols();
            int cellSize = Math.min(getWidth() / cols, getHeight() / rows);
            int boardW = cellSize * cols, boardH = cellSize * rows;
            int xOffset = (getWidth() - boardW) / 2, yOffset = (getHeight() - boardH) / 2;
            int c = (p.x - xOffset) / cellSize;
            int r = (p.y - yOffset) / cellSize;
            return new Point(r, c);
        }

        public void startAnimation(Block b, Block.Direction dir) {
            if (clockTimer != null && !clockTimer.isRunning() && board.isVictory()) return;
            Point oldPos = b.getPosition();
            if (!board.moveBlock(b, dir)) return;
            animBlock = b;
            animStart = oldPos;
            animEnd = b.getPosition();
            animSteps = 12;
            animStep = 0;
            int delay = Math.max(8, (int)(8 * b.getInertia()));
            if (animTimer != null && animTimer.isRunning()) animTimer.stop();
            animTimer = new Timer(delay, ae -> {
                animStep++;
                repaint();
                if (animStep >= animSteps) {
                    animTimer.stop();
                    animBlock = null;
                    repaint();
                    if (board.isVictory()) {
                        clockTimer.stop();
                        basic.showPanel("victory");
                    }
                }
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            skipBlock = animBlock;
            super.paintComponent(g);
            if (animBlock != null) {
                Graphics2D g2 = (Graphics2D) g;
                float frac = (float)animStep / animSteps;
                int rows = board.getRows(), cols = board.getCols();
                int cellSize = Math.min(getWidth() / cols, getHeight() / rows);
                int boardW = cellSize * cols, boardH = cellSize * rows;
                int xOffset = (getWidth() - boardW) / 2, yOffset = (getHeight() - boardH) / 2;

                int sx = xOffset + animStart.y * cellSize;
                int sy = yOffset + animStart.x * cellSize;
                int ex = xOffset + animEnd.y * cellSize;
                int ey = yOffset + animEnd.x * cellSize;
                int x = Math.round(sx + (ex - sx) * frac);
                int y = Math.round(sy + (ey - sy) * frac);
                int w = animBlock.getSize().width * cellSize;
                int h = animBlock.getSize().height * cellSize;

                g2.setColor(typeColor.getOrDefault(animBlock.getType(), new Color(0xCCCCCC)));
                g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
                g2.setColor(new Color(0x888888));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);

                String txt = typeLabel.getOrDefault(animBlock.getType(), "");
                if (!txt.isEmpty()) {
                    Font font = getFont().deriveFont(Font.BOLD, cellSize * 0.4f);
                    g2.setFont(font);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(txt), th = fm.getAscent();
                    g2.setColor(new Color(0x333333));
                    g2.drawString(txt, x + (w - tw) / 2, y + (h + th) / 2 - 4);
                }
            }
            skipBlock = null;
        }
    }
}

