package com.eric.GobangGame;

import javax.swing.SwingUtilities;

/**
 * 负责处理游戏的核心规则逻辑：落子、胜负判定、平局判定、AI回合管理
 */
public class GobangGameHandler {
    
    private final GobangGame game;
    private final int ROW;
    private final int COL;

    public GobangGameHandler(GobangGame game, int row, int col) {
        this.game = game;
        this.ROW = row;
        this.COL = col;
    }

    /**
     * 玩家/AI落子并检查游戏状态
     */
    /**
     * 玩家/AI落子并检查游戏状态
     */
    public void playerMove(int row, int col, int playerType) {
        System.out.println("Handler.playerMove 被调用: 位置(" + row + ", " + col + "), 玩家类型=" + playerType); // 调试

        int[][] board = game.getBoard();

        // 1. 记录落子
        board[row][col] = playerType;
        game.getMoveHistory().add(new int[]{row, col});
        game.repaint(); // 立即更新棋盘显示

        System.out.println("落子完成，当前黑棋回合: " + game.isBlackTurn()); // 调试

        // 2. 检查游戏是否结束
        if (checkWin(row, col)) {
            System.out.println("游戏结束，玩家" + playerType + "获胜"); // 调试
            game.getUi().showWinMessage(playerType);
            game.setGameOver(true);
        } else if (checkDraw()) {
            System.out.println("游戏平局"); // 调试
            game.getUi().showDrawMessage();
            game.setGameOver(true);
        } else {
            // 3. 切换回合
            game.setBlackTurn(!game.isBlackTurn());
            System.out.println("切换回合，新回合黑棋: " + game.isBlackTurn()); // 调试

            // 4. 如果是人机对战，且游戏未结束，让AI落子
            if (game.getGameMode() == 1 && !game.isGameOver()) {
                System.out.println("人机模式，准备AI回合..."); // 调试
                aiTurn();
            }
        }
    }

    /**
     * AI思考并落子
     */
    public void aiTurn() {
        if (game.isGameOver()) return;

        // 确定AI的棋子颜色
        int aiPlayer = game.isPlayerIsBlack() ? 2 : 1; // 玩家执黑则AI执白，反之亦然

        // 判断是否该AI落子
        if ((game.isBlackTurn() && aiPlayer == 1) || (!game.isBlackTurn() && aiPlayer == 2)) {
            // 更新AI状态为Working
            game.getUi().updateAiLabel("AI: Thinking...");

            // 在新线程中执行AI计算（避免UI冻结）
            new Thread(() -> {
                try {
                    // 根据难度设置不同的思考时间
                    int thinkTime = (game.getAiDifficulty() + 1) * 500; // 0.5-1.5秒
                    Thread.sleep(thinkTime); // 模拟思考时间

                    SwingUtilities.invokeLater(() -> {
                        // AI计算最佳落子位置
                        int[] move = game.getAi().calculateMove(game.getBoard(), aiPlayer);

                        if (move[0] != -1 && move[1] != -1 && game.getBoard()[move[0]][move[1]] == 0) {
                            // AI落子前再次确认状态
                            game.getUi().updateAiLabel("AI: Moving...");

                            // AI落子，调用playerMove处理后续状态检查
                            playerMove(move[0], move[1], aiPlayer);

                            // 落子完成后，更新AI状态为IDLE（等待玩家）
                            game.getUi().updateAiLabel("AI: IDLE");
                        }
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().updateAiLabel("AI: Error");
                    });
                }
            }).start();
        }
    }

    /**
     * 检查是否获胜：判断当前落子位置是否形成五子连线
     */
    public boolean checkWin(int row, int col) {
        int[][] board = game.getBoard();
        int type = board[row][col]; // 当前落子的棋子类型
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}}; // 四个方向

        for (int[] dir : directions) {
            int count = 1; 
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

            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否平局：棋盘无空位且无人获胜
     */
    public boolean checkDraw() {
        int[][] board = game.getBoard();
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] == 0) {
                    return false; // 还有空位，不是平局
                }
            }
        }
        return true;
    }
}