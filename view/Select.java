package view;

import model.GameMap;
import model.Board;
import model.Block;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;

/**
 * 关卡选择页面。
 */
public class Select extends JPanel {

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> mapList = new JList<>(listModel);
    private final PreviewPanel previewPanel = new PreviewPanel();
    private final InfoPanel infoPanel = new InfoPanel();

    public Select(Basic basic) {
        setLayout(new BorderLayout());

        // 标题
        JLabel title = new JLabel("请选择地图", JLabel.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        // 加载地图文件列表
        File mapDir = new File("maps");
        if (mapDir.exists() && mapDir.isDirectory()) {
            String[] files = mapDir.list((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (files != null) {
                for (String f : files) {
                    listModel.addElement(f.substring(0, f.lastIndexOf('.')));
                }
            }
        }
        mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 列表与预览面板分割
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(mapList), previewPanel);
        splitPane.setResizeWeight(0.4);
        splitPane.setDividerSize(4);
        add(splitPane, BorderLayout.CENTER);

        // 右侧信息区
        JPanel right = new JPanel(new BorderLayout());
        right.add(previewPanel, BorderLayout.CENTER);
        right.add(infoPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(right);

        // 监听选中
        mapList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String name = mapList.getSelectedValue();
                    if (name != null) {
                        GameMap map = new GameMap("maps" + File.separator + name + ".txt");
                        previewPanel.setMap(map);
                        infoPanel.updateInfo(map);
                    }
                }
            }
        });

        // 开始按钮
        JButton startBtn = new JButton("开始游戏");
        add(startBtn, BorderLayout.SOUTH);
        startBtn.addActionListener(e -> {
            String name = mapList.getSelectedValue();
            if (name == null) {
                JOptionPane.showMessageDialog(Select.this,
                        "请选择一个地图！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            GameMap map = new GameMap("maps" + File.separator + name + ".txt");
            basic.addPanel("game", new Game(basic, map));
            basic.showPanel("game");
        });
    }

    /**
     * 只负责任何静态预览的面板
     */
    private static class PreviewPanel extends BoardPanelBase {
        public PreviewPanel() {
            super(null);
        }

        public void setMap(GameMap map) {
            this.board = new Board(map);
            repaint();
        }
    }

    /**
     * 地图基本信息面板
     */
    private static class InfoPanel extends JPanel {
        private final JLabel sizeLabel = new JLabel();
        private final JLabel blockLabel = new JLabel();
        private final JLabel victoryLabel = new JLabel();

        public InfoPanel() {
            setLayout(new GridLayout(3, 1));
            add(sizeLabel);
            add(blockLabel);
            add(victoryLabel);
        }

        public void updateInfo(GameMap map) {
            sizeLabel.setText("尺寸：" + map.getRows() + " × " + map.getCols());
            blockLabel.setText("方块数：" + map.getBlocks().size());
            victoryLabel.setText("胜利区数：" + map.getVictoryCells().size());
        }
    }
}