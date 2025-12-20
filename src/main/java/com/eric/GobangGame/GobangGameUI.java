package com.eric.GobangGame;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 负责构建和管理所有用户界面元素（菜单、按钮、对话框）
 */
public class GobangGameUI {

    private final GobangGame game;
    private ResourceBundle messages;
    private Locale currentLocale;
    private JComboBox<String> languageComboBox;

    // UI组件引用 (由GobangGame持有)
    private JButton undoBtn, restartBtn, closeBtn, loadBtn, saveBtn;
    private JMenuItem openItem, saveItem, closeItem, undoItem, restartItem, settingItem, aboutItem;
    private JMenu fileMenu, gameMenu, toolsMenu, aboutMenu;
    private JLabel aiState;

    public GobangGameUI(GobangGame game, Locale initialLocale) {
        this.game = game;
        setLanguage(initialLocale);
    }

    public ResourceBundle getMessages() {
        return messages;
    }

    /**
     * 设置语言并加载资源文件
     */
    public void setLanguage(Locale locale) {
        this.currentLocale = locale;
        try {
            this.messages = ResourceBundle.getBundle("messages", locale);
        } catch (Exception e) {
            System.err.println("Error loading resource bundle for locale: " + locale + ". Falling back to English.");
            this.messages = ResourceBundle.getBundle("messages", Locale.ENGLISH);
            this.currentLocale = Locale.ENGLISH;
        }
    }

    /**
     * 更新界面语言
     */
    public void updateUILanguage() {
        // 更新窗口标题
        game.setTitle(messages.getString("game.title"));

        // 更新按钮
        if (undoBtn != null) undoBtn.setText(messages.getString("button.undo"));
        if (restartBtn != null) restartBtn.setText(messages.getString("button.restart"));
        if (closeBtn != null) closeBtn.setText(messages.getString("button.exit"));

        // 更新菜单
        if (fileMenu != null) fileMenu.setText(messages.getString("menu.file"));
        if (gameMenu != null) gameMenu.setText(messages.getString("menu.game"));
        if (toolsMenu != null) toolsMenu.setText(messages.getString("menu.tools"));
        if (aboutMenu != null) aboutMenu.setText(messages.getString("menu.about"));

        // 更新菜单项 (需要重新创建或更新所有引用的JMenuItem)
        // 为了简化，这里只更新已引用的项
        if (openItem != null) openItem.setText(messages.getString("menu.open"));
        if (saveItem != null) saveItem.setText(messages.getString("menu.save"));
        if (closeItem != null) closeItem.setText(messages.getString("menu.exit"));
        if (undoItem != null) undoItem.setText(messages.getString("menu.undo"));
        if (restartItem != null) restartItem.setText(messages.getString("menu.restart"));
        if (settingItem != null) settingItem.setText(messages.getString("menu.settings"));
        if (aboutItem != null) aboutItem.setText(messages.getString("menu.about_software"));

        // 刷新界面
        game.revalidate();
        game.repaint();
    }

