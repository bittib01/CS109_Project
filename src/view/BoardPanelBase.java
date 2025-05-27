package view;

import model.Board;
import model.Block;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用的棋盘绘制面板，负责渲染方块与胜利区，支持等比缩放、扁平化配色及文本覆盖功能。
 * 子类可通过设置 skipBlock 跳过特定方块的绘制（如动画效果中移动的方块）。
 */
public class BoardPanelBase extends JPanel {
    /** 当前棋盘模型 */
    protected Board board;
    /** 要跳过绘制的方块，用于动画 */
    protected Block skipBlock = null;

    /** 覆盖在面板中心显示的文字内容 */
    private String displayText;
    /** 控制是否在面板上显示 displayText */
    private boolean showText = false;

    /**
     * 不同类型方块对应的填充颜色映射
     */
    protected final Map<Block.Type, Color> typeColor = new HashMap<>() {{
        put(Block.Type.SMALL, new Color(0xAEDFF7));      // 小方块
        put(Block.Type.HORIZONTAL, new Color(0xBEE3DB)); // 横向长方块
        put(Block.Type.VERTICAL, new Color(0xF7D6AE));   // 纵向长方块
        put(Block.Type.LARGE, new Color(0xF2F3B2));      // 大方块
    }};

    /** 名称列表：小方块的标签 */
    protected final List<String> smallNames       = List.of("卒");
    /** 横向方块的标签列表 */
    protected final List<String> horizontalNames  = List.of("关羽", "赵云", "黄忠", "典韦", "周瑜", "甘宁", "魏延", "吕蒙");
    /** 纵向方块的标签列表 */
    protected final List<String> verticalNames    = List.of("张飞", "马超", "许褚", "张辽", "孙策", "陆逊", "徐晃", "张郃");
    /** 大方块的标签列表 */
    protected final List<String> largeNames       = List.of("曹操");

    /**
     * 构造方法，初始化棋盘模型并启用双缓冲以减少闪烁。
     * @param board 棋盘模型
     */
    public BoardPanelBase(Board board) {
        this.board = board;
        // 启用双缓冲加速绘制
        setDoubleBuffered(true);
    }

    /**
     * 设置面板中央显示的文字，并开启文字展示模式。
     * @param text 要显示的文本内容
     */
    public void setText(String text) {
        this.displayText = text;
        this.showText = true;
        // 当展示文字时，使背景可见
        setOpaque(true);
    }

    /**
     * 绘制面板内容：
     * <ul>
     *     <li>如 showText 为 true，调用 drawCenteredText 渲染文字并返回。</li>
     *     <li>否则依次绘制胜利区、棋盘边框及各个方块。</li>
     * </ul>
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 文本模式优先
        if (showText) {
            drawCenteredText(g);
            return;
        }
        if (board == null) return;

        Graphics2D g2 = (Graphics2D) g;
        // 开启抗锯齿优化
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 计算格子尺寸与偏移，实现等比缩放和居中
        int rows = board.getRows(), cols = board.getCols();
        int cellSize = Math.min(getWidth() / cols, getHeight() / rows);
        int boardW = cellSize * cols, boardH = cellSize * rows;
        int xOffset = (getWidth() - boardW) / 2, yOffset = (getHeight() - boardH) / 2;

        // 绘制胜利区背景
        g2.setColor(new Color(0xE6F4EA));
        for (Point p : board.getVictoryCells()) {
            int x = xOffset + p.y * cellSize;
            int y = yOffset + p.x * cellSize;
            g2.fillRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 12, 12);
        }

        // 绘制棋盘外框
        g2.setColor(new Color(0xDDDDDD));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(xOffset, yOffset, boardW, boardH, 16, 16);

        // 遍历所有方块并绘制
        for (Block b : board.getBlocks()) {
            // 跳过指定方块
            if (b == skipBlock) continue;

            Point pos = b.getPosition();
            int x = xOffset + pos.y * cellSize;
            int y = yOffset + pos.x * cellSize;
            int w = b.getSize().width * cellSize;
            int h = b.getSize().height * cellSize;

            // 方块填充
            g2.setColor(typeColor.getOrDefault(b.getType(), new Color(0xCCCCCC)));
            g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);

            // 边框描边
            if (b.equals(board.getFocused())) {
                // 焦点方块使用红色粗边
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
            } else {
                // 普通方块灰色细边
                g2.setColor(new Color(0x888888));
                g2.setStroke(new BasicStroke(1.5f));
            }
            g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 16, 16);

            // 在方块中心绘制对应的文本标签
            List<String> names = switch (b.getType()) {
                case SMALL -> smallNames;
                case HORIZONTAL -> horizontalNames;
                case VERTICAL -> verticalNames;
                case LARGE -> largeNames;
            };
            if (!names.isEmpty()) {
                paintTextInBlock(b, names, cellSize, g2, x, w, y, h);
            }
        }
    }

    void paintTextInBlock(Block b, List<String> names, int cellSize, Graphics2D g2, int x, int w, int y, int h) {
        // 确定当前方块在同类型列表中的序号
        List<Block> sameTypeBlocks = board.getBlocks().stream()
                .filter(block -> block.getType() == b.getType())
                .toList();
        int idx = sameTypeBlocks.indexOf(b);
        String label = names.get(idx % names.size());

        // 设置字体并测量尺寸
        Font font = getFont().deriveFont(Font.BOLD, cellSize * 0.4f);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label), th = fm.getAscent();

        // 绘制文本
        g2.setColor(new Color(0x333333));
        g2.drawString(label, x + (w - tw) / 2, y + (h + th) / 2 - 4);
    }

    /**
     * 在面板中央居中绘制 displayText 文本。
     * @param g Graphics 绘图上下文
     */
    private void drawCenteredText(Graphics g) {
        if (displayText == null || displayText.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 根据面板大小动态计算字体大小
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

    /**
     * 切换是否显示中央文字模式。
     * @param showText true 显示文字，false 重新绘制棋盘
     */
    public void setShowText(boolean showText) {
        this.showText = showText;
    }
}
