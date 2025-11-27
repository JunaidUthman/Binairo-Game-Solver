package com.lsiproject.app;

/**
 * Représente l'action de placer un 0 ou un 1 à une position donnée.
 */
public class BinairoAssignment extends CellAssignment {
    public final int row;
    public final int col;
    public final int value; // 0 ou 1

    public BinairoAssignment(int row, int col, int value) {
        this.row = row;
        this.col = col;
        this.value = value;
    }

    @Override
    public String toString() {
        return "(" + (row + 1) + ", " + (col + 1) + ") = " + value;
    }
}