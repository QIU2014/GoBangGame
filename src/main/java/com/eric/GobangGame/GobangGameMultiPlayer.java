package com.eric.GobangGame;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * 五子棋多人游戏实现 - 支持网络对战
 * 职责：处理网络连接、消息传递、回合同步
 */
public class GobangGameMultiPlayer {

    private final GobangGame game;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isHost = false;
    private boolean isMyTurn = false;
    private String playerName = "Player";
    private String opponentName = "Opponent";
    private int myPlayerType = 1; // 1=黑棋，2=白棋
    private ExecutorService networkExecutor;
    private boolean connected = false;
    private String serverAddress = "localhost";
    private int port = 12345;
    private SwingWorker<Boolean, Void> connectionWorker;

    // 消息类型常量
    private static final String MSG_MOVE = "MOVE";
    private static final String MSG_CHAT = "CHAT";
    private static final String MSG_START = "START";
    private static final String MSG_RESTART = "RESTART";
    private static final String MSG_DISCONNECT = "DISCONNECT";
    private static final String MSG_PLAYER_INFO = "PLAYER_INFO";
    private static final String MSG_GAME_OVER = "GAME_OVER";

    public GobangGameMultiPlayer(GobangGame game) {
        this.game = game;
        this.networkExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * 作为主机创建服务器
     */
    public void createServer(int port, String playerName) {
        this.isHost = true;
        this.port = port;
        this.playerName = playerName;
        this.isMyTurn = true; // 主机默认执黑先手
        this.myPlayerType = 1;

        connectionWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    SwingUtilities.invokeLater(() -> {
                        // 更新UI状态为等待连接
                        game.getUi().updateAiLabel(game.getUi().getMessages().getString("message.waiting_connection"));
                    });

                    // 设置超时时间（30秒）
                    serverSocket.setSoTimeout(30000);
                    socket = serverSocket.accept();

                    // 连接建立成功
                    setupStreams();
                    connected = true;

                    // 发送玩家信息
                    sendMessage(MSG_PLAYER_INFO + ":" + playerName);

                    // 开始监听消息
                    startMessageListener();

                    return true;

                } catch (SocketTimeoutException e) {
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().updateAiLabel("AI: IDLE");
                        game.getUi().showMessage("message.connection_timeout", "message.title.error", JOptionPane.ERROR_MESSAGE);
                    });
                    return false;
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().updateAiLabel("AI: IDLE");
                        game.getUi().showMessage("message.server_failed", "message.title.error", JOptionPane.ERROR_MESSAGE);
                    });
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        // 连接成功，开始游戏
                        SwingUtilities.invokeLater(() -> {
                            startMultiplayerGame();
                        });
                    } else {
                        // 连接失败，重置游戏模式
                        game.setGameMode(0);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().updateAiLabel("AI: IDLE");
                        game.getUi().showMessage("message.connection_error", "message.title.error", JOptionPane.ERROR_MESSAGE);
                        game.setGameMode(0);
                    });
                }
            }
        };

        connectionWorker.execute();
    }

    /**
     * 作为客户端连接服务器
     */
    public void connectToServer(String address, int port, String playerName) {
        this.isHost = false;
        this.serverAddress = address;
        this.port = port;
        this.playerName = playerName;
        this.isMyTurn = false; // 客户端默认执白后手
        this.myPlayerType = 2;

        connectionWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    SwingUtilities.invokeLater(() -> {
                        // 更新UI状态为连接中
                        game.getUi().updateAiLabel(game.getUi().getMessages().getString("message.connecting"));
                    });

                    socket = new Socket(address, port);
                    setupStreams();
                    connected = true;

                    // 发送玩家信息
                    sendMessage(MSG_PLAYER_INFO + ":" + playerName);

                    // 开始监听消息
                    startMessageListener();

                    return true;

                } catch (UnknownHostException e) {
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().updateAiLabel("AI: IDLE");
                        game.getUi().showMessage("message.host_not_found", "message.title.error", JOptionPane.ERROR_MESSAGE);
                    });
                    return false;
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().updateAiLabel("AI: IDLE");
                        game.getUi().showMessage("message.connection_failed", "message.title.error", JOptionPane.ERROR_MESSAGE);
                    });
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        // 连接成功，等待主机开始游戏
                        SwingUtilities.invokeLater(() -> {
                            game.getUi().updateAiLabel(game.getUi().getMessages().getString("message.waiting_for_host"));
                        });
                    } else {
                        // 连接失败，重置游戏模式
                        game.setGameMode(0);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().updateAiLabel("AI: IDLE");
                        game.getUi().showMessage("message.connection_error", "message.title.error", JOptionPane.ERROR_MESSAGE);
                        game.setGameMode(0);
                    });
                }
            }
        };

        connectionWorker.execute();
    }

    /**
     * 设置输入输出流
     */
    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * 开始多人游戏
     */
    private void startMultiplayerGame() {
        // 重置游戏状态
        game.startNewGame();
        game.setGameMode(2); // 2=多人游戏模式

        // 更新UI显示当前回合
        updateTurnDisplay();

        // 发送开始消息给对手
        if (isHost) {
            sendMessage(MSG_START + ":" + myPlayerType);
        }
    }

    /**
     * 发送落子消息
     */
    public void sendMove(int row, int col) {
        if (connected && isMyTurn) {
            sendMessage(MSG_MOVE + ":" + row + "," + col);
            isMyTurn = false;
            updateTurnDisplay();
        }
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(String message) {
        if (connected) {
            sendMessage(MSG_CHAT + ":" + message);
        }
    }

    /**
     * 发送重新开始请求
     */
    public void sendRestartRequest() {
        if (connected) {
            sendMessage(MSG_RESTART);
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * 开始监听网络消息
     */
    private void startMessageListener() {
        networkExecutor.submit(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    // 非主动断开的异常
                    SwingUtilities.invokeLater(() -> {
                        game.getUi().showMessage("message.connection_lost", "message.title.error", JOptionPane.ERROR_MESSAGE);
                        disconnect();
                    });
                }
            }
        });
    }

    /**
     * 处理接收到的消息
     */
    private void processMessage(String message) {
        System.out.println("收到消息: " + message);

        if (message.startsWith(MSG_MOVE)) {
            // 处理对手落子
            String[] parts = message.split(":");
            if (parts.length == 2) {
                String[] coordinates = parts[1].split(",");
                if (coordinates.length == 2) {
                    try {
                        int row = Integer.parseInt(coordinates[0]);
                        int col = Integer.parseInt(coordinates[1]);

                        SwingUtilities.invokeLater(() -> {
                            // 在棋盘上放置对手的棋子
                            int[][] board = game.getBoard();
                            if (board[row][col] == 0) {
                                int opponentPlayerType = (myPlayerType == 1) ? 2 : 1;
                                game.getHandler().playerMove(row, col, opponentPlayerType);
                                isMyTurn = true;
                                updateTurnDisplay();
                            }
                        });
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else if (message.startsWith(MSG_CHAT)) {
            // 处理聊天消息
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                String chatMessage = parts[1];
                SwingUtilities.invokeLater(() -> {
                    showChatMessage(opponentName + ": " + chatMessage);
                });
            }

        } else if (message.startsWith(MSG_START)) {
            // 处理游戏开始
            String[] parts = message.split(":");
            if (parts.length == 2) {
                try {
                    int hostPlayerType = Integer.parseInt(parts[1]);
                    myPlayerType = (hostPlayerType == 1) ? 2 : 1;
                    isMyTurn = (myPlayerType == 1); // 黑棋先手

                    SwingUtilities.invokeLater(() -> {
                        startMultiplayerGame();
                    });
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

        } else if (message.startsWith(MSG_RESTART)) {
            // 处理重新开始请求
            SwingUtilities.invokeLater(() -> {
                int response = JOptionPane.showConfirmDialog(game,
                        opponentName + " " + game.getUi().getMessages().getString("message.restart_request"),
                        game.getUi().getMessages().getString("message.title.confirm"),
                        JOptionPane.YES_NO_OPTION);

                if (response == JOptionPane.YES_OPTION) {
                    sendMessage(MSG_RESTART + ":ACCEPT");
                    restartGame();
                } else {
                    sendMessage(MSG_RESTART + ":REJECT");
                }
            });

        } else if (message.startsWith(MSG_RESTART + ":ACCEPT")) {
            // 对手接受了重新开始请求
            SwingUtilities.invokeLater(() -> {
                restartGame();
            });

        } else if (message.startsWith(MSG_RESTART + ":REJECT")) {
            // 对手拒绝了重新开始请求
            SwingUtilities.invokeLater(() -> {
                game.getUi().showMessage("message.restart_rejected", "message.title.info", JOptionPane.INFORMATION_MESSAGE);
            });

        } else if (message.startsWith(MSG_PLAYER_INFO)) {
            // 处理玩家信息
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                opponentName = parts[1];
                SwingUtilities.invokeLater(() -> {
                    updatePlayerDisplay();
                });
            }

        } else if (message.startsWith(MSG_GAME_OVER)) {
            // 处理游戏结束
            SwingUtilities.invokeLater(() -> {
                game.setGameOver(true);
            });

        } else if (message.startsWith(MSG_DISCONNECT)) {
            // 处理对手断开连接
            SwingUtilities.invokeLater(() -> {
                game.getUi().showMessage("message.opponent_disconnected", "message.title.info", JOptionPane.INFORMATION_MESSAGE);
                disconnect();
            });
        }
    }

    /**
     * 重新开始游戏
     */
    private void restartGame() {
        // 重置游戏状态
        game.startNewGame();

        // 如果是主机，保持先手；如果是客户端，保持后手
        isMyTurn = isHost;

        // 更新显示
        updateTurnDisplay();

        // 如果主机是后手，需要等待对手先走
        if (isHost && !isMyTurn) {
            // 主机执白，等待对手先走
            myPlayerType = 2;
        } else if (!isHost && isMyTurn) {
            // 客户端执黑，可以先走
            myPlayerType = 1;
        }
    }

    /**
     * 更新回合显示
     */
    public void updateTurnDisplay() {
        SwingUtilities.invokeLater(() -> {
            String turnMessage;
            if (isMyTurn) {
                turnMessage = game.getUi().getMessages().getString("multiplayer.your_turn");
            } else {
                turnMessage = game.getUi().getMessages().getString("multiplayer.opponent_turn");
            }
            game.getUi().updateAiLabel(turnMessage);
        });
    }

    /**
     * 更新玩家显示
     */
    private void updatePlayerDisplay() {
        SwingUtilities.invokeLater(() -> {
            String displayText = String.format("%s (%s) vs %s (%s)",
                    playerName,
                    myPlayerType == 1 ? game.getUi().getMessages().getString("game.black") : game.getUi().getMessages().getString("game.white"),
                    opponentName,
                    myPlayerType == 1 ? game.getUi().getMessages().getString("game.white") : game.getUi().getMessages().getString("game.black"));

            game.setTitle(displayText);
        });
    }

    /**
     * 显示聊天消息
     */
    private void showChatMessage(String message) {
        // 这里可以扩展为显示在专门的聊天区域
        System.out.println("聊天: " + message);
    }

    /**
     * 检查是否是我的回合
     */
    public boolean isMyTurn() {
        return isMyTurn;
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        connected = false;

        if (out != null) {
            sendMessage(MSG_DISCONNECT);
            out.close();
        }

        try {
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        networkExecutor.shutdown();

        // 重置游戏模式
        game.setGameMode(0);
        SwingUtilities.invokeLater(() -> {
            game.getUi().updateAiLabel("AI: IDLE");
            game.setTitle(game.getUi().getMessages().getString("game.title"));
        });
    }

    /**
     * 获取玩家类型
     */
    public int getMyPlayerType() {
        return myPlayerType;
    }

    /**
     * 获取对手名称
     */
    public String getOpponentName() {
        return opponentName;
    }

    /**
     * 获取玩家名称
     */
    public String getPlayerName() {
        return playerName;
    }
}