package view;

import model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * 主游戏窗口，支持鼠标拖拽、键盘控制和动画
 */
public class Game extends JFrame {
    private final Board board;
    private final int cellSize = 100;  // 单元格像素大小
    private BoardPanel panel;

    public Game(MapModel map) {
        this.board = new Board(map);
        initUI();
    }

    // 初始化 UI
    private void initUI() {
        setTitle("华容道");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        panel = new BoardPanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // 键盘监听
        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Block focused = board.getFocused();
                Block.Direction dir = null;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:    dir = Block.Direction.UP; break;
                    case KeyEvent.VK_DOWN:  dir = Block.Direction.DOWN; break;
                    case KeyEvent.VK_LEFT:  dir = Block.Direction.LEFT; break;
                    case KeyEvent.VK_RIGHT: dir = Block.Direction.RIGHT; break;
                }
                if (dir == null) return;
                if (e.isControlDown()) {
                    board.moveFocus(dir);
                } else if (focused != null) {
                    animateMove(focused, dir);
                }
                panel.repaint();
            }
        });
    }

    // 平滑动画移动
    private void animateMove(Block b, Block.Direction dir) {
        if (!board.moveBlock(b, dir)) return;
        // 可拓展：使用 Swing Timer 根据 b.getInertia() 控制动画帧间隔
    }

    // 负责绘制和鼠标交互
    private class BoardPanel extends JPanel {
        private Block dragging;
        private Point anchor;

        public BoardPanel() {
            int w = util.Config.cols * cellSize;
            int h = util.Config.rows * cellSize;
            setPreferredSize(new Dimension(w, h));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int r = e.getY() / cellSize;
                    int c = e.getX() / cellSize;
                    Block b = board.getBlockAt(new Point(r, c));
                    board.setFocused(b);
                    dragging = b;
                    anchor = e.getPoint();
                    repaint();
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragging != null) {
                        int dr = e.getY() / cellSize - dragging.getPosition().x;
                        int dc = e.getX() / cellSize - dragging.getPosition().y;
                        Block.Direction dir = null;
                        if (Math.abs(dr) > Math.abs(dc)) {
                            dir = dr > 0 ? Block.Direction.DOWN : Block.Direction.UP;
                        } else {
                            dir = dc > 0 ? Block.Direction.RIGHT : Block.Direction.LEFT;
                        }
                        animateMove(dragging, dir);
                        dragging = null;
                        repaint();
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    // 可视上拖动效果（可选）
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            List<Block> list = board.getBlocks();
            for (Block b : list) {
                Point pos = b.getPosition();
                int x = pos.y * cellSize, y = pos.x * cellSize;
                int w = b.getSize().width * cellSize, h = b.getSize().height * cellSize;
                if (b == board.getFocused()) g.setColor(Color.ORANGE);
                else g.setColor(Color.GRAY);
                g.fillRoundRect(x+5, y+5, w-10, h-10, 20, 20);
            }
        }
    }

    // 程序入口
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MapModel map = new MapModel("map.txt");
            new Game(map);
        });
    }
}
