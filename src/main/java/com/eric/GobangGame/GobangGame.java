package com.eric.GobangGame;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 五子棋游戏主类 - 协调器
 * 职责：管理游戏状态，协调 UI、Handler 和 AI 的交互
 */
public class GobangGame extends JFrame{
    
    // --- 棋盘常量 (仍保留在主类中，因为其他组件需要访问它们来确定尺寸) ---
    public static final int ROW = 15;
    public static final int COL = 15;
    public static final int CELL_SIZE = 40;
    public static final int MARGIN = 30; 
    public static final int BUTTON_WIDTH = 120;
    public static final int BUTTON_HEIGHT = 40;

    // --- 核心游戏状态 ---
    private int[][] board = new int[ROW][COL];
    private boolean isBlackTurn = true; 
    private boolean gameOver = false;
    private List<int[]> moveHistory = new ArrayList<>(); 
    
    // --- 游戏模式状态 ---
    private int gameMode = 0; // 0=双人，1=人机
    private int aiDifficulty = 1; // AI难度
    private boolean playerIsBlack = true; // 玩家是否执黑

    // --- 模块引用 ---
    private GobangGameAi ai;
    private GobangGameUI ui;
    private GobangGameHandler handler;
    private ChessboardPanel chessboard; 

    // 构造方法
    public GobangGame() {
        // 1. 初始化模块
        this.ai = new GobangGameAi(aiDifficulty);
        this.ui = new GobangGameUI(this, Locale.of("zh", "CN")); // 默认语言
        this.handler = new GobangGameHandler(this, ROW, COL);
        
        // 2. 初始化窗口属性
        setTitle(ui.getMessages().getString("game.title"));
        int windowWidth = MARGIN * 2 + COL * CELL_SIZE + BUTTON_WIDTH + 40;
        int windowHeight = MARGIN * 2 + ROW * CELL_SIZE;
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false); 

        // 3. 构建布局
        Container container = getContentPane();
        container.setLayout(new BorderLayout(20, 0));

        // 4. 添加棋盘面板 (View)
        this.chessboard = new ChessboardPanel(this, board, ROW, COL, CELL_SIZE, MARGIN);
        container.add(chessboard, BorderLayout.CENTER);

        // 5. 添加按钮面板 (View)
        JPanel buttonPanel = ui.createButtonPanel();
        container.add(buttonPanel, BorderLayout.EAST); 

        // 6. 添加菜单栏 (View)
        setJMenuBar(ui.createMenuBar());
    }

    // --- 公共的协调方法 ---

    /**
     * 开始新游戏
     */
    public void startNewGame() {
        board = new int[ROW][COL]; // 重置棋盘
        moveHistory.clear();
        isBlackTurn = true;
        gameOver = false;
        
        // 如果是人机对战且AI先手，则调用AI回合
        if (gameMode == 1 && (isBlackTurn && !playerIsBlack || !isBlackTurn && playerIsBlack)) {
            SwingUtilities.invokeLater(() -> {
                handler.aiTurn();
            });
        }
        
        repaint();
    }
    
    /**
     * 悔棋操作
     */
    public void undoMove() {
        if (gameOver || moveHistory.isEmpty()) {
            ui.showMessage(
                gameOver ? "message.cannot_undo_game_over" : "message.cannot_undo_no_moves",
                "message.title.tip",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 撤销最后一步落子
        int[] lastMove = moveHistory.remove(moveHistory.size() - 1);
        board[lastMove[0]][lastMove[1]] = 0; 
        isBlackTurn = !isBlackTurn; 
        
        // 如果是AI模式，可能需要额外悔一步（悔掉AI的）
        if (gameMode == 1 && !moveHistory.isEmpty()) {
             // 确保悔棋后的回合是玩家的回合
            int playerColor = playerIsBlack ? 1 : 2;
            int currentPlayer = isBlackTurn ? 1 : 2;
            
            if (currentPlayer != playerColor) {
                lastMove = moveHistory.remove(moveHistory.size() - 1);
                board[lastMove[0]][lastMove[1]] = 0; 
                isBlackTurn = !isBlackTurn; 
            }
        }
        
        repaint();
    }
    
    /**
     * Save current game state to a file
     */
    public void saveGame(File file) {
    	try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
    		GameSave save = new GameSave(board, isBlackTurn, gameOver, moveHistory, gameMode, playerIsBlack, aiDifficulty);
    		oos.writeObject(save);
    		ui.showMessage("message.save_success", "message.title.success", JOptionPane.INFORMATION_MESSAGE);
    	} catch (IOException ex) {
    		ui.showMessage("message.save_failed", "message.title.error", JOptionPane.ERROR_MESSAGE);
    	}
    }

    /**
     * Load game state from a file
     */
    public void loadGame(File file) {
    	try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
    		GameSave save = (GameSave) ois.readObject();
        
    		// Restore game state
    		this.board = save.getBoard();
    		this.isBlackTurn = save.isBlackTurn();
    		this.gameOver = save.isGameOver();
    		this.moveHistory = save.getMoveHistory();
            this.gameMode = save.getGameMode();
            this.playerIsBlack = save.isPlayerIsBlack();
            this.aiDifficulty = save.getAiDifficulty();
            this.ai.setDifficulty(save.getAiDifficulty());
            this.chessboard.setBoard(this.board);

    		// 加载存档后，如果游戏未结束且是人机对战模式，可能需要AI行动
    		if (gameMode == 1 && !gameOver) {
    			int aiPlayer = playerIsBlack ? 2 : 1;
    			if ((isBlackTurn && aiPlayer == 1) || (!isBlackTurn && aiPlayer == 2)) {
    				handler.aiTurn();
    			}
    		}
        
    		repaint();
    		ui.showMessage("message.load_success", "message.title.success", JOptionPane.INFORMATION_MESSAGE);
    	} catch (IOException | ClassNotFoundException ex) {
    		ui.showMessage("message.load_failed", "message.title.error", JOptionPane.ERROR_MESSAGE);
    	}
    }
    
    // --- Getters and Setters ---

    public int[][] getBoard() { return board; }
    public boolean isBlackTurn() { return isBlackTurn; }
    public void setBlackTurn(boolean isBlackTurn) { this.isBlackTurn = isBlackTurn; }
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    public List<int[]> getMoveHistory() { return moveHistory; }
    public int getGameMode() { return gameMode; }
    public void setGameMode(int gameMode) { this.gameMode = gameMode; }
    public boolean isPlayerIsBlack() { return playerIsBlack; }
    public void setPlayerIsBlack(boolean playerIsBlack) { this.playerIsBlack = playerIsBlack; }
    public int getAiDifficulty() { return aiDifficulty; }
    public void setAiDifficulty(int aiDifficulty) { this.aiDifficulty = aiDifficulty; }
    
    // 模块 Getter
    public GobangGameAi getAi() { return ai; }
    public GobangGameUI getUi() { return ui; }
    public GobangGameHandler getHandler() { return handler; }
    
    // 常量 Getter
    public int getBUTTON_WIDTH() { return BUTTON_WIDTH; }
    public int getBUTTON_HEIGHT() { return BUTTON_HEIGHT; }

    // 主方法
    public static void main(String[] args) {
    	SwingUtilities.invokeLater(() -> {
    		try {
    			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        
    		GobangGame game = new GobangGame();
    		game.setVisible(true);
        
    		SwingUtilities.invokeLater(() -> {
    			game.ui.showGameModeDialog();
    		});
    	});
    }
}