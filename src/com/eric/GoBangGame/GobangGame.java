package com.eric.GoBangGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 五子棋游戏（双人对战版）- 右侧添加悔棋按钮
 * 功能：棋盘绘制、落子、胜负判定、悔棋、重新开始
 */
public class GobangGame extends JFrame {
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

    // 构造方法：初始化游戏窗口（棋盘+右侧按钮）
    public GobangGame() {
        setTitle("Java 五子棋游戏");
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
        JButton undoBtn = new JButton("悔棋");
        undoBtn.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        undoBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        
        JButton closeBtn = new JButton("退出");
        closeBtn.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        closeBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        

        // 悔棋按钮点击事件
        undoBtn.addActionListener(e -> {
            if (gameOver || moveHistory.isEmpty()) {
                JOptionPane.showMessageDialog(GobangGame.this, 
                    gameOver ? "游戏已结束，无法悔棋！" : "暂无落子，无法悔棋！");
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
        JButton restartBtn = new JButton("重新开始游戏");
        restartBtn.setPreferredSize(new Dimension(120, 40));
        restartBtn.setFont(new Font("宋体", Font.PLAIN, 14));
        restartBtn.addActionListener(e -> {
            board = new int[ROW][COL];
            moveHistory.clear();
            isBlackTurn = true;
            gameOver = false;
            chessboard.repaint();
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
        JMenu gameMenu = new JMenu("游戏");
        JMenu fileMenu = new JMenu("文件");
        JMenu toolsMenu = new JMenu("工具");
        JMenu aboutMenu = new JMenu("关于");
        JMenuItem undoItem = new JMenuItem("悔棋");
        JMenuItem restartItem = new JMenuItem("重新开始");
        JMenuItem closeItem = new JMenuItem("退出");
        JMenuItem settingItem = new JMenuItem("选项");
        JMenuItem aboutItem = new JMenuItem("关于软件");
        undoItem.addActionListener(e -> undoBtn.doClick()); // 菜单悔棋与按钮联动
        restartItem.addActionListener(e -> restartBtn.doClick()); // 菜单重启与按钮联动
        closeItem.addActionListener(e -> closeBtn.doClick());
        aboutItem.addActionListener(e -> {
        	JFrame aboutWindow = new JFrame("关于软件");
        	Font font = new Font("宋体", Font.PLAIN, 14);
        	aboutWindow.setResizable(false);
        	aboutWindow.setLocationRelativeTo(null);
        	aboutWindow.setSize(300, 300);

        	// Use BorderLayout with proper spacing
        	aboutWindow.setLayout(new BorderLayout());

        	JLabel label = new JLabel("<html><div style='text-align: center; font-family: Arial; font-size: 14px; line-height: 1.5;'>"
        	    + "GoBangGame v1.0<br>"
        	    + "2025-11-21<br>"
        	    + "版权所有 (C) 2025 邱翰如<br>"
        	    + "GoBangGame是免费软件。"
        	    + "</div></html>", JLabel.CENTER);
        	label.setFont(font);

        	JButton licenceBtn = new JButton("许可证");
        	licenceBtn.setPreferredSize(new Dimension(100, 35));
        	licenceBtn.addActionListener(f -> {
        	    JFrame licenceWindow = new JFrame("许可证");
        	    licenceWindow.setResizable(false);
        	    licenceWindow.setLocationRelativeTo(null);
        	    
        	    JTextArea licenceText = new JTextArea();
        	    licenceText.setText("MIT Licence\n\n" +
        	        "Copyright 2025 邱翰如\n\n" +
        	        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
        	        "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
        	        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.");
        	    licenceText.setEditable(false);
        	    licenceText.setLineWrap(true);
        	    licenceText.setWrapStyleWord(true);
        	    licenceText.setFont(font);
        	    licenceText.setMargin(new Insets(15, 15, 15, 15));
        	    
        	    licenceWindow.setSize(500, 600);
        	    licenceWindow.add(new JScrollPane(licenceText));
        	    licenceWindow.setVisible(true);
        	});
        	JButton websiteBtn = new JButton("官网");
        	websiteBtn.setPreferredSize(new Dimension(100, 35));
        	websiteBtn.addActionListener(f -> {
        		JOptionPane.showMessageDialog(null, "敬请期待!","敬请期待!", JOptionPane.INFORMATION_MESSAGE);
        	});

        	licenceBtn.setFont(font);
        	websiteBtn.setFont(font);

        	// Create a panel for the button with proper padding
        	JPanel buttonPanelLicence = new JPanel(new FlowLayout());
        	buttonPanelLicence.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0)); // Add padding
        	buttonPanelLicence.add(licenceBtn);
        	buttonPanelLicence.add(websiteBtn);

        	// Add components with proper positioning
        	aboutWindow.add(label, BorderLayout.CENTER);      // Text in center
        	aboutWindow.add(buttonPanelLicence, BorderLayout.SOUTH); // Button at bottom with padding

        	aboutWindow.setVisible(true);
        });
        toolsMenu.add(settingItem);
        fileMenu.add(closeItem);
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
                        // 记录落子
                        board[i][j] = isBlackTurn ? 1 : 2;
                        moveHistory.add(new int[]{i, j});

                        // 检查是否获胜
                        if (checkWin(i, j)) {
                            String winner = isBlackTurn ? "黑棋" : "白棋";
                            JOptionPane.showMessageDialog(GobangGame.this, winner + "获胜！");
                            gameOver = true;
                        }
                        // 检查是否平局（棋盘满了但无人获胜）
                        else if (checkDraw()) {
                            JOptionPane.showMessageDialog(GobangGame.this, "平局！");
                            gameOver = true;
                        }

                        // 切换回合
                        isBlackTurn = !isBlackTurn;
                        repaint(); // 重绘棋盘
                    }
                }
            });
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
    }

    // 主方法：启动游戏
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GobangGame game = new GobangGame();
            game.setVisible(true);
        });
    }
}
