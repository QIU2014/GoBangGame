package com.eric.GobangGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 五子棋AI对战实现
 * 支持简单、中等、困难三种难度
 */
public class GobangGameAi {
    
    private static final int ROW = 15;
    private static final int COL = 15;
    private int aiDifficulty; // 0=简单，1=中等，2=困难
    private Random random;
    
    public GobangGameAi(int difficulty) {
        this.aiDifficulty = difficulty;
        this.random = new Random();
    }
    
    /**
     * AI计算最佳落子位置
     * @param board 当前棋盘状态
     * @param aiPlayer AI的棋子类型（1=黑，2=白）
     * @return [row, col] 最佳落子位置
     */
    public int[] calculateMove(int[][] board, int aiPlayer) {
        switch (aiDifficulty) {
            case 0: // 简单难度：随机落子
                return getRandomMove(board);
            case 1: // 中等难度：基于简单评估
                return getMediumMove(board, aiPlayer);
            case 2: // 困难难度：使用Minimax算法
                return getHardMove(board, aiPlayer);
            default:
                return getRandomMove(board);
        }
    }
    
    /**
     * 简单难度：在空白位置随机落子
     */
    private int[] getRandomMove(int[][] board) {
        List<int[]> emptyCells = new ArrayList<>();
        
        // 收集所有空位置
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] == 0) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }
        if (emptyCells.isEmpty()) {
            return new int[]{-1, -1}; // 棋盘已满
        }
        
        // 随机选择一个空位置
        return emptyCells.get(random.nextInt(emptyCells.size()));
    }
    
    /**
     * 中等难度：基于棋型评估
     */
    private int[] getMediumMove(int[][] board, int aiPlayer) {
        int humanPlayer = (aiPlayer == 1) ? 2 : 1;
        
        // 优先级1：如果有一步取胜的机会，就下在那里
        int[] winningMove = findWinningMove(board, aiPlayer);
        if (winningMove[0] != -1) {
            return winningMove;
        }
        
        // 优先级2：阻止对手立即获胜
        int[] blockingMove = findWinningMove(board, humanPlayer);
        if (blockingMove[0] != -1) {
            return blockingMove;
        }
        
        // 优先级3：评估所有位置，选择最优
        return evaluateBestMove(board, aiPlayer, humanPlayer);
    }
    
    /**
     * 困难难度：使用Minimax算法
     */
    private int[] getHardMove(int[][] board, int aiPlayer) {
        // 使用带Alpha-Beta剪枝的Minimax算法
        Object[] result = minimax(board, 3, true, Integer.MIN_VALUE, Integer.MAX_VALUE, aiPlayer);
        return (int[]) result[1];
    }
    
    /**
     * 查找立即获胜的位置
     */
    private int[] findWinningMove(int[][] board, int player) {
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] == 0) {
                    // 尝试在此位置落子
                    board[i][j] = player;
                    if (checkWinForPosition(board, i, j, player)) {
                        board[i][j] = 0; // 恢复棋盘
                        return new int[]{i, j};
                    }
                    board[i][j] = 0; // 恢复棋盘
                }
            }
        }
        return new int[]{-1, -1}; // 没有立即获胜的位置
    }
    
    /**
     * 评估最佳落子位置（中等难度使用）
     */
    private int[] evaluateBestMove(int[][] board, int aiPlayer, int humanPlayer) {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = new int[]{-1, -1};
        
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] == 0) {
                    // 评估这个位置对AI的得分
                    int score = evaluatePosition(board, i, j, aiPlayer, humanPlayer);
                    
                    // 添加一些随机性，避免完全确定性的行为
                    score += random.nextInt(10);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove[0] = i;
                        bestMove[1] = j;
                    }
                }
            }
        }
        
        // 如果没有找到合适的位置，随机选择一个
        if (bestMove[0] == -1) {
            return getRandomMove(board);
        }
        
        return bestMove;
    }
    
    /**
     * 评估位置的得分
     */
    private int evaluatePosition(int[][] board, int row, int col, int aiPlayer, int humanPlayer) {
        int score = 0;
        
        // 检查四个方向的棋型
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            // 评估AI的棋型
            int aiPattern = evaluatePattern(board, row, col, dir[0], dir[1], aiPlayer);
            score += getPatternScore(aiPattern) * 2; // AI的棋型得分加倍
            
            // 评估对手的棋型（需要防守）
            int humanPattern = evaluatePattern(board, row, col, dir[0], dir[1], humanPlayer);
            score += getPatternScore(humanPattern); // 防守对手的棋型
        }
        
        // 中心位置优先
        int centerRow = ROW / 2;
        int centerCol = COL / 2;
        int distanceFromCenter = Math.abs(row - centerRow) + Math.abs(col - centerCol);
        score += (14 - distanceFromCenter) * 5; // 越靠近中心得分越高
        
        return score;
    }
    
    /**
     * 评估特定方向的棋型
     */
    private int evaluatePattern(int[][] board, int row, int col, int dx, int dy, int player) {
        int count = 1; // 当前位置
        
        // 正向检查
        for (int i = 1; i < 5; i++) {
            int r = row + i * dx;
            int c = col + i * dy;
            if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == player) {
                count++;
            } else if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == 0) {
                // 空位置，继续
            } else {
                break; // 遇到对手棋子或边界
            }
        }
        
        // 反向检查
        for (int i = 1; i < 5; i++) {
            int r = row - i * dx;
            int c = col - i * dy;
            if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == player) {
                count++;
            } else if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == 0) {
                // 空位置，继续
            } else {
                break; // 遇到对手棋子或边界
            }
        }
        
        return count;
    }
    
    /**
     * 根据棋型数量返回得分
     */
    private int getPatternScore(int patternLength) {
        switch (patternLength) {
            case 5: return 100000; // 五连，必胜
            case 4: return 10000;  // 四连，威胁很大
            case 3: return 1000;   // 三连，有潜力
            case 2: return 100;    // 二连
            case 1: return 10;     // 单子
            default: return patternLength > 5 ? 1000000 : 0;
        }
    }
    /**
     * Minimax算法实现（带Alpha-Beta剪枝）
     */
    private Object[] minimax(int[][] board, int depth, boolean isMaximizing, 
                          int alpha, int beta, int aiPlayer) {
        int humanPlayer = (aiPlayer == 1) ? 2 : 1;
        
        // 深度为0或游戏结束，返回评估值
        if (depth == 0 || isGameOver(board)) {
            return new Object[]{evaluateBoard(board, aiPlayer, humanPlayer), null};
        }
        
        int[] bestMove = new int[]{-1, -1};
        
        if (isMaximizing) { // AI回合（最大化）
            int maxEval = Integer.MIN_VALUE;
            
            // 获取所有可能的位置
            List<int[]> possibleMoves = getPossibleMoves(board);
            
            for (int[] move : possibleMoves) {
                int i = move[0];
                int j = move[1];
                
                // 模拟落子
                board[i][j] = aiPlayer;
                Object[] childResult = minimax(board, depth - 1, false, alpha, beta, aiPlayer);
                int eval = (Integer) childResult[0];
                board[i][j] = 0; // 撤销落子
                
                if (eval > maxEval) {
                    maxEval = eval;
                    bestMove = move;
                }
                
                // Alpha-Beta剪枝
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break; // 剪枝
                }
            }
            return new Object[]{maxEval, bestMove};
        } else { // 对手回合（最小化）
            int minEval = Integer.MAX_VALUE;
            
            List<int[]> possibleMoves = getPossibleMoves(board);
            
            for (int[] move : possibleMoves) {
                int i = move[0];
                int j = move[1];
                
                board[i][j] = humanPlayer;
                Object[] childResult = minimax(board, depth - 1, true, alpha, beta, aiPlayer);
                int eval = (Integer) childResult[0];
                board[i][j] = 0;
                
                if (eval < minEval) {
                    minEval = eval;
                    bestMove = move;
                }
                
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return new Object[]{minEval, bestMove};
        }
    }
    
    /**
     * 获取所有可能的落子位置（优化：只考虑有棋子的附近位置）
     */
    private List<int[]> getPossibleMoves(int[][] board) {
        List<int[]> moves = new ArrayList<>();
        
        // 先收集有棋子位置周围的空位
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] != 0) {
                    // 检查周围3×3区域
                    for (int di = -2; di <= 2; di++) {
                        for (int dj = -2; dj <= 2; dj++) {
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < ROW && nj >= 0 && nj < COL && 
                                board[ni][nj] == 0 && !containsMove(moves, ni, nj)) {
                                moves.add(new int[]{ni, nj});
                            }
                        }
                    }
                }
            }
        }
        
        // 如果还没有落子，选择中心位置
        if (moves.isEmpty()) {
            moves.add(new int[]{ROW/2, COL/2});
        }
        
        return moves;
    }
    
    private boolean containsMove(List<int[]> moves, int row, int col) {
        for (int[] move : moves) {
            if (move[0] == row && move[1] == col) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 评估整个棋盘的得分
     */
    private int evaluateBoard(int[][] board, int aiPlayer, int humanPlayer) {
        int score = 0;
        
        // 检查AI是否有获胜的可能
        if (findWinningMove(board, aiPlayer)[0] != -1) {
            return 1000000;
        }
        
        // 检查对手是否有获胜的可能
        if (findWinningMove(board, humanPlayer)[0] != -1) {
            return -1000000;
        }
        
        // 评估所有位置的棋型
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] == aiPlayer) {
                    score += evaluateAllPatterns(board, i, j, aiPlayer);
                } else if (board[i][j] == humanPlayer) {
                    score -= evaluateAllPatterns(board, i, j, humanPlayer);
                }
            }
        }
        
        return score;
    }
    
    /**
     * 评估棋子所有方向的棋型
     */
    private int evaluateAllPatterns(int[][] board, int row, int col, int player) {
        int totalScore = 0;
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int pattern = evaluatePattern(board, row, col, dir[0], dir[1], player);
            totalScore += getPatternScore(pattern);
        }
        
        return totalScore;
    }
    
    /**
     * 检查游戏是否结束
     */
    private boolean isGameOver(int[][] board) {
        // 检查是否有五子连线
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] != 0) {
                    if (checkWinForPosition(board, i, j, board[i][j])) {
                        return true;
                    }
                }
            }
        }
        
        // 检查是否棋盘已满
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] == 0) {
                    return false; // 还有空位
                }
            }
        }
        return true; // 棋盘已满
    }
    
    /**
     * 检查特定位置是否形成五子连线
     */
    private boolean checkWinForPosition(int[][] board, int row, int col, int player) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int count = 1;
            int dx = dir[0];
            int dy = dir[1];
            
            // 正向检查
            for (int i = 1; i < 5; i++) {
                int r = row + i * dx;
                int c = col + i * dy;
                if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == player) {
                    count++;
                } else {
                    break;
                }
            }
            
            // 反向检查
            for (int i = 1; i < 5; i++) {
                int r = row - i * dx;
                int c = col - i * dy;
                if (r >= 0 && r < ROW && c >= 0 && c < COL && board[r][c] == player) {
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
     * 设置AI难度
     */
    public void setDifficulty(int difficulty) {
        this.aiDifficulty = difficulty;
    }
    
    /**
     * 获取当前难度
     */
    public int getDifficulty() {
        return aiDifficulty;
    }
}