    /**
     * 构建右侧按钮面板
     */
    public JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        // 回到原来的FlowLayout，但减少垂直间距
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 30));
        buttonPanel.setPreferredSize(new Dimension(game.getBUTTON_WIDTH(), game.getHeight()));
        try {
            this.saveBtn = new JButton(messages.getString("button.save"));
            this.loadBtn = new JButton(messages.getString("button.load"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Falling back to hard-coded text");
            this.saveBtn = new JButton("Save");
            this.loadBtn = new JButton("Open");
        }
        this.undoBtn = new JButton(messages.getString("button.undo"));
        this.restartBtn = new JButton(messages.getString("button.restart"));
        this.closeBtn = new JButton(messages.getString("button.exit"));
        this.aiState = new JLabel("AI: IDLE", SwingConstants.CENTER);

        Dimension btnSize = new Dimension(game.getBUTTON_WIDTH(), game.getBUTTON_HEIGHT());

        // 设置按钮属性
        saveBtn.setPreferredSize(btnSize);
        loadBtn.setPreferredSize(btnSize);
        undoBtn.setPreferredSize(btnSize);
        restartBtn.setPreferredSize(btnSize);
        closeBtn.setPreferredSize(btnSize);

        Font font = new Font("宋体", Font.PLAIN, 14);
        saveBtn.setFont(font);
        loadBtn.setFont(font);
        undoBtn.setFont(font);
        restartBtn.setFont(font);
        closeBtn.setFont(font);
        aiState.setFont(font);

        // 设置AI标签样式
        aiState.setForeground(Color.BLUE);

        // 添加按钮事件
        saveBtn.addActionListener(e -> saveItem.getActionListeners()[0].actionPerformed(null));
        loadBtn.addActionListener(e -> openItem.getActionListeners()[0].actionPerformed(null));
        undoBtn.addActionListener(e -> game.undoMove());
        restartBtn.addActionListener(e -> game.startNewGame());
        closeBtn.addActionListener(e -> System.exit(0));

        // 直接添加组件到buttonPanel
        buttonPanel.add(saveBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(undoBtn);
        buttonPanel.add(restartBtn);
        buttonPanel.add(closeBtn);

        // 添加一个分隔符
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(game.getBUTTON_WIDTH() - 20, 2));
        buttonPanel.add(separator);

        // 添加AI标签
        buttonPanel.add(aiState);

        return buttonPanel;
    }

    public void updateAiLabel(String state) {
        if (aiState != null) {
            aiState.setText(state);

            // 根据状态改变颜色
            if (state.contains("Thinking") || state.contains("Working")) {
                aiState.setForeground(Color.RED);
            } else if (state.contains("IDLE")) {
                aiState.setForeground(Color.GREEN);
            } else if (state.contains("Error")) {
                aiState.setForeground(Color.ORANGE);
            }

            // 确保标签可见
            aiState.repaint();
        }
    }

    /**
     * 构建菜单栏
     */
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 初始化菜单
        this.fileMenu = new JMenu(messages.getString("menu.file"));
        this.gameMenu = new JMenu(messages.getString("menu.game"));
        this.toolsMenu = new JMenu(messages.getString("menu.tools"));
        this.aboutMenu = new JMenu(messages.getString("menu.about"));

        // 初始化菜单项
        this.openItem = new JMenuItem(messages.getString("menu.open"));
        this.saveItem = new JMenuItem(messages.getString("menu.save"));
        this.closeItem = new JMenuItem(messages.getString("menu.exit"));
        this.undoItem = new JMenuItem(messages.getString("menu.undo"));
        this.restartItem = new JMenuItem(messages.getString("menu.restart"));
        this.settingItem = new JMenuItem(messages.getString("menu.settings"));
        this.aboutItem = new JMenuItem(messages.getString("menu.about_software"));
        JMenuItem newGameItem = new JMenuItem(messages.getString("menu.new_game"));
        JMenuItem aiSettingsItem = new JMenuItem(messages.getString("menu.ai_settings"));

        // 添加事件监听
        undoItem.addActionListener(e -> undoBtn.doClick());
        restartItem.addActionListener(e -> restartBtn.doClick());
        closeItem.addActionListener(e -> closeBtn.doClick());
        newGameItem.addActionListener(e -> showGameModeDialog());
        settingItem.addActionListener(e -> showSettingsDialog());
        aboutItem.addActionListener(e -> showAboutDialog());
        
        aiSettingsItem.addActionListener(e -> {
            if (game.getGameMode() == 1) {
                showAISettingsDialog(game);
            } else {
                JOptionPane.showMessageDialog(game, 
                    messages.getString("message.ai_mode_only"),
                    messages.getString("message.title.info"),
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        saveItem.addActionListener(e -> {
            if (game.getMoveHistory().isEmpty()) {
                JOptionPane.showMessageDialog(game, messages.getString("message.no_game_to_save"));
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(messages.getString("dialog.save_game"));
            fileChooser.setSelectedFile(new File("gobang_save.dat"));
            
            if (fileChooser.showSaveDialog(game) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().toLowerCase().endsWith(".dat")) {
                    fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".dat");
                }
                game.saveGame(fileToSave);
            }
        });

        openItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(messages.getString("dialog.open_game"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                messages.getString("filter.game_files"), "dat"));
            
            if (fileChooser.showOpenDialog(game) == JFileChooser.APPROVE_OPTION) {
                File fileToOpen = fileChooser.getSelectedFile();
                if (fileToOpen.exists() && fileToOpen.canRead()) {
                    if (!game.getMoveHistory().isEmpty()) {
                        int result = JOptionPane.showConfirmDialog(
                            game, messages.getString("message.confirm_load"), 
                            messages.getString("message.title.confirm"), JOptionPane.YES_NO_OPTION);
                        if (result != JOptionPane.YES_OPTION) return;
                    }
                    game.loadGame(fileToOpen);
                } else {
                    JOptionPane.showMessageDialog(game, messages.getString("message.cannot_read_file"));
                }
            }
        });

        // 组合菜单
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
        
        return menuBar;
    }
    
    /**
     * 显示游戏模式选择对话框
     */
    public void showGameModeDialog() {
        JFrame modeWindow = new JFrame(messages.getString("dialog.game_mode"));
        modeWindow.setSize(400, 300);
        modeWindow.setLocationRelativeTo(null);
        modeWindow.setResizable(false);
        
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel(messages.getString("label.select_mode"), JLabel.CENTER);
        titleLabel.setFont(new Font("宋体", Font.BOLD, 16));
        panel.add(titleLabel);
        
        JButton twoPlayerBtn = new JButton(messages.getString("mode.two_player"));
        twoPlayerBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        twoPlayerBtn.addActionListener(e -> {
            game.setGameMode(0);
            game.startNewGame();
            modeWindow.dispose();
        });
        panel.add(twoPlayerBtn);
        
        JButton aiModeBtn = new JButton(messages.getString("mode.ai"));
        aiModeBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        aiModeBtn.addActionListener(e -> {
            game.setGameMode(1);
            showAISettingsDialog(modeWindow);
        });
        panel.add(aiModeBtn);
        
        modeWindow.add(panel);
        modeWindow.setVisible(true);
    }

    /**
     * 显示AI设置对话框
     */
    public void showAISettingsDialog(JFrame parentWindow) {
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
        difficultyCombo.setSelectedIndex(game.getAiDifficulty());
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
        blackRadio.setSelected(game.isPlayerIsBlack());
        whiteRadio.setSelected(!game.isPlayerIsBlack());
        colorPanel.add(blackRadio);
        colorPanel.add(whiteRadio);
        panel.add(colorPanel);
        
        // 开始游戏按钮
        JButton startBtn = new JButton(messages.getString("button.start_game"));
        startBtn.setFont(new Font("宋体", Font.BOLD, 14));
        startBtn.addActionListener(e -> {
            game.setAiDifficulty(difficultyCombo.getSelectedIndex());
            game.setPlayerIsBlack(blackRadio.isSelected());
            game.getAi().setDifficulty(game.getAiDifficulty()); // 更新AI实例难度
            game.getUi().updateAiLabel("AI: IDLE");
            game.startNewGame();
            settingsWindow.dispose();
            if (parentWindow != null) {
                parentWindow.dispose();
            }
        });
        panel.add(startBtn);
        
        settingsWindow.add(panel);
        settingsWindow.setVisible(true);
    }

    /**
     * 显示设置对话框
     */
    public void showSettingsDialog() {
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
            messages.getString("language.chinese_simple"), messages.getString("language.chinese_traditional"), 
            messages.getString("language.english"), messages.getString("language.german"),
            messages.getString("language.korean_north_korea"), messages.getString("language.korean_south_korea")
        };
        
        languageComboBox = new JComboBox<>(languageOptions);
        languageComboBox.setFont(font);
        
        // 设置当前选中的语言
        int selectedIndex = 2; // Default to English
        if (currentLocale.getLanguage().equals("zh")) {
            selectedIndex = currentLocale.getCountry().equals("CN") ? 0 : 1;
        } else if (currentLocale.getLanguage().equals("de")) {
            selectedIndex = 3;
        } else if (currentLocale.getLanguage().equals("ko")) {
            selectedIndex = currentLocale.getCountry().equals("KP") ? 4 : 5;
        }
        languageComboBox.setSelectedIndex(selectedIndex);
        
        languageComboBox.addActionListener(f -> {
            int selected = languageComboBox.getSelectedIndex();
            Locale newLocale;
            switch (selected) {
                case 0: newLocale = Locale.of("zh", "CN"); break;
                case 1: newLocale = Locale.of("zh", "TW"); break;
                case 3: newLocale = Locale.of("de", "DE"); break;
                case 4: newLocale = Locale.of("ko", "KP"); break;
                case 5: newLocale = Locale.of("ko", "KR"); break;
                case 2:
                default: newLocale = Locale.of("en", "US"); break;
            }
            
            setLanguage(newLocale);
            updateUILanguage();
            
            String message = messages.getString("message.language_changed");
            JOptionPane.showMessageDialog(settingsWindow, message, messages.getString("message.title.info"), JOptionPane.INFORMATION_MESSAGE);
        });
        
        settingPanel.add(languageComboBox);
        settingPanel.add(new JLabel()); // 占位符
        settingPanel.add(new JLabel()); // 占位符
        
        settingsWindow.add(settingPanel);
        settingsWindow.setVisible(true);
    }

    /**
     * 显示关于对话框
     */
    public void showAboutDialog() {
        JFrame aboutWindow = new JFrame(messages.getString("dialog.about"));
        Font font = new Font("宋体", Font.PLAIN, 14);
        aboutWindow.setResizable(false);
        aboutWindow.setLocationRelativeTo(null);
        aboutWindow.setSize(300, 300);
        aboutWindow.setLayout(new BorderLayout());

        JLabel label = new JLabel("<html><div style='text-align: center; font-family: Arial; font-size: 14px; line-height: 1.5;'>"
            + messages.getString("about.version") + "<br>" + messages.getString("about.date") + "<br>"
            + messages.getString("about.copyright") + "<br>" + messages.getString("about.description")
            + "</div></html>", JLabel.CENTER);
        label.setFont(font);

        JButton licenceBtn = new JButton(messages.getString("button.license"));
        JButton websiteBtn = new JButton(messages.getString("button.website"));
        
        licenceBtn.addActionListener(f -> showLicenseDialog());
        websiteBtn.addActionListener(f -> {
            if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://qiuerichanru.work.gd"));
                } catch (IOException | URISyntaxException e1) {
                    JOptionPane.showMessageDialog(null, messages.getString("message.browser_error"));
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
    
    // ------------------- 状态消息显示 -------------------

    public void showMessage(String messageKey, String titleKey, int messageType) {
        JOptionPane.showMessageDialog(game, 
            messages.getString(messageKey), 
            messages.getString(titleKey), 
            messageType);
    }

    public void showWinMessage(int winnerType) {
        String winner = winnerType == 1 ? messages.getString("game.black") : messages.getString("game.white");
        JOptionPane.showMessageDialog(game, winner + messages.getString("game.win"));
    }

    public void showDrawMessage() {
        JOptionPane.showMessageDialog(game, messages.getString("game.draw"));
    }
}