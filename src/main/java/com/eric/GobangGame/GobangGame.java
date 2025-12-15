package com.eric.GobangGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;
import java.util.ResourceBundle;
import java.util.Locale;

/**
 * 五子棋游戏（双人对战 + AI对战版）- 支持多语言
 * 功能：棋盘绘制、落子、胜负判定、悔棋、重新开始、多语言支持、AI对战
 */
public class GobangGame extends JFrame{
    // 棋盘相关配置
    private static final int ROW = 15; // 棋盘行数（15路棋盘）
    private static final int COL = 15; // 棋盘列数
    private static final int CELL_SIZE = 40; // 每个格子大小（像素）
    private static final int MARGIN = 30; // 棋盘边距
    private static final int BUTTON_WIDTH = 120; // 按钮宽度
    private static final int BUTTON_HEIGHT = 40; // 按钮高度

    // 棋子状态：0=空，1=黑棋，2=白棋
    private int[][] board = new int[ROW][COL];
    private boolean isBlackTurn = true; // 是否黑棋回合（黑棋先行）
    private boolean gameOver = false; // 游戏是否结束
    private List<int[]> moveHistory = new ArrayList<>(); // 落子历史（用于悔棋）

    // 游戏模式相关
    private int gameMode = 0; // 0=双人对战，1=人机对战
    private int aiDifficulty = 1; // AI难度：0=简单，1=中等，2=困难
    private boolean playerIsBlack = true; // 玩家是否执黑
    private GobangGameAi ai; // AI实例

    // 多语言支持
    private ResourceBundle messages;
    private Locale currentLocale;
    private JComboBox<String> languageComboBox;

    // UI组件引用
    private JButton undoBtn, restartBtn, closeBtn;
    private JMenuItem openItem, saveItem, closeItem, undoItem, restartItem, settingItem, aboutItem;
    private JMenu fileMenu, gameMenu, toolsMenu, aboutMenu;
    private JPanel sidePanel;
    private JLabel thinkingLabel;

