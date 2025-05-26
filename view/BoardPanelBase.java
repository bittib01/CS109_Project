package view;

import model.Board;
import model.Block;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用的棋盘绘制面板，负责绘制方块与胜利区，支持等比缩放与扁平化配色。
 * 子类可通过设置 skipBlock 来跳过绘制某个方块（比如动画中的方块）。
 */
public class BoardPanelBase extends JPanel {
    protected Board board;
    protected Block skipBlock = null;
    private String displayText;
    private boolean showText = false;

    protected final Map<Block.Type, Color> typeColor = new HashMap<>() {{
        put(Block.Type.SMALL, new Color(0xAEDFF7));
        put(Block.Type.HORIZONTAL, new Color(0xBEE3DB));
        put(Block.Type.VERTICAL, new Color(0xF7D6AE));
        put(Block.Type.LARGE, new Color(0xF2F3B2));
    }};
    // 多名称列表
    protected final List<String> smallNames     = List.of("卒");
    protected final List<String> horizontalNames = List.of("关羽", "黄忠", "马超", "赵云");
    protected final List<String> verticalNames   = List.of("张飞", "许褚", "典韦", "赵云");
    protected final List<String> largeNames      = List.of("曹操");

    public BoardPanelBase(Board board) {
        this.board = board;
        setDoubleBuffered(true);
    }

    public void setText(String text) {
        this.displayText = text;
        this.showText = true;
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (showText) {
            drawCenteredText(g);
            return;
        }
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

        // 计数
        Map<Block.Type, Integer> typeCount = new HashMap<>();

        for (Block b : board.getBlocks()) {
            if (b == skipBlock) continue;
            Point pos = b.getPosition();
            int x = xOffset + pos.y * cellSize;
            int y = yOffset + pos.x * cellSize;
            int w = b.getSize().width * cellSize;
            int h = b.getSize().height * cellSize;

            // 填充
            g2.setColor(typeColor.getOrDefault(b.getType(), new Color(0xCCCCCC)));
            g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
            // 描边:如果这是当前焦点块，就再用红色粗线框起来
            if (b.equals(board.getFocused())) {
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
            } else {
                g2.setColor(new Color(0x888888));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);
            }

            // 文本
            List<String> names;
            switch (b.getType()) {
                case SMALL:      names = smallNames;      break;
                case HORIZONTAL: names = horizontalNames; break;
                case VERTICAL:   names = verticalNames;   break;
                case LARGE:      names = largeNames;      break;
                default:         names = List.of("");
            }
            if (!names.isEmpty()) {
                List<Block> sameTypeBlocks = board.getBlocks().stream()
                        .filter(block -> block.getType() == b.getType())
                        .toList();
                int idx = sameTypeBlocks.indexOf(b);
                String label = names.get(idx % names.size());

                Font font = getFont().deriveFont(Font.BOLD, cellSize * 0.4f);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(label), th = fm.getAscent();
                g2.setColor(new Color(0x333333));
                g2.drawString(label, x + (w - tw) / 2, y + (h + th) / 2 - 4);
            }
        }
    }

    private void drawCenteredText(Graphics g) {
        if (displayText == null || displayText.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int fontSize = Math.min(getWidth(), getHeight()) / 8;
        Font font = new Font("微软雅黑", Font.BOLD, fontSize);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics(font);
        int textWidth = fm.stringWidth(displayText);
        int textHeight = fm.getAscent() - fm.getDescent();
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() + textHeight) / 2;
        g2.setColor(new Color(0x333333));
        g2.drawString(displayText, x, y);
    }

    public void setShowText(boolean showText) {
        this.showText = showText;
    }
}
