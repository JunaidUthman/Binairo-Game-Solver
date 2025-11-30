package com.lsiproject.app;

/**
 * Classe Abstraite de Base pour les Algorithmes de Recherche.
 * Adaptée de l'exemple initial pour utiliser GridState et CellAssignment.
 */
public abstract class CSPSearchBase {

    public static final boolean DEBUG = false;

    // Définitions des joueurs/agents (utilisées pour distinguer les rôles)
    public static boolean PROGRAM = false; // L'Agent Solveur (Backtracking)
    public static boolean HUMAN = true;    // L'Utilisateur

    // --- Méthodes Abstraites à Implémenter ---

    public abstract boolean wonPosition(GridState p, boolean player);
    public abstract void printPosition(GridState p);
    public abstract GridState makeMove(GridState p, boolean player, CellAssignment assignment);
    public abstract CellAssignment createMove();

}