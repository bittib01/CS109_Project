package view;
import model.GameMap;
import model.Board;
import util.Saver;
import util.UserController;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * 关卡选择页面。
 */
public class Select extends JPanel {
    private final UserController userController = UserController.getInstance();
    private final DefaultListModel < String > listModel = new DefaultListModel < > ();
    private final JList < String > mapList = new JList < > (listModel);
    private final PreviewPanel previewPanel = new PreviewPanel();
    private final InfoPanel infoPanel = new InfoPanel();
    private final StatsPanel statsPanel = new StatsPanel();
    private final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JComboBox < Game.Mode > modeCombo = new JComboBox < > (Game.Mode.values());
    private final JButton resetAllBtn = new JButton("重置存档");
    private boolean ifContinue = false;
    private final Basic basic;
    private JPanel topBar;
    private JButton loadBtn; // 继续游戏按钮
    public Select(Basic basic) {
        this.basic = basic;
        setLayout(new BorderLayout());
        buildTopBar();
        add(topBar, BorderLayout.NORTH);
        loadMapList();
        mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(mapList), previewPanel);
        splitPane.setResizeWeight(0.2);
        splitPane.setDividerSize(4);
        add(splitPane, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout());
        right.add(previewPanel, BorderLayout.CENTER);
        JPanel settings = new JPanel();
        settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
        JPanel infoStats = new JPanel(new GridLayout(1, 2));
        infoStats.add(infoPanel);
        infoStats.add(statsPanel);
        settings.add(infoStats);
        modePanel.add(new JLabel("游戏模式："));
        modeCombo.setSelectedItem(Game.Mode.NORMAL);
        modePanel.add(modeCombo);
        settings.add(modePanel);
        right.add(settings, BorderLayout.SOUTH);
        splitPane.setRightComponent(right);
        statsPanel.setVisible(false);
        modePanel.setVisible(false);
        mapList.addListSelectionListener((ListSelectionEvent e) -> updateSelection());
        JButton startBtn = new JButton("开始游戏");
        add(startBtn, BorderLayout.SOUTH);
        startBtn.addActionListener(e -> startGame());
    }
    private void buildTopBar() {
        topBar = new JPanel(new BorderLayout(10, 5));
        topBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        topBar.setBackground(new Color(0xF5F5F5));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        String user = userController.getCurrentUser().getUsername();
        left.add(new JLabel(user));
        if(!"Guest".equals(user)) {
            JButton logout = new JButton("退出登录");
            logout.addActionListener(e -> {
                userController.switchUser("Guest", "Guest");
                refreshTopBar();
            });
            left.add(logout);
            // 创建继续游戏按钮并设置可见性
            loadBtn = new JButton("继续游戏");
            loadBtn.addActionListener(e -> {
                ifContinue = true;
                // 选中最近的地图
                String recentMap = getRecentMapName();
                if(recentMap != null) {
                mapList.setSelectedValue(recentMap, true);
                }
                startGame();
            });
            left.add(loadBtn);
            updateLoadButtonVisibility(); // 初始设置可见性
            resetAllBtn.setVisible(true);
        }
        else {
            JButton login = new JButton("登录");
            login.addActionListener(e -> StyledDialog.showLoginDialog(this, this::refreshTopBar));
            JButton register = new JButton("注册");
            register.addActionListener(e -> StyledDialog.showRegisterDialog(this, this::refreshTopBar));
            left.add(login);
            left.add(register);
            resetAllBtn.setVisible(false);
        }
        left.add(resetAllBtn);
        resetAllBtn.addActionListener(e -> resetAllSaves());
        topBar.add(left, BorderLayout.WEST);
        JLabel title = new JLabel("请选择地图", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        topBar.add(title, BorderLayout.CENTER);
    }

    /**
     * 更新继续游戏按钮的可见性
     */
    private void updateLoadButtonVisibility() {
        if(loadBtn != null) {
            boolean flag = hasRecentGame();
            loadBtn.setVisible(flag);
        }
    }

    /**
     * 检查是否存在最近的游戏存档
     * @return 是否存在有效最近存档
     */
    private boolean hasRecentGame() {
        String recentMap = getRecentMapName();
        if(recentMap == null || "null".equals(recentMap)) {
            return false;
        }
        // 检查地图文件是否存在
        File mapFile = new File(recentMap);
        return mapFile.exists();
    }

    /**
     * 获取最近游戏的地图名称
     * @return 地图名称或null
     */
    private String getRecentMapName() {
        String username = userController.getCurrentUser().getUsername();
        return Saver.getRecentMapName(username);
    }
    void refreshTopBar() {
        remove(topBar);
        buildTopBar();
        add(topBar, BorderLayout.NORTH);
        revalidate();
        repaint();
    }
    private void resetAllSaves() {
        String u = userController.getCurrentUser().getUsername();
        int choice = JOptionPane.showConfirmDialog(this, "确认要清除所有存档吗？", "重置存档", JOptionPane.YES_NO_OPTION);
        if(choice == JOptionPane.YES_OPTION) {
            try {
                Saver.resetAllSaves(u);
                statsPanel.clear();
                mapList.clearSelection();
                JOptionPane.showMessageDialog(this, "所有存档已重置");
                updateLoadButtonVisibility(); // 重置后更新按钮状态
            }
            catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "重置失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void loadMapList() {
        File dir = new File("maps");
        if(dir.exists() && dir.isDirectory()) {
            String[] files = dir.list((d, n) -> n.toLowerCase().endsWith(".txt"));
            if(files != null) {
                listModel.clear();
                for(String f: files) {
                    listModel.addElement(f.replaceAll("\\.txt$", ""));
                }
            }
        }
    }
    private void updateSelection() {
        String name = mapList.getSelectedValue();
        if(name != null) {
            GameMap map = new GameMap("maps" + File.separator + name + ".txt");
            if(map.isValid()) {
                previewPanel.setMap(map);
                infoPanel.updateInfo(map);
                statsPanel.updateStats(userController.getCurrentUser().getUsername(), map);
                statsPanel.setVisible(true);
                modePanel.setVisible(true);
                return;
            }
            previewPanel.setText("地图文件不合法！");
        }
        else {
            previewPanel.setText("请选择一个地图");
        }
        infoPanel.updateInfo("");
        statsPanel.clear();
        statsPanel.setVisible(false);
        modePanel.setVisible(false);
    }
    private void startGame() {
        String name;
        if(ifContinue) {
            // 处理继续游戏情况
            name = getFileNameWithoutExtension(getRecentMapName());
            if("null".equals(name)) {
                JOptionPane.showMessageDialog(this, "最近游戏存档不存在！", "提示", JOptionPane.WARNING_MESSAGE);
                ifContinue = false;
                return;
            }
            // 检查地图是否存在
            if(!listModel.contains(name)) {
                JOptionPane.showMessageDialog(this, "最近游戏地图已删除！", "提示", JOptionPane.WARNING_MESSAGE);
                ifContinue = false;
                return;
            }
        }
        else {
            // 正常开始游戏
            name = mapList.getSelectedValue();
            if(name == null) {
                JOptionPane.showMessageDialog(this, "请选择一个地图！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        GameMap map = new GameMap("maps" + File.separator + name + ".txt");
        if(!map.isValid()) {
            JOptionPane.showMessageDialog(this, "请选择一个合法地图！", "提示", JOptionPane.WARNING_MESSAGE);
            ifContinue = false;
            return;
        }
        Game.Mode m = (Game.Mode) modeCombo.getSelectedItem();
        basic.addPanel("game", new Game(basic, map, m));
        basic.showPanel("game");
        Game game = (Game) basic.getPanel("game");
        if(ifContinue) {
            try {
                game.load();
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(this, "加载存档失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            finally {
                ifContinue = false; // 重置状态
            }
        }
    }
    /**
     * 从文件路径中提取不带扩展名的文件名
     * @param filePath 文件路径（支持任意系统路径格式，如 "maps/test2.txt" 或 "D:\\data\\file.log"）
     * @return 不带扩展名的文件名；若路径无效返回空字符串
     */
    public static String getFileNameWithoutExtension(String filePath) {
        // 处理空路径或null
        if(filePath == null || filePath.trim().isEmpty()) {
            return "";
        }
        // 获取完整文件名（含扩展名）
        File file = new File(filePath);
        String fullName = file.getName();
        if(fullName.isEmpty()) {
            return ""; // 无效路径（如空字符串或仅目录分隔符）
        }
        // 查找最后一个点的位置（扩展名分隔符）
        int lastDotIndex = fullName.lastIndexOf('.');
        // 两种情况不保留扩展名：存在点且不在开头（避免隐藏文件如".gitignore"被截断）
        if(lastDotIndex > 0) {
            return fullName.substring(0, lastDotIndex);
        }
        else {
            // 无扩展名或点在开头（视为无扩展名）
            return fullName;
        }
    }

    // 预览面板
    private static class PreviewPanel extends BoardPanelBase {
        public PreviewPanel() {
            super(null);
            setShowText(true);
            setText("请选择一个地图");
        }
        public void setMap(GameMap map) {
            setShowText(false);
            this.board = new Board(map);
            repaint();
        }
        public void setText(String t) {
            super.setText(t);
            repaint();
        }
    }
    // 信息面板
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
            sizeLabel.setText("尺寸：" + map.getRows() + "×" + map.getCols());
            blockLabel.setText("方块数：" + map.getBlocks().size());
            victoryLabel.setText("胜利区数：" + map.getVictoryCells().size());
        }
        public void updateInfo(String t) {
            sizeLabel.setText(t);
            blockLabel.setText(t);
            victoryLabel.setText(t);
        }
    }
    // 统计面板
    private static class StatsPanel extends JPanel {
        private final JLabel completedLabel = new JLabel("完成次数：-");
        private final JLabel movesLabel = new JLabel("最少步数：-");
        private final JLabel timeLabel = new JLabel("最快时间：-");
        public StatsPanel() {
            setLayout(new GridLayout(3, 1));
            add(completedLabel);
            add(movesLabel);
            add(timeLabel);
        }
        public void updateStats(String user, GameMap map) {
            Optional < Saver.Stats > opt = Saver.getStats(user, map);
            if(opt.isPresent()) {
                Saver.Stats s = opt.get();
                completedLabel.setText("完成次数：" + s.completedCount());
                if(s.bestMoves() != Integer.MAX_VALUE) movesLabel.setText("最少步数：" + s.bestMoves());
                if(s.bestTime() != Long.MAX_VALUE) timeLabel.setText("最快时间：" + formatTime(s.bestTime()));
            }
            else clear();
        }
        public void clear() {
            completedLabel.setText("完成次数：-");
            movesLabel.setText("最少步数：-");
            timeLabel.setText("最快时间：-");
        }
        private String formatTime(long ms) {
            long s = ms / 1000, m = s / 60;
            s %= 60;
            return String.format("%d分%02d秒", m, s);
        }
    }
    public void updateInfoPanel(GameMap map) {
        infoPanel.updateInfo(map);
    }
    public void updateStats(GameMap map) {
        statsPanel.updateStats(userController.getCurrentUser().getUsername(), map);
    }
}