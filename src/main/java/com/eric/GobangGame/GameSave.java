package com.eric.GobangGame;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Inner class to represent game save data
 */
public class GameSave implements Serializable {
	private static final long serialVersionUID = 1L;

	private int[][] board;
	private boolean isBlackTurn;
	private boolean gameOver;
	private List<int[]> moveHistory;
    private int gameMode;
    private boolean playerIsBlack;
    private int aiDifficulty;

	public GameSave(int[][] board, boolean isBlackTurn, boolean gameOver, List<int[]> moveHistory, 
                    int gameMode, boolean playerIsBlack, int aiDifficulty) {
		this.board = copyBoard(board);
		this.isBlackTurn = isBlackTurn;
		this.gameOver = gameOver;
		this.moveHistory = new ArrayList<>(moveHistory); // Create a copy
        this.gameMode = gameMode;
        this.playerIsBlack = playerIsBlack;
        this.aiDifficulty = aiDifficulty;
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
    public int getGameMode() { return gameMode; }
    public boolean isPlayerIsBlack() { return playerIsBlack; }
    public int getAiDifficulty() { return aiDifficulty; }
}