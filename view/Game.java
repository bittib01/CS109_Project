package view;

import model.GameMap;
import model.Board;
import model.Block;
import util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 游戏主面板
 */
public class Game extends JPanel {
    private final Board board;
    private InteractiveBoardPanel panel;
    private Basic basic;
    private final Log log = Log.getInstance();

    public Game(Basic basic, GameMap map) {
        this.board = new Board(map);
        this.basic = basic;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        panel = new InteractiveBoardPanel(board);
        add(panel, BorderLayout.CENTER);
        panel.setBackground(Color.white);
        panel.setFocusable(true);
        SwingUtilities.invokeLater(panel::requestFocusInWindow);
    }

    /**
     * 带键鼠交互与移动动画的棋盘面板
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
                        case KeyEvent.VK_UP -> Block.Direction.UP;
                        case KeyEvent.VK_DOWN -> Block.Direction.DOWN;
                        case KeyEvent.VK_LEFT -> Block.Direction.LEFT;
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

        private Point pointToCell(Point p) {
            int rows = board.getRows(), cols = board.getCols();
            int cellSize = Math.min(getWidth() / cols, getHeight() / rows);
            int boardW = cellSize * cols, boardH = cellSize * rows;
            int xOffset = (getWidth() - boardW) / 2, yOffset = (getHeight() - boardH) / 2;
            int c = (p.x - xOffset) / cellSize;
            int r = (p.y - yOffset) / cellSize;
            return new Point(r, c);
        }

        private void startAnimation(Block b, Block.Direction dir) {
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
                    if (board.isVictory()) basic.showPanel("victory");
                }
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            // 先让基类跳过 animBlock 并绘制其他内容
            skipBlock = animBlock;
            super.paintComponent(g);

            // 然后单独绘制动画方块
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

                // 填充
                g2.setColor(typeColor.getOrDefault(animBlock.getType(), new Color(0xCCCCCC)));
                g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
                // 描边
                g2.setColor(new Color(0x888888));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);

                // 文本
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

            // 重置 skipBlock
            skipBlock = null;
        }
    }
}