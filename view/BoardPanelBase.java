package view;

import model.Board;
import model.Block;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用的棋盘绘制面板，负责绘制方块与胜利区，支持等比缩放与扁平化配色。
 * 子类可通过设置 skipBlock 来跳过绘制某个方块（比如动画中的方块）。
 */
public class BoardPanelBase extends JPanel {
    protected Board board;
    /** 要跳过绘制的方块（可由子类在 paintComponent 里设置） */
    protected Block skipBlock = null;

    // 配色和标签映射
    protected final Map<Block.Type, Color> typeColor = new HashMap<>() {{
        put(Block.Type.SMALL, new Color(0xAEDFF7));
        put(Block.Type.VERTICAL, new Color(0xBEE3DB));
        put(Block.Type.HORIZONTAL, new Color(0xF7D6AE));
        put(Block.Type.LARGE, new Color(0xF2F3B2));
    }};
    protected final Map<Block.Type, String> typeLabel = new HashMap<>() {{
        put(Block.Type.SMALL, "卒");
        put(Block.Type.VERTICAL, "张飞");
        put(Block.Type.HORIZONTAL, "关羽");
        put(Block.Type.LARGE, "曹操");
    }};

    public BoardPanelBase(Board board) {
        this.board = board;
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (board == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rows = board.getRows(), cols = board.getCols();
        int cellSize = Math.min(getWidth() / cols, getHeight() / rows);
        int boardW = cellSize * cols, boardH = cellSize * rows;
        int xOffset = (getWidth() - boardW) / 2, yOffset = (getHeight() - boardH) / 2;

        // 胜利区背景
        g2.setColor(new Color(0xE6F4EA));
        for (Point p : board.getVictoryCells()) {
            int x = xOffset + p.y * cellSize;
            int y = yOffset + p.x * cellSize;
            g2.fillRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 12, 12);
        }

        // 边框
        g2.setColor(new Color(0xDDDDDD));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(xOffset, yOffset, boardW, boardH, 16, 16);

        // 方块
        for (Block b : board.getBlocks()) {
            if (b == skipBlock) continue;  // 跳过动画中的方块
            Point pos = b.getPosition();
            int x = xOffset + pos.y * cellSize;
            int y = yOffset + pos.x * cellSize;
            int w = b.getSize().width * cellSize;
            int h = b.getSize().height * cellSize;

            // 填充
            g2.setColor(typeColor.getOrDefault(b.getType(), new Color(0xCCCCCC)));
            g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
            // 描边
            g2.setColor(new Color(0x888888));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);

            // 文本
            String txt = typeLabel.getOrDefault(b.getType(), "");
            if (!txt.isEmpty()) {
                Font font = getFont().deriveFont(Font.BOLD, cellSize * 0.4f);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(txt), th = fm.getAscent();
                g2.setColor(new Color(0x333333));
                g2.drawString(txt, x + (w - tw) / 2, y + (h + th) / 2 - 4);
            }
        }
    }
}