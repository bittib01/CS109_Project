package view;

import model.Map;

import javax.swing.*;
import java.awt.*;

/**
 * 关卡选择页面。
 */
public class Select extends JPanel {

    /**
     * 构造方法：显示可用地图列表。
     * @param basic 基础窗口引用，用于页面切换
     */
    public Select(Basic basic) {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("请选择地图", JLabel.CENTER);
        add(title, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        // TODO: 动态加载地图文件
        listModel.addElement("Map1");
        listModel.addElement("Map2");
        JList<String> mapList = new JList<>(listModel);
        add(new JScrollPane(mapList), BorderLayout.CENTER);

        JButton startBtn = new JButton("开始游戏");
        add(startBtn, BorderLayout.SOUTH);

        // 开始按钮事件：载入选中地图并进入游戏页面
        startBtn.addActionListener(e -> {
            String mapName = mapList.getSelectedValue();
            if (mapName != null) {
                Map map = new Map("maps\\" + mapName + ".txt");
                basic.addPanel("game", new Game(basic,map));
                basic.showPanel("game");
            }
        });
    }
}