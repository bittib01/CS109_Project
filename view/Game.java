package view;

import model.*;
import util.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class Game extends JFrame {
    private final Board board;
    private final int cellSize = 100;  // 单元格像素大小
    private BoardPanel panel;

    // —— 动画相关状态 ——
    private Timer animTimer;            // Swing 定时器
    private Block animBlock;            // 正在动画的方块
    private Point animStart, animEnd;   // 起始/结束格子坐标
    private int animStep, animSteps;    // 当前步数/总步数

    public Game(MapModel map) {
        this.board = new Board(map);
        initUI();
    }

    private void initUI() {
        setTitle("华容道");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        panel = new BoardPanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Block focused = board.getFocused();
                Block.Direction dir = switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> Block.Direction.UP;
                    case KeyEvent.VK_DOWN -> Block.Direction.DOWN;
                    case KeyEvent.VK_LEFT -> Block.Direction.LEFT;
                    case KeyEvent.VK_RIGHT -> Block.Direction.RIGHT;
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
    }

    /**
     * 平滑动画移动：先在模型中更新格子，再启动定时器按像素插值绘制
     */
    private void animateMove(Block b, Block.Direction dir) {
        // 1. 记录起始格子
        Point oldPos = b.getPosition();

        // 2. 尝试移动模型，若失败则直接返回
        if (!board.moveBlock(b, dir)) return;

        // 3. 初始化动画状态
        animBlock  = b;
        animStart  = oldPos;
        animEnd    = b.getPosition();
        animSteps  = 10;                   // 分 15 步完成插值
        animStep   = 0;
        int delay  = (int)(10 * b.getInertia());

        // 4. 启动定时器
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animTimer = new Timer(delay, e -> {
            animStep++;
            panel.repaint();
            if (animStep >= animSteps) {
                ((Timer)e.getSource()).stop();
                animBlock = null;   // 动画结束
                panel.repaint();
            }
        });
        animTimer.start();
    }

    /**
     * 负责绘制和鼠标交互
     */
    private class BoardPanel extends JPanel {
        private Point draggingPoint;
        private Block draggingBlock;
        private long pressTime;
        private static final int CLICK_THRESHOLD = 100; // 毫秒

        public BoardPanel() {
            int w = Config.cols * cellSize;
            int h = Config.rows * cellSize;
            setPreferredSize(new Dimension(w, h));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    pressTime = System.currentTimeMillis();
                    int r = e.getY() / cellSize;
                    int c = e.getX() / cellSize;
                    draggingPoint = new Point(e.getY(), e.getX());
                    Block b = board.getBlockAt(new Point(r, c));
                    draggingBlock = b;
                    board.setFocused(b);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    long releaseTime = System.currentTimeMillis();
                    long duration = releaseTime - pressTime;

                    if (draggingBlock != null && duration >= CLICK_THRESHOLD) {
                        // 仅当按下时间超过阈值才视为拖动操作
                        int dr = e.getY() - draggingPoint.x;
                        int dc = e.getX() - draggingPoint.y;
                        Block.Direction dir;
                        if (Math.abs(dr) > Math.abs(dc)) {
                            dir = dr > 0 ? Block.Direction.DOWN : Block.Direction.UP;
                            animateMove(draggingBlock, dir);
                        } else if (Math.abs(dc) > Math.abs(dr)) {
                            dir = dc > 0 ? Block.Direction.RIGHT : Block.Direction.LEFT;
                            animateMove(draggingBlock, dir);
                        }
                    }
                    // 如果时间较短则认为是点击，不执行拖动，仅更新焦点显示
                    draggingBlock = null;
                    draggingPoint = null;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            List<Block> list = board.getBlocks();
            for (Block b : list) {
                int drawX, drawY;
                if (b == animBlock) {
                    float frac = (float)animStep / animSteps;
                    int startX = animStart.y * cellSize;
                    int startY = animStart.x * cellSize;
                    int endX   = animEnd.y   * cellSize;
                    int endY   = animEnd.x   * cellSize;
                    drawX = Math.round(startX + (endX - startX) * frac);
                    drawY = Math.round(startY + (endY - startY) * frac);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MapModel map = new MapModel("map.txt");
            new Game(map);
        });
    }
}
