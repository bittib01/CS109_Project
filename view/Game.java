package view;

import model.*;
import util.Config;
import util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 游戏主面板，嵌入在 Basic 窗口中，负责游戏逻辑及绘制。
 */
public class Game extends JPanel {
    Log log = Log.getInstance();
    private final Board board;                  // 游戏模型
    private final int cellSize = 100;           // 单元格像素大小
    private BoardPanel panel;
    private Basic basic;                        // 基础窗口引用，用于切换到胜利界面

    // —— 动画相关状态 ——
    private Timer animTimer;                    // Swing 定时器
    private Block animBlock;                    // 正在动画的方块
    private Point animStart, animEnd;           // 起始/结束格子坐标
    private int animStep, animSteps;            // 当前步数/总步数

    /**
     * 构造方法：初始化游戏面板
     * @param basic  基础窗口，用于切换页面
     * @param map    地图模型
     */
    public Game(Basic basic, Map map) {
        this.basic = basic;
        this.board = new Board(map);
        initUI();
    }

    /**
     * 初始化 UI 和键鼠事件
     */
    private void initUI() {
        Config config = Config.getInstance();
        setLayout(new BorderLayout());
        panel = new BoardPanel();
        add(panel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(config.getInt("cols") * cellSize, config.getInt("rows") * cellSize));

        // 确保面板可获取焦点并监听键盘
        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Block focused = board.getFocused();
                Block.Direction dir = switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> {
                        log.detail("VK_UP");
                        yield Block.Direction.UP;
                    }
                    case KeyEvent.VK_DOWN -> {
                        log.detail("VK_DOWN");
                        yield Block.Direction.DOWN;
                    }
                    case KeyEvent.VK_LEFT -> {
                        log.detail("VK_LEFT");
                        yield Block.Direction.LEFT;
                    }
                    case KeyEvent.VK_RIGHT -> {
                        log.detail("VK_RIGHT");
                        yield Block.Direction.RIGHT;
                    }
                    default -> null;
                };
                if (dir == null) return;
                if (e.isControlDown()) {
                    board.moveFocus(dir);
                    panel.repaint();
                } else if (focused != null) {
                    animateMove(focused, dir);
                }
            }
        });
        // 确保初始化完成后获取焦点
        SwingUtilities.invokeLater(() -> panel.requestFocusInWindow());
    }

    /**
     * 平滑动画移动：先在模型中更新格子，再启动定时器按像素插值绘制，结束后检测胜利
     */
    private void animateMove(Block b, Block.Direction dir) {
        Point oldPos = b.getPosition();
        if (!board.moveBlock(b, dir)) return;  // 无法移动

        // 初始化动画状态
        animBlock = b;
        animStart = oldPos;
        animEnd   = b.getPosition();
        animSteps = 10;
        animStep  = 0;
        int delay = (int)(10 * b.getInertia());

        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        animTimer = new Timer(delay, e -> {
            animStep++;
            panel.repaint();
            if (animStep >= animSteps) {
                ((Timer)e.getSource()).stop();
                animBlock = null;
                panel.repaint();
                // 动画结束后检测胜利
                if (board.isVictory()) {
                    basic.showPanel("victory");
                }
            }
        });
        animTimer.start();
    }

    /**
     * 内部绘制与鼠标交互面板
     */
    private class BoardPanel extends JPanel {
        private Point draggingPoint;
        private Block draggingBlock;
        private long pressTime;
        private static final int CLICK_THRESHOLD = 100; // 毫秒

        public BoardPanel() {
            Config config = Config.getInstance();
            setPreferredSize(new Dimension( config.getInt("cols") * cellSize, config.getInt("rows") * cellSize));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    pressTime = System.currentTimeMillis();
                    int r = e.getY() / cellSize;
                    int c = e.getX() / cellSize;
                    draggingPoint = e.getPoint();
                    draggingBlock = board.getBlockAt(new Point(r, c));
                    board.setFocused(draggingBlock);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    long duration = System.currentTimeMillis() - pressTime;
                    if (draggingBlock != null && duration >= CLICK_THRESHOLD) {
                        int dr = e.getY() - draggingPoint.y;
                        int dc = e.getX() - draggingPoint.x;
                        Block.Direction dir;
                        if (Math.abs(dr) > Math.abs(dc)) {
                            dir = dr > 0 ? Block.Direction.DOWN : Block.Direction.UP;
                        } else {
                            dir = dc > 0 ? Block.Direction.RIGHT : Block.Direction.LEFT;
                        }
                        animateMove(draggingBlock, dir);
                    }
                    draggingBlock = null;
                    draggingPoint = null;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (Block b : board.getBlocks()) {
                int drawX, drawY;
                if (b == animBlock) {
                    float frac = (float)animStep / animSteps;
                    int sx = animStart.y * cellSize;
                    int sy = animStart.x * cellSize;
                    int ex = animEnd.y * cellSize;
                    int ey = animEnd.x * cellSize;
                    drawX = Math.round(sx + (ex - sx) * frac);
                    drawY = Math.round(sy + (ey - sy) * frac);
                } else {
                    Point pos = b.getPosition();
                    drawX = pos.y * cellSize;
                    drawY = pos.x * cellSize;
                }
                int w = b.getSize().width * cellSize;
                int h = b.getSize().height * cellSize;
                g.setColor(b == board.getFocused() ? Color.ORANGE : Color.GRAY);
                g.fillRoundRect(drawX + 5, drawY + 5, w - 10, h - 10, 20, 20);
            }
        }
    }
}
