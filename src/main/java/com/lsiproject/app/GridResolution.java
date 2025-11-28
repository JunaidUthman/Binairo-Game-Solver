package com.lsiproject.app;

/**
 * Conteneur pour le résultat de la création et de la vérification de la résolubilité.
 */
public class GridResolution {
    private final BinairoGrid initialGrid;
    private final BinairoGrid solution; // Null si non résoluble

    public GridResolution(BinairoGrid initialGrid, BinairoGrid solution) {
        this.initialGrid = initialGrid;
        this.solution = solution;
    }

    public BinairoGrid getInitialGrid() { return initialGrid; }
    public BinairoGrid getSolution() { return solution; }
    public boolean isResolvable() { return solution != null; }
}
