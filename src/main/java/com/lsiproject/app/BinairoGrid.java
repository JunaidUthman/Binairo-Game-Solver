package com.lsiproject.app;

import java.util.*;
import java.io.Serializable;

/**
 * Représente la grille de Binairo et gère les contraintes.
 */
public class BinairoGrid extends GridState implements Serializable {
    private int size;
    private int[][] board; // -1: EMPTY, 0: ZERO, 1: ONE

    // Pour les techniques CSP: suit les domaines (valeurs possibles)
    private Map<String, Set<Integer>> domains;

    public static final int EMPTY = -1;
    public static final int ZERO = 0;
    public static final int ONE = 1;

    public BinairoGrid(int size) {
        this.size = size;
        this.board = new int[size][size];
        this.domains = new HashMap<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = EMPTY;
                domains.put(i + "," + j, new HashSet<>(Arrays.asList(ZERO, ONE)));
            }
        }
    }

    // Constructeur de copie profond (crucial pour le Backtracking)
    public BinairoGrid(BinairoGrid other) {
        this.size = other.size;
        this.board = new int[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(other.board[i], 0, this.board[i], 0, size);
        }
        // Copie des domaines (deep copy)
        this.domains = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> entry : other.domains.entrySet()) {
            this.domains.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
    }

    // --- Méthodes d'accès ---

    public int getSize() { return size; }
    public int getValue(int r, int c) { return board[r][c]; }
    public void setValue(int r, int c, int value) { board[r][c] = value; }
    public Map<String, Set<Integer>> getDomains() { return domains; }

    // --- Vérification d'état ---

    public boolean isFull() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == EMPTY) return false;
            }
        }
        return true;
    }

    /**
     * Vérifie si la grille entière est valide.
     */
    public boolean isCompletelyValid() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!checkLocalConstraints(i, j)) return false; // R1
            }
            if (isRowFull(i)) {
                if (!checkBalance(i, true)) return false; // R2 (Ligne)
                if (!checkDuplicateRow(i)) return false; // R3 (Ligne)
            }
            if (isColFull(i)) {
                if (!checkBalance(i, false)) return false; // R2 (Colonne)
                if (!checkDuplicateCol(i)) return false; // R3 (Colonne)
            }
        }
        // R3 doit être vérifié sur toutes les paires de lignes/colonnes complètes.
        return true;
    }

    // --- Vérification des contraintes (Règles du jeu) ---

    /**
     * R1: Maximum deux chiffres identiques côte à côte.
     * Vérifie autour de (r, c) sur la ligne et la colonne.
     * @return true si la règle n'est pas violée localement.
     */
    public boolean checkLocalConstraints(int r, int c) {
        if (board[r][c] == EMPTY) return true; // Rien à vérifier pour une case vide

        int val = board[r][c];

        // Vérification de la Ligne
        // Check r, c-2, c-1, c
        if (c >= 2 && board[r][c-1] == val && board[r][c-2] == val) return false;
        // Check r, c-1, c, c+1
        if (c >= 1 && c <= size - 2 && board[r][c-1] == val && board[r][c+1] == val) return false;
        // Check r, c, c+1, c+2
        if (c <= size - 3 && board[r][c+1] == val && board[r][c+2] == val) return false;

        // Vérification de la Colonne
        // Check r-2, r-1, r, c
        if (r >= 2 && board[r-1][c] == val && board[r-2][c] == val) return false;
        // Check r-1, r, r+1, c
        if (r >= 1 && r <= size - 2 && board[r-1][c] == val && board[r+1][c] == val) return false;
        // Check r, r+1, r+2, c
        if (r <= size - 3 && board[r+1][c] == val && board[r+2][c] == val) return false;

        return true;
    }

    /**
     * R2: Égalité parfaite (paires) ou différence d'une unité (impaires).
     * Vérifié uniquement si la ligne/colonne est pleine.
     */
    public boolean checkBalance(int index, boolean isRow) {
        int count0 = 0;
        int count1 = 0;
        int target = size / 2;

        for (int i = 0; i < size; i++) {
            int val = isRow ? board[index][i] : board[i][index];
            if (val == ZERO) count0++;
            else if (val == ONE) count1++;
        }

        // Si la ligne/colonne n'est pas pleine, la contrainte n'est pas violée
        if (count0 + count1 < size) {
            return count0 <= target && count1 <= target; // Vérifie la limite MAX
        }

        // Si pleine, vérifier la Règle R2
        if (size % 2 == 0) { // Grille paire
            return count0 == target && count1 == target;
        } else { // Grille impaire (pas standard pour Binairo, mais géré ici)
            return (count0 == target && count1 == target + 1) ||
                    (count0 == target + 1 && count1 == target);
        }
    }

    /**
     * R3: Aucune ligne/colonne ne peut être identique à une autre ligne/colonne complète.
     * Vérifié uniquement sur les lignes/colonnes complètes.
     */
    public boolean checkDuplicateRow(int r) {
        if (!isRowFull(r)) return true;

        for (int otherR = 0; otherR < size; otherR++) {
            if (otherR == r || !isRowFull(otherR)) continue;

            boolean duplicate = true;
            for (int c = 0; c < size; c++) {
                if (board[r][c] != board[otherR][c]) {
                    duplicate = false;
                    break;
                }
            }
            if (duplicate) return false;
        }
        return true;
    }

    public boolean checkDuplicateCol(int c) {
        if (!isColFull(c)) return true;

        for (int otherC = 0; otherC < size; otherC++) {
            if (otherC == c || !isColFull(otherC)) continue;

            boolean duplicate = true;
            for (int r = 0; r < size; r++) {
                if (board[r][c] != board[r][otherC]) {
                    duplicate = false;
                    break;
                }
            }
            if (duplicate) return false;
        }
        return true;
    }

    // --- Utilitaires de contraintes ---

    public boolean isRowFull(int r) {
        for (int c = 0; c < size; c++) {
            if (board[r][c] == EMPTY) return false;
        }
        return true;
    }

    public boolean isColFull(int c) {
        for (int r = 0; r < size; r++) {
            if (board[r][c] == EMPTY) return false;
        }
        return true;
    }

    // --- Affichage ---

    public String display() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int j = 0; j < size; j++) sb.append(String.format(" %-2d", j + 1));
        sb.append("\n");
        sb.append("  ").append("-".repeat(size * 3 + 1)).append("\n");

        for (int i = 0; i < size; i++) {
            sb.append(String.format("%-2d|", i + 1));
            for (int j = 0; j < size; j++) {
                int val = board[i][j];
                String cell = val == EMPTY ? "." : String.valueOf(val);
                sb.append(String.format(" %-2s", cell));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}