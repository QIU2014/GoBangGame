package com.eric.GobangGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

/**
 * 棋盘面板：负责绘制棋盘、棋子，处理鼠标落子事件
 */
public class ChessboardPanel extends JPanel {
    private final GobangGame game;
    private final int ROW;
    private final int COL;
    private final int CELL_SIZE;
    private final int MARGIN;

    // 移除 board 参数
    public ChessboardPanel(GobangGame game, int row, int col, int cellSize, int margin) {
        this.game = game;
        this.ROW = row;
        this.COL = col;
        this.CELL_SIZE = cellSize;
        this.MARGIN = margin;

        setPreferredSize(new Dimension(MARGIN * 2 + COL * CELL_SIZE, MARGIN * 2 + ROW * CELL_SIZE));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (game.isGameOver()) return;

                int x = e.getX();
                int y = e.getY();
                int j = Math.round((float)(x - MARGIN) / CELL_SIZE); // 计算列 (j)
                int i = Math.round((float)(y - MARGIN) / CELL_SIZE); // 计算行 (i)

                // 校验坐标是否合法
                if (i >= 0 && i < ROW && j >= 0 && j < COL) {
                    int[][] board = game.getBoard(); // 从 game 获取当前棋盘
                    if (board[i][j] == 0) {
                        int currentPlayer = game.isBlackTurn() ? 1 : 2;
                        @SuppressWarnings("unused")
                        ResourceBundle messages = game.getUi().getMessages();

                        // 人机对战模式下的回合检查
                        if (game.getGameMode() == 1) {
                            int playerColor = game.isPlayerIsBlack() ? 1 : 2;
                            if (currentPlayer != playerColor) {
                                game.getUi().showMessage("message.ai_thinking", "message.title.info", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                        }

                        // 调用Handler进行落子和状态更新
                        game.getHandler().playerMove(i, j, currentPlayer);
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawChessboard(g);
        drawChessPieces(g);
    }

    // 绘制棋盘（15x15网格+星位）
    private void drawChessboard(Graphics g) {
        g.setColor(Color.BLACK);
        // 绘制横线和竖线
        for (int i = 0; i < ROW; i++) {
            int y = MARGIN + i * CELL_SIZE;
            g.drawLine(MARGIN, y, MARGIN + (COL - 1) * CELL_SIZE, y);
            int x = MARGIN + i * CELL_SIZE;
            g.drawLine(x, MARGIN, x, MARGIN + (ROW - 1) * CELL_SIZE);
        }

        // 绘制星位
        int centerIndex = 7;
        int starIndex1 = 3;
        int starIndex2 = 11;

        int center = MARGIN + centerIndex * CELL_SIZE;
        int star1 = MARGIN + starIndex1 * CELL_SIZE;
        int star2 = MARGIN + starIndex2 * CELL_SIZE;

        drawChessPoint(g, center, center);
        drawChessPoint(g, star1, star1);
        drawChessPoint(g, star1, star2);
        drawChessPoint(g, star2, star1);
        drawChessPoint(g, star2, star2);
    }

    // 绘制棋盘上的黑点（星位和中心）
    private void drawChessPoint(Graphics g, int x, int y) {
        g.fillOval(x - 3, y - 3, 6, 6);
    }

    // 绘制已落的棋子
    private void drawChessPieces(Graphics g) {
        int[][] board = game.getBoard(); // 从 game 获取当前棋盘

        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                if (board[i][j] != 0) {
                    int x = MARGIN + j * CELL_SIZE;
                    int y = MARGIN + i * CELL_SIZE;

                    if (board[i][j] == 1) { // 黑棋
                        g.setColor(Color.BLACK);
                        g.fillOval(x - CELL_SIZE / 2 + 2, y - CELL_SIZE / 2 + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                    } else { // 白棋
                        g.setColor(Color.BLACK);
                        g.drawOval(x - CELL_SIZE / 2 + 2, y - CELL_SIZE / 2 + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                        g.setColor(Color.WHITE);
                        g.fillOval(x - CELL_SIZE / 2 + 3, y - CELL_SIZE / 2 + 3, CELL_SIZE - 6, CELL_SIZE - 6);
                    }
                }
            }
        }
    }
}