    // 构造方法：初始化游戏窗口（棋盘+右侧按钮）
    public GobangGame() {
        // 初始化AI
        this.ai = new GobangGameAi(aiDifficulty);
        
        // 初始化语言（默认简体中文）
        setLanguage(Locale.of("zh", "CN"));
        
        setTitle(messages.getString("game.title"));
        // 窗口大小 = 棋盘宽度 + 按钮宽度 + 边距，高度与棋盘一致
        int windowWidth = MARGIN * 2 + COL * CELL_SIZE + BUTTON_WIDTH + 40;
        int windowHeight = MARGIN * 2 + ROW * CELL_SIZE;
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null); // 窗口居中
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false); // 禁止窗口缩放

        // 1. 创建主容器（采用边界布局，左右分区）
        Container container = getContentPane();
        container.setLayout(new BorderLayout(20, 0)); // 左右组件间距20像素

        // 2. 创建棋盘面板（核心绘图与交互区域）
        ChessboardPanel chessboard = new ChessboardPanel();
        container.add(chessboard, BorderLayout.CENTER); // 棋盘放中间

        // 3. 创建右侧按钮面板（垂直布局）
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 50)); // 垂直间距50像素
        buttonPanel.setPreferredSize(new Dimension(BUTTON_WIDTH, windowHeight)); // 固定按钮面板宽度

        // 4. 创建悔棋按钮
        this.undoBtn = new JButton(messages.getString("button.undo"));
        undoBtn.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        undoBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        
        this.closeBtn = new JButton(messages.getString("button.exit"));
        closeBtn.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        closeBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        

        // 悔棋按钮点击事件
        undoBtn.addActionListener(e -> {
            if (gameOver || moveHistory.isEmpty()) {
                JOptionPane.showMessageDialog(GobangGame.this, 
                    gameOver ? messages.getString("message.cannot_undo_game_over") : messages.getString("message.cannot_undo_no_moves"));
                return;
            }
            // 撤销最后一步落子
            int[] lastMove = moveHistory.remove(moveHistory.size() - 1);
            int x = lastMove[0];
            int y = lastMove[1];
            board[x][y] = 0; // 清空棋子
            isBlackTurn = !isBlackTurn; // 切换回上一回合
            chessboard.repaint(); // 重绘棋盘
        });

        // 5. 创建重新开始按钮（保留原功能，与悔棋按钮并列）
        this.restartBtn = new JButton(messages.getString("button.restart"));
        restartBtn.setPreferredSize(new Dimension(120, 40));
        restartBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        restartBtn.addActionListener(e -> {
            startNewGame();
        });
        
        closeBtn.addActionListener(e -> {
            System.exit(0);
        });

        // 6. 将按钮添加到右侧面板
        buttonPanel.add(undoBtn);
        buttonPanel.add(restartBtn);
        buttonPanel.add(closeBtn);
        container.add(buttonPanel, BorderLayout.EAST); // 按钮面板放右侧

        // 添加菜单（保留原菜单功能，兼容两种操作方式）
        JMenuBar menuBar = new JMenuBar();
        this.fileMenu = new JMenu(messages.getString("menu.file"));
        this.gameMenu = new JMenu(messages.getString("menu.game"));
        this.toolsMenu = new JMenu(messages.getString("menu.tools"));
        this.aboutMenu = new JMenu(messages.getString("menu.about"));
        
        this.undoItem = new JMenuItem(messages.getString("menu.undo"));
        this.restartItem = new JMenuItem(messages.getString("menu.restart"));
        this.closeItem = new JMenuItem(messages.getString("menu.exit"));
        this.settingItem = new JMenuItem(messages.getString("menu.settings"));
        this.saveItem = new JMenuItem(messages.getString("menu.save"));
        this.openItem = new JMenuItem(messages.getString("menu.open"));
        this.aboutItem = new JMenuItem(messages.getString("menu.about_software"));
        
        // 新增菜单项
        JMenuItem newGameItem = new JMenuItem(messages.getString("menu.new_game"));
        JMenuItem aiSettingsItem = new JMenuItem(messages.getString("menu.ai_settings"));
        
        undoItem.addActionListener(e -> undoBtn.doClick()); // 菜单悔棋与按钮联动
        restartItem.addActionListener(e -> restartBtn.doClick()); // 菜单重启与按钮联动
        closeItem.addActionListener(e -> closeBtn.doClick());
        
        newGameItem.addActionListener(e -> showGameModeDialog());
        aiSettingsItem.addActionListener(e -> {
            if (gameMode == 1) {
                showAISettingsDialog(this);
            } else {
                JOptionPane.showMessageDialog(GobangGame.this, 
                    messages.getString("message.ai_mode_only"),
                    messages.getString("message.title.info"),
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        saveItem.addActionListener(e -> {
            if (moveHistory.isEmpty()) {
                JOptionPane.showMessageDialog(GobangGame.this, 
                    messages.getString("message.no_game_to_save"), 
                    messages.getString("message.title.tip"), 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(messages.getString("dialog.save_game"));
            fileChooser.setSelectedFile(new File("gobang_save.dat"));
            
            int userSelection = fileChooser.showSaveDialog(GobangGame.this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().toLowerCase().endsWith(".dat")) {
                    fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".dat");
                }
                saveGame(fileToSave);
            }
        });

        openItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(messages.getString("dialog.open_game"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                messages.getString("filter.game_files"), "dat"));
            
            int userSelection = fileChooser.showOpenDialog(GobangGame.this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToOpen = fileChooser.getSelectedFile();
                if (fileToOpen.exists() && fileToOpen.canRead()) {
                    // Confirm loading if current game has progress
                    if (!moveHistory.isEmpty()) {
                        int result = JOptionPane.showConfirmDialog(
                            GobangGame.this, 
                            messages.getString("message.confirm_load"), 
                            messages.getString("message.title.confirm"), 
                            JOptionPane.YES_NO_OPTION
                        );
                        if (result != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                    loadGame(fileToOpen);
                } else {
                    JOptionPane.showMessageDialog(GobangGame.this, 
                        messages.getString("message.cannot_read_file"), 
                        messages.getString("message.title.error"), 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        settingItem.addActionListener(e -> {
            showSettingsDialog();
        });
        
        aboutItem.addActionListener(e -> {
            showAboutDialog();
        });
        
        toolsMenu.add(settingItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(closeItem);
        gameMenu.add(newGameItem);
        gameMenu.add(aiSettingsItem);
        gameMenu.addSeparator();
        gameMenu.add(undoItem);
        gameMenu.add(restartItem);
        aboutMenu.add(aboutItem);
        menuBar.add(fileMenu);
        menuBar.add(gameMenu);
        menuBar.add(toolsMenu);
        menuBar.add(aboutMenu);
        setJMenuBar(menuBar);
    }

    /**
     * 显示游戏模式选择对话框
     */
    private void showGameModeDialog() {
        JFrame modeWindow = new JFrame(messages.getString("dialog.game_mode"));
        modeWindow.setSize(400, 300);
        modeWindow.setLocationRelativeTo(null);
        modeWindow.setResizable(false);
        
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel(messages.getString("label.select_mode"), JLabel.CENTER);
        titleLabel.setFont(new Font("宋体", Font.BOLD, 16));
        panel.add(titleLabel);
        
        // 双人对战按钮
        JButton twoPlayerBtn = new JButton(messages.getString("mode.two_player"));
        twoPlayerBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        twoPlayerBtn.addActionListener(e -> {
            gameMode = 0;
            startNewGame();
            modeWindow.dispose();
        });
        panel.add(twoPlayerBtn);
        
        // 人机对战按钮
        JButton aiModeBtn = new JButton(messages.getString("mode.ai"));
        aiModeBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        aiModeBtn.addActionListener(e -> {
            gameMode = 1;
            showAISettingsDialog(modeWindow);
        });
        panel.add(aiModeBtn);
        
        modeWindow.add(panel);
        modeWindow.setVisible(true);
    }

    /**
     * 显示AI设置对话框
     */
    private void showAISettingsDialog(JFrame parentWindow) {
        JFrame settingsWindow = new JFrame(messages.getString("dialog.ai_settings"));
        settingsWindow.setSize(400, 350);
        settingsWindow.setLocationRelativeTo(null);
        settingsWindow.setResizable(false);
        
        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 难度选择
        JLabel difficultyLabel = new JLabel(messages.getString("label.select_difficulty"), JLabel.CENTER);
        difficultyLabel.setFont(new Font("宋体", Font.BOLD, 14));
        panel.add(difficultyLabel);
        
        String[] difficultyOptions = {
            messages.getString("difficulty.easy"),
            messages.getString("difficulty.medium"),
            messages.getString("difficulty.hard")
        };
        
        JComboBox<String> difficultyCombo = new JComboBox<>(difficultyOptions);
        difficultyCombo.setSelectedIndex(aiDifficulty);
        panel.add(difficultyCombo);
        
        // 棋子颜色选择
        JLabel colorLabel = new JLabel(messages.getString("label.select_color"), JLabel.CENTER);
        colorLabel.setFont(new Font("宋体", Font.BOLD, 14));
        panel.add(colorLabel);
        
        JPanel colorPanel = new JPanel(new FlowLayout());
        JRadioButton blackRadio = new JRadioButton(messages.getString("color.black"));
        JRadioButton whiteRadio = new JRadioButton(messages.getString("color.white"));
        ButtonGroup colorGroup = new ButtonGroup();
        colorGroup.add(blackRadio);
        colorGroup.add(whiteRadio);
        blackRadio.setSelected(playerIsBlack);
        whiteRadio.setSelected(!playerIsBlack);
        colorPanel.add(blackRadio);
        colorPanel.add(whiteRadio);
        panel.add(colorPanel);
        
        // 开始游戏按钮
        JButton startBtn = new JButton(messages.getString("button.start_game"));
        startBtn.setFont(new Font("宋体", Font.BOLD, 14));
        startBtn.addActionListener(e -> {
            aiDifficulty = difficultyCombo.getSelectedIndex();
            playerIsBlack = blackRadio.isSelected();
            ai.setDifficulty(aiDifficulty);
            startNewGame();
            settingsWindow.dispose();
            if (parentWindow != null) {
                parentWindow.dispose();
            }
        });
        panel.add(startBtn);
        
        settingsWindow.add(panel);
        settingsWindow.setVisible(true);
    }

    private void setLanguage(Locale locale) {
        this.currentLocale = locale;
        try {
            System.out.println("Attempting to load resource bundle for: " + locale);
            this.messages = ResourceBundle.getBundle("messages", locale);
            System.out.println("Successfully loaded resource bundle for: " + locale);
            System.out.println("Current locale after loading: " + messages.getLocale());
            
        } catch (Exception e) {
            System.err.println("Error loading resource bundle for locale: " + locale);
            e.printStackTrace();
            try {
                System.out.println("Falling back to English...");
                this.messages = ResourceBundle.getBundle("messages", Locale.ENGLISH);
                this.currentLocale = Locale.ENGLISH;
                System.out.println("Fell back to: " + this.messages.getLocale());
            } catch (Exception ex) {
                System.err.println("Could not load any resource bundle!");
                ex.printStackTrace();
            }
        }
    }

    /**
     * 更新界面语言 - 直接更新所有存储的组件引用
     */
    private void updateUILanguage() {
        setTitle(messages.getString("game.title"));
        
        // 更新按钮
        if (undoBtn != null) undoBtn.setText(messages.getString("button.undo"));
        if (restartBtn != null) restartBtn.setText(messages.getString("button.restart"));
        if (closeBtn != null) closeBtn.setText(messages.getString("button.exit"));
        
        // 更新菜单
        if (fileMenu != null) fileMenu.setText(messages.getString("menu.file"));
        if (gameMenu != null) gameMenu.setText(messages.getString("menu.game"));
        if (toolsMenu != null) toolsMenu.setText(messages.getString("menu.tools"));
        if (aboutMenu != null) aboutMenu.setText(messages.getString("menu.about"));
        
        // 更新菜单项
        if (openItem != null) openItem.setText(messages.getString("menu.open"));
        if (saveItem != null) saveItem.setText(messages.getString("menu.save"));
        if (closeItem != null) closeItem.setText(messages.getString("menu.exit"));
        if (undoItem != null) undoItem.setText(messages.getString("menu.undo"));
        if (restartItem != null) restartItem.setText(messages.getString("menu.restart"));
        if (settingItem != null) settingItem.setText(messages.getString("menu.settings"));
        if (aboutItem != null) aboutItem.setText(messages.getString("menu.about_software"));
        
        // 刷新界面
        revalidate();
        repaint();
    }

    /**
     * 显示设置对话框
     */
    private void showSettingsDialog() {
        JFrame settingsWindow = new JFrame(messages.getString("dialog.settings"));
        Font font = new Font("Microsoft YaHei", Font.PLAIN, 14);
        settingsWindow.setSize(400, 200);
        settingsWindow.setLocationRelativeTo(null);
        settingsWindow.setResizable(false);
        
        JPanel settingPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        settingPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel languageSetting = new JLabel(messages.getString("settings.language"));
        languageSetting.setFont(font);
        settingPanel.add(languageSetting);
        
        String[] languageOptions = {
            messages.getString("language.chinese_simple"),
            messages.getString("language.chinese_traditional"), 
            messages.getString("language.english"),
            messages.getString("language.german"),
            messages.getString("language.korean_north_korea"),
            messages.getString("language.korean_south_korea")
        };
        
        languageComboBox = new JComboBox<>(languageOptions);
        languageComboBox.setFont(font);
        
        // 设置当前选中的语言
        if (currentLocale.getLanguage().equals("zh")) {
            if (currentLocale.getCountry().equals("CN")) {
                languageComboBox.setSelectedIndex(0);
            } else {
                languageComboBox.setSelectedIndex(1);
            }
        } else {
            languageComboBox.setSelectedIndex(2);
        }
        
        languageComboBox.addActionListener(f -> {
            int selectedIndex = languageComboBox.getSelectedIndex();
            Locale newLocale;
            switch (selectedIndex) {
                case 0:
                    newLocale = Locale.of("zh", "CN");
                    break;
                case 1:
                    newLocale = Locale.of("zh", "TW");
                    break;
                case 2:
                    newLocale = Locale.of("en", "US");
                    break;
                case 3:
                    newLocale = Locale.of("de", "DE");
                    break;
                case 4:
                    newLocale = Locale.of("ko", "KP");
                    break;
                case 5:
                    newLocale = Locale.of("ko", "KR");
                    break;
                default:
                    newLocale = Locale.of("zh", "CN");
            }
            
            setLanguage(newLocale);
            updateUILanguage();
            
            // Safe message display
            String message;
            try {
                message = messages.getString("message.language_changed");
            } catch (java.util.MissingResourceException e) {
                message = "Language changed successfully!";
            }
            
            JOptionPane.showMessageDialog(settingsWindow, 
                message,
                messages.getString("message.title.info"),
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        settingPanel.add(languageComboBox);
        
        // 占位符
        settingPanel.add(new JLabel());
        settingPanel.add(new JLabel());
        
        settingsWindow.add(settingPanel);
        settingsWindow.setVisible(true);
    }

    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        JFrame aboutWindow = new JFrame(messages.getString("dialog.about"));
        Font font = new Font("宋体", Font.PLAIN, 14);
        aboutWindow.setResizable(false);
        aboutWindow.setLocationRelativeTo(null);
        aboutWindow.setSize(300, 300);
        aboutWindow.setLayout(new BorderLayout());

        JLabel label = new JLabel("<html><div style='text-align: center; font-family: Arial; font-size: 14px; line-height: 1.5;'>"
            + messages.getString("about.version") + "<br>"
            + messages.getString("about.date") + "<br>"
            + messages.getString("about.copyright") + "<br>"
            + messages.getString("about.description")
            + "</div></html>", JLabel.CENTER);
        label.setFont(font);

        JButton licenceBtn = new JButton(messages.getString("button.license"));
        licenceBtn.setPreferredSize(new Dimension(100, 35));
        licenceBtn.addActionListener(f -> {
            showLicenseDialog();
        });
        
        JButton websiteBtn = new JButton(messages.getString("button.website"));
        websiteBtn.setPreferredSize(new Dimension(100, 35));
        websiteBtn.addActionListener(f -> {
            if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://qiuerichanru.work.gd"));
                } catch (IOException | URISyntaxException e1) {
                    JOptionPane.showMessageDialog(null, 
                        messages.getString("message.browser_error"),
                        messages.getString("message.title.error"),
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        licenceBtn.setFont(font);
        websiteBtn.setFont(font);

        JPanel buttonPanelLicence = new JPanel(new FlowLayout());
        buttonPanelLicence.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        buttonPanelLicence.add(licenceBtn);
        buttonPanelLicence.add(websiteBtn);

        aboutWindow.add(label, BorderLayout.CENTER);
        aboutWindow.add(buttonPanelLicence, BorderLayout.SOUTH);
        aboutWindow.setVisible(true);
    }

    /**
     * 显示许可证对话框
     */
    private void showLicenseDialog() {
        JFrame licenceWindow = new JFrame(messages.getString("dialog.license"));
        licenceWindow.setResizable(false);
        licenceWindow.setLocationRelativeTo(null);
        
        JTextArea licenceText = new JTextArea();
        licenceText.setText(messages.getString("license.content"));
        licenceText.setEditable(false);
        licenceText.setLineWrap(true);
        licenceText.setWrapStyleWord(true);
        licenceText.setFont(new Font("宋体", Font.PLAIN, 14));
        licenceText.setMargin(new Insets(15, 15, 15, 15));
        
        licenceWindow.setSize(500, 600);
        licenceWindow.add(new JScrollPane(licenceText));
        licenceWindow.setVisible(true);
    }

    /**
     * 开始新游戏
     */
    private void startNewGame() {
        board = new int[ROW][COL];
        moveHistory.clear();
        isBlackTurn = true;
        gameOver = false;
        
        // 如果是人机对战且玩家执白，则AI先手
        if (gameMode == 1 && !playerIsBlack) {
            // 延迟执行AI落子，确保UI已经更新
            SwingUtilities.invokeLater(() -> {
                aiTurn();
            });
        }
        
        repaint();
    }

    /**
     * AI思考并落子
     */
    private void aiTurn() {
        if (gameOver) return;
        
        // 确定AI的棋子颜色
        int aiPlayer = playerIsBlack ? 2 : 1; // 玩家执黑则AI执白，反之亦然
        
        // 判断是否该AI落子
        if ((isBlackTurn && aiPlayer == 1) || (!isBlackTurn && aiPlayer == 2)) {
            // 显示思考中提示
            /*JOptionPane.showMessageDialog(this, 
                messages.getString("message.ai_thinking"), 
                messages.getString("message.title.info"), 
                JOptionPane.INFORMATION_MESSAGE);*/
            
            // 在新线程中执行AI计算（避免UI冻结）
            new Thread(() -> {
                try {
                    // 根据难度设置不同的思考时间
                    int thinkTime = (aiDifficulty + 1) * 500; // 0.5-1.5秒
                    Thread.sleep(thinkTime); // 模拟思考时间
                    
                    SwingUtilities.invokeLater(() -> {
                        // AI计算最佳落子位置
                        int[] move = ai.calculateMove(board, aiPlayer);
                        
                        if (move[0] != -1 && move[1] != -1 && board[move[0]][move[1]] == 0) {
                            // AI落子
                            board[move[0]][move[1]] = aiPlayer;
                            moveHistory.add(new int[]{move[0], move[1]});
                            
                            // 检查AI是否获胜
                            if (checkWin(move[0], move[1])) {
                                String winner = aiPlayer == 1 ? 
                                    messages.getString("game.black") : messages.getString("game.white");
                                JOptionPane.showMessageDialog(GobangGame.this, 
                                    messages.getString("message.ai_win") + " (" + winner + ")");
                                gameOver = true;
                            } else if (checkDraw()) {
                                JOptionPane.showMessageDialog(GobangGame.this, messages.getString("game.draw"));
                                gameOver = true;
                            } else {
                                // 切换回合
                                isBlackTurn = !isBlackTurn;
                            }
                            
                            repaint();
                        }
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }
    
    /**
     * 检查是否获胜：判断当前落子位置是否形成五子连线
     * 方向：水平、垂直、左上-右下、右上-左下
     */
    private boolean checkWin(int row, int col) {
        int type = board[row][col]; // 当前落子的棋子类型（1=黑，2=白）
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}}; // 四个方向

        for (int[] dir : directions) {
            int count = 1; // 当前落子算1颗
            int dx = dir[0];
            int dy = dir[1];

            // 向一个方向遍历
            for (int i = 1; i < 5; i++) {
                int r = row + i * dx;
                int c = col + i * dy;
                if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == type) {
                    count++;
                } else {
                    break;
                }
            }

            // 向反方向遍历
            for (int i = 1; i < 5; i++) {
                int r = row - i * dx;
                int c = col - i * dy;
                if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == type) {
                    count++;
                } else {
                    break;
                }
            }

            // 五子连线则获胜
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否平局：棋盘无空位且无人获胜
     */
    private boolean checkDraw() {
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] == 0) {
                    return false; // 还有空位，不是平局
                }
            }
        }
        return true;
    }

    /**
     * 棋盘面板：负责绘制棋盘、棋子，处理鼠标落子事件
     */
    private class ChessboardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawChessboard(g); // 绘制棋盘网格
            drawChessPieces(g); // 绘制已落棋子
        }

        // 绘制棋盘（15x15网格+星位）
        private void drawChessboard(Graphics g) {
            g.setColor(Color.BLACK);
            // 绘制横线
            for (int i = 0; i < ROW; i++) {
                int y = MARGIN + i * CELL_SIZE;
                g.drawLine(MARGIN, y, MARGIN + (COL - 1) * CELL_SIZE, y);
            }
            // 绘制竖线
            for (int i = 0; i < COL; i++) {
                int x = MARGIN + i * CELL_SIZE;
                g.drawLine(x, MARGIN, x, MARGIN + (ROW - 1) * CELL_SIZE);
            }
            // 绘制棋盘中心和星位（5个关键点）
            int center = MARGIN + 7 * CELL_SIZE;
            int star = CELL_SIZE * 4;
            drawChessPoint(g, center, center); // 中心
            drawChessPoint(g, center - star, center - star); // 左上星位
            drawChessPoint(g, center - star, center + star); // 左下星位
            drawChessPoint(g, center + star, center - star); // 右上星位
            drawChessPoint(g, center + star, center + star); // 右下星位
        }

        // 绘制棋盘上的黑点（星位和中心）
        private void drawChessPoint(Graphics g, int x, int y) {
            g.fillOval(x - 3, y - 3, 6, 6);
        }

        // 绘制已落的棋子
        private void drawChessPieces(Graphics g) {
            for (int i = 0; i < ROW; i++) {
                for (int j = 0; j < COL; j++) {
                    if (board[i][j] != 0) {
                        int x = MARGIN + j * CELL_SIZE;
                        int y = MARGIN + i * CELL_SIZE;
                        // 黑棋
                        if (board[i][j] == 1) {
                            g.setColor(Color.BLACK);
                            g.fillOval(x - CELL_SIZE / 2 + 2, y - CELL_SIZE / 2 + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                        }
                        // 白棋（黑边+白心）
                        else {
                            g.setColor(Color.BLACK);
                            g.drawOval(x - CELL_SIZE / 2 + 2, y - CELL_SIZE / 2 + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                            g.setColor(Color.WHITE);
                            g.fillOval(x - CELL_SIZE / 2 + 3, y - CELL_SIZE / 2 + 3, CELL_SIZE - 6, CELL_SIZE - 6);
                        }
                    }
                }
            }
        }

        // 初始化鼠标监听器：处理落子逻辑
        public ChessboardPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (gameOver) return; // 游戏结束则不响应

                    // 计算鼠标点击位置对应的棋盘坐标（i=行，j=列）
                    int x = e.getX();
                    int y = e.getY();
                    int j = (x - MARGIN + CELL_SIZE / 2) / CELL_SIZE;
                    int i = (y - MARGIN + CELL_SIZE / 2) / CELL_SIZE;

                    // 校验坐标是否合法（在棋盘内且未落子）
                    if (i >= 0 && i < ROW && j >= 0 && j < COL && board[i][j] == 0) {
                        // 确定当前玩家的棋子类型
                        int currentPlayer = isBlackTurn ? 1 : 2;
                        
                        // 在人机对战模式下，检查是否是玩家回合
                        if (gameMode == 1) {
                            int playerColor = playerIsBlack ? 1 : 2;
                            if (currentPlayer != playerColor) {
                                // 不是玩家的回合，不能落子
                                JOptionPane.showMessageDialog(GobangGame.this, 
                                    messages.getString("message.ai_thinking"),
                                    messages.getString("message.title.info"),
                                    JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                        }
                        
                        // 玩家落子
                        board[i][j] = currentPlayer;
                        moveHistory.add(new int[]{i, j});

                        // 检查游戏是否结束
                        if (GobangGame.this.checkWin(i, j)) {
                            String winner = isBlackTurn ? messages.getString("game.black") : messages.getString("game.white");
                            JOptionPane.showMessageDialog(GobangGame.this, winner + messages.getString("game.win"));
                            gameOver = true;
                        } else if (GobangGame.this.checkDraw()) {
                            JOptionPane.showMessageDialog(GobangGame.this, messages.getString("game.draw"));
                            gameOver = true;
                        } else {
                            // 切换回合
                            isBlackTurn = !isBlackTurn;
                            
                            // 如果是人机对战，且游戏未结束，让AI落子
                            if (gameMode == 1 && !gameOver) {
                                aiTurn();
                            }
                        }
                        
                        repaint();
                    }
                }
            });
        }
    }
    /**
     * Save current game state to a file
     */
    private void saveGame(File file) {
    	try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
    		// Create a save object containing all game state
    		GameSave save = new GameSave(board, isBlackTurn, gameOver, moveHistory);
    		oos.writeObject(save);
    		JOptionPane.showMessageDialog(this, 
    				messages.getString("message.save_success"), 
    				messages.getString("message.title.success"), 
    				JOptionPane.INFORMATION_MESSAGE);
    	} catch (IOException ex) {
    		JOptionPane.showMessageDialog(this, 
    				messages.getString("message.save_failed") + ex.getMessage(), 
    				messages.getString("message.title.error"), 
    				JOptionPane.ERROR_MESSAGE);
    	}
    }

    /**
     * Load game state from a file
     */
    private void loadGame(File file) {
    	try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
    		GameSave save = (GameSave) ois.readObject();
        
    		// Restore game state
    		GobangGame.this.board = save.getBoard();
    		GobangGame.this.isBlackTurn = save.isBlackTurn();
    		GobangGame.this.gameOver = save.isGameOver();
    		GobangGame.this.moveHistory = save.getMoveHistory();
        
    		// 加载存档后，如果游戏未结束且是人机对战模式，可能需要AI行动
    		if (gameMode == 1 && !gameOver) {
    			// 检查是否是AI的回合
    			int aiPlayer = playerIsBlack ? 2 : 1;
    			if ((isBlackTurn && aiPlayer == 1) || (!isBlackTurn && aiPlayer == 2)) {
    				aiTurn();
    			}
    		}
        
    		repaint(); // Refresh the display
    		JOptionPane.showMessageDialog(this, 
    				messages.getString("message.load_success"), 
    				messages.getString("message.title.success"), 
    				JOptionPane.INFORMATION_MESSAGE);
    	} catch (IOException | ClassNotFoundException ex) {
    		JOptionPane.showMessageDialog(this, 
    				messages.getString("message.load_failed") + ex.getMessage(), 
    				messages.getString("message.title.error"), 
    				JOptionPane.ERROR_MESSAGE);
    	}
    }
    
    /**
     * Inner class to represent game save data
     */
    private static class GameSave implements Serializable {
    	private static final long serialVersionUID = 1L;
    
    	private int[][] board;
    	private boolean isBlackTurn;
    	private boolean gameOver;
    	private List<int[]> moveHistory;
    
    	public GameSave(int[][] board, boolean isBlackTurn, boolean gameOver, List<int[]> moveHistory) {
    		this.board = copyBoard(board);
    		this.isBlackTurn = isBlackTurn;
    		this.gameOver = gameOver;
    		this.moveHistory = new ArrayList<>(moveHistory); // Create a copy
    	}
    
    	// Deep copy of the board
    	private int[][] copyBoard(int[][] original) {
    		int[][] copy = new int[original.length][];
    		for (int i = 0; i < original.length; i++) {
    			copy[i] = original[i].clone();
    		}
    		return copy;
    	}
    
    	// Getters
    	public int[][] getBoard() { return copyBoard(board); }
    	public boolean isBlackTurn() { return isBlackTurn; }
    	public boolean isGameOver() { return gameOver; }
    	public List<int[]> getMoveHistory() { return new ArrayList<>(moveHistory); }
    }
    // 主方法：启动游戏
    public static void main(String[] args) {
    	// 使用SwingUtilities.invokeLater确保线程安全
    	SwingUtilities.invokeLater(() -> {
    		try {
    			// 设置LookAndFeel为系统默认
    			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        
    		// 创建游戏实例
    		GobangGame game = new GobangGame();
        
    		// 显示游戏窗口
    		game.setVisible(true);
        
    		// 显示游戏模式选择对话框
    		// 使用SwingUtilities.invokeLater确保对话框在窗口显示后弹出
    		SwingUtilities.invokeLater(() -> {
    			game.showGameModeDialog();
    		});
    	});
    }
}
