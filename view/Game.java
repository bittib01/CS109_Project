package view;

import model.GameMap;
import model.Board;
import model.Block;
import util.Log;
import util.Saver;
import util.UserController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 游戏主面板（包含顶部导航栏和游戏控制逻辑）
 */
public class Game extends JPanel {
    /** 游戏模式枚举：普通、限时、限步 */
    public enum Mode {
        NORMAL("普通模式"),
        TIME_LIMIT("限时模式"),
        MOVE_LIMIT("限步模式");

        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final Mode mode;                          // 当前模式
    private final UserController userController = UserController.getInstance();
    private JPanel topBar;                            // 顶部导航栏，用于刷新
    private final GameMap map;                        // 地图模型

    private int completedCount = 0;                   // 完成次数统计
    private long bestTime;                            // 最佳时长
    private int bestMoves;                            // 最佳步数

    private Board board;                              // 棋盘模型
    private InteractiveBoardPanel panel;              // 交互面板
    private Basic basic;                              // 顶层面板管理器
    private final Log log = Log.getInstance();        // 日志

    private JLabel timeLabel;                         // 时间显示
    private JLabel movesLabel;                        // 步数显示
    private long startTime;                           // 游戏开始时间
    private Timer clockTimer;                         // 定时器

    private JPanel controlPanel;                      // 右侧控制面板
    private boolean isControlPanelVisible = false;    // 控制面板显隐

    /** 构造函数：初始化数据、加载历史统计，并构建界面 */
    public Game(Basic basic, GameMap map, Mode mode) {
        this.basic = basic;
        this.map = map;
        this.mode = mode;
        this.board = new Board(map);

        // 从存档读取统计
        Optional<Saver.Stats> opt = Saver.getStats(
                userController.getCurrentUser().getUsername(), map);
        if (opt.isPresent()) {
            Saver.Stats s = opt.get();
            this.completedCount = s.getCompletedCount();
            this.bestTime = s.getBestTime();
            this.bestMoves = s.getBestMoves();
        } else {
            // 默认无历史
            this.completedCount = 0;
            this.bestTime = Long.MAX_VALUE;
            this.bestMoves = Integer.MAX_VALUE;
        }

        initUI();
    }

    /** 初始化界面布局与定时器 */
    private void initUI() {
        setLayout(new BorderLayout());
        panel = new InteractiveBoardPanel(board);
        panel.setBackground(Color.WHITE);
        panel.setFocusable(true);
        add(panel, BorderLayout.CENTER);
        SwingUtilities.invokeLater(panel::requestFocusInWindow);

        topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        startTime = System.currentTimeMillis();
        clockTimer = new Timer(1000, e -> updateTimeLabel());
        clockTimer.start();
    }

    /** 刷新顶部导航栏 */
    private void refreshTopBar() {
        remove(topBar);
        topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);
        revalidate(); repaint();
    }

    /** 构建顶部导航栏 */
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        topBar.setBackground(new Color(0xF0F0F0));

        // 左侧：用户信息与登录登出
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,15,5));
        left.add(new JLabel(userController.getCurrentUser().getUsername()));
        if (!"Guest".equals(userController.getCurrentUser().getUsername())) {
            // 登出
            JButton logout = new JButton("退出登录");
            logout.addActionListener(e -> { userController.switchUser("Guest","Guest"); refreshTopBar(); });
            left.add(logout);
            // 存档
            JButton save = new JButton("存档");
            save.addActionListener(e -> {
                String user = userController.getCurrentUser().getUsername();
                Saver.saveManual(board, map, user,
                        completedCount, bestTime, bestMoves,
                        mode.toString(), board.getHistory(),
                        System.currentTimeMillis() - startTime);
                JOptionPane.showMessageDialog(this, "手动存档完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                panel.requestFocusInWindow();
            });
            left.add(save);
            // 读取存档
            JButton load = new JButton("读取存档");
            load.addActionListener(e -> {
                load();
            });
            left.add(load);
        } else {
            // 登录/注册
            JButton login = new JButton("登录"); login.addActionListener(e -> showLoginDialog()); left.add(login);
            JButton reg   = new JButton("注册"); reg.addActionListener(e -> showRegisterDialog()); left.add(reg);
        }

        // 右侧：返回、重玩、撤销、控制面板、步数、时间
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,15,5));
        // 返回选单
        JButton toMenu=new JButton("返回选单");
        toMenu.addActionListener(e->{
            clockTimer.stop();
            basic.showPanel("select");
            Select s=(Select)basic.getPanel("select"); s.updateInfoPanel(map); s.updateStats(map); s.refreshTopBar();
        }); right.add(toMenu);
        // 重玩
        JButton replay=new JButton("重玩"); replay.addActionListener(e->replay()); right.add(replay);
        // 撤销
        JButton undo=new JButton("撤销"); undo.addActionListener(e->{ if(board.undo()){panel.repaint();} else Toolkit.getDefaultToolkit().beep(); panel.requestFocusInWindow(); }); right.add(undo);
        // 控制
        JButton ctrl=new JButton("按钮"); ctrl.addActionListener(e->{toggleControlPanel();panel.requestFocusInWindow();}); right.add(ctrl);
        // 步数
        movesLabel=new JLabel("步数: " + board.getHistory().size()); movesLabel.setFont(new Font("微软雅黑",Font.PLAIN,14)); right.add(movesLabel);
        // 时间
        timeLabel=new JLabel("00:00"); timeLabel.setFont(new Font("微软雅黑",Font.PLAIN,14)); right.add(timeLabel);

        topBar.add(left, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);
        return topBar;
    }

    public void load() {
        String user = userController.getCurrentUser().getUsername();
        try {
            long elapsed = Saver.load(board, map, user);
            panel.repaint();
            startTime = System.currentTimeMillis() - elapsed;
            if (!clockTimer.isRunning()) clockTimer.start();
            JOptionPane.showMessageDialog(this, "读取存档成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "读取存档失败："+ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "警告", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "读取存档错误："+ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
        panel.requestFocusInWindow();
    }

    /**
     * 弹出登录对话框
     */
    private void showLoginDialog() {
        JTextField userField = new JTextField(15);
        JPasswordField pwdField = new JPasswordField(15);
        JPanel p = new JPanel(new GridLayout(2,2,5,5));
        p.add(new JLabel("用户名:")); p.add(userField);
        p.add(new JLabel("密码:")); p.add(pwdField);
        int r = JOptionPane.showConfirmDialog(this, p, "用户登录", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            String u = userField.getText().trim();
            String pw = new String(pwdField.getPassword());
            if (userController.login(u, pw)) {
                refreshTopBar();
            } else {
                JOptionPane.showMessageDialog(this, "登录失败：用户名或密码错误", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 弹出注册对话框
     */
    private void showRegisterDialog() {
        JTextField userField = new JTextField(15);
        JPasswordField pwdField = new JPasswordField(15);
        JPanel p = new JPanel(new GridLayout(2,2,5,5));
        p.add(new JLabel("用户名:")); p.add(userField);
        p.add(new JLabel("密码:")); p.add(pwdField);
        int r = JOptionPane.showConfirmDialog(this, p, "用户注册", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            String u = userField.getText().trim();
            String pw = new String(pwdField.getPassword());
            if (userController.register(u, pw)) {
                userController.login(u, pw);
                refreshTopBar();
            } else {
                JOptionPane.showMessageDialog(this, "用户名已存在，注册失败！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 切换控制面板显示状态
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
        JPanel cp = new JPanel(new GridBagLayout());
        cp.setPreferredSize(new Dimension(180, 0));
        cp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        cp.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        JButton up = new JButton("↑");
        c.gridx = 1; c.gridy = 0; cp.add(up, c);
        JButton left = new JButton("←");
        c.gridx = 0; c.gridy = 1; cp.add(left, c);
        JButton right = new JButton("→");
        c.gridx = 2; c.gridy = 1; cp.add(right, c);
        JButton down = new JButton("↓");
        c.gridx = 1; c.gridy = 2; cp.add(down, c);

        up.addActionListener(e -> panel.handleDirection(Block.Direction.UP));
        down.addActionListener(e -> panel.handleDirection(Block.Direction.DOWN));
        left.addActionListener(e -> panel.handleDirection(Block.Direction.LEFT));
        right.addActionListener(e -> panel.handleDirection(Block.Direction.RIGHT));

        return cp;
    }

    /**
     * 更新顶部时间和步数，并检查限时/限步失败
     */
    private void updateTimeLabel() {
        long elapsed = System.currentTimeMillis() - startTime;
        int sec = (int) (elapsed / 1000) % 60;
        int min = (int) (elapsed / 1000) / 60;
        String timeText = String.format("%02d:%02d", min, sec);

        if (mode == Mode.TIME_LIMIT) {
            int limit = map.getTimeLimit();
            timeText += String.format("/%02d:%02d", limit / 60, limit % 60);
            if (elapsed > limit * 1000L) {
                int mv = board.getHistory().size();
                basic.addPanel("failure", new Failure(basic, map, mode, mv, elapsed));
                replay();
                clockTimer.stop();
                basic.showPanel("failure");
                return;
            }
        }
        timeLabel.setText(timeText);

        int mv = board.getHistory().size();
        String mvText = "步数: " + mv;
        if (mode == Mode.MOVE_LIMIT) {
            int lim = map.getMoveLimit();
            mvText += "/" + lim;
            if (mv > lim) {
                clockTimer.stop();
                long el2 = System.currentTimeMillis() - startTime;
                basic.addPanel("failure", new Failure(basic, map, mode, mv, el2));
                replay();
                basic.showPanel("failure");
                return;
            }
        }
        movesLabel.setText(mvText);
    }

    /**
     * 带动画与交互的棋盘面板
     */
    private class InteractiveBoardPanel extends BoardPanelBase {
        private Timer animTimer;
        private Block animBlock;
        private Point animStart, animEnd;
        private int animStep, animSteps;
        private Point dragStart;
        private Block dragBlock;
        private long pressTime;
        private static final int CLICK_THRESH = 100;

        public InteractiveBoardPanel(Board board) {
            super(board);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    Block f = board.getFocused();
                    Block.Direction d = switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP -> Block.Direction.UP;
                        case KeyEvent.VK_DOWN -> Block.Direction.DOWN;
                        case KeyEvent.VK_LEFT -> Block.Direction.LEFT;
                        case KeyEvent.VK_RIGHT -> Block.Direction.RIGHT;
                        default -> null;
                    };
                    if (d == null) return;
                    if (e.isControlDown()) {
                        board.moveFocus(d);
                        repaint();
                    } else if (f != null) {
                        startAnimation(f, d);
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    pressTime = System.currentTimeMillis();
                    dragStart = e.getPoint();
                    Point cell = pointToCell(dragStart);
                    dragBlock = board.getBlockAt(cell);
                    board.setFocused(dragBlock);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    long dur = System.currentTimeMillis() - pressTime;
                    if (dragBlock != null && dur >= CLICK_THRESH) {
                        int dx = e.getX() - dragStart.x;
                        int dy = e.getY() - dragStart.y;
                        Block.Direction d = Math.abs(dy) > Math.abs(dx)
                                ? (dy > 0 ? Block.Direction.DOWN : Block.Direction.UP)
                                : (dx > 0 ? Block.Direction.RIGHT : Block.Direction.LEFT);
                        startAnimation(dragBlock, d);
                    }
                    dragBlock = null;
                }
            });
        }

        public void handleDirection(Block.Direction dir) {
            Block f = board.getFocused();
            if (f != null) startAnimation(f, dir);
            requestFocusInWindow();
        }

        private Point pointToCell(Point p) {
            int rows = board.getRows(), cols = board.getCols();
            int cs = Math.min(getWidth() / cols, getHeight() / rows);
            int bw = cs * cols, bh = cs * rows;
            int xo = (getWidth() - bw) / 2, yo = (getHeight() - bh) / 2;
            return new Point((p.y - yo) / cs, (p.x - xo) / cs);
        }

        public void startAnimation(Block b, Block.Direction dir) {
            if (clockTimer != null && !clockTimer.isRunning() && board.isVictory()) return;
            Point oldPos = b.getPosition();
            if (!board.moveBlock(b, dir)) return;

            Game.this.updateTimeLabel();
            animBlock = b;
            animStart = oldPos;
            animEnd = b.getPosition();
            animSteps = 12;
            animStep = 0;
            int delay = Math.max(8, (int) (8 * b.getInertia()));
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
                        completedCount++;
                        long e = System.currentTimeMillis() - startTime;
                        bestTime = Math.min(bestTime, e);
                        int mv = board.getHistory().size();
                        bestMoves = Math.min(bestMoves, mv);
                        boolean nt = e < bestTime;
                        boolean nm = mv < bestMoves;
                        Saver.saveResult(
                                board, map, userController.getCurrentUser().getUsername(),
                                completedCount, bestTime, bestMoves, mode.toString(),
                                board.getHistory(), e
                        );
                        basic.addPanel("victory", new Victory(basic, map, mode, mv, e, nt, nm));
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

                // 计数
                Map<Block.Type, Integer> typeCount = new HashMap<>();

                g2.setColor(typeColor.getOrDefault(animBlock.getType(), new Color(0xCCCCCC)));
                g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);

                // 描边:如果这是当前焦点块，就再用红色粗线框起来
                if (animBlock.equals(board.getFocused())) {
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(3));
                    g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
                } else {
                    g2.setColor(new Color(0x888888));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
                }


                List<String> names;
                switch (animBlock.getType()) {
                    case SMALL:      names = smallNames;      break;
                    case HORIZONTAL: names = horizontalNames; break;
                    case VERTICAL:   names = verticalNames;   break;
                    case LARGE:      names = largeNames;      break;
                    default:         names = List.of("");
                }
                if (!names.isEmpty()) {
                    List<Block> sameTypeBlocks = board.getBlocks().stream()
                            .filter(block -> block.getType() == animBlock.getType())
                            .toList();
                    int idx = sameTypeBlocks.indexOf(animBlock);
                    String label = names.get(idx % names.size());

                    Font font = getFont().deriveFont(Font.BOLD, cellSize * 0.4f);
                    g2.setFont(font);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(label), th = fm.getAscent();
                    g2.setColor(new Color(0x333333));
                    g2.drawString(label, x + (w - tw) / 2, y + (h + th) / 2 - 4);
                }
            }
            skipBlock = null;

        }
    }
    /**
     * 重置和重新开始游戏
     */
    public void replay() {
        board.reset(); panel.repaint(); startTime=System.currentTimeMillis();
        if (!clockTimer.isRunning()) clockTimer.start();
        panel.requestFocusInWindow();
    }
}
