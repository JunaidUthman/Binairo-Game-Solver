package com.lsiproject.app;

import java.util.Scanner;

public class Main {

    private static Scanner scanner = new Scanner(System.in);
    private static BinairoSolver solver = new BinairoSolver();

    public static void main(String[] args) {

        System.out.println("Bienvenue dans le jeu Binairo (Takuzu/Binero)");

        while (true) {
            System.out.println("\n===== Menu Principal =====");
            System.out.println("1. Résolution Manuelle (Jouer)");
            System.out.println("2. Résolution Automatique (AI)");
            System.out.println("3. Quitter");
            System.out.print("Votre choix: ");

            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        startGame(false);
                        break;
                    case 2:
                        startGame(true);
                        break;
                    case 3:
                        System.out.println("Au revoir !");
                        return;
                    default:
                        System.out.println("Choix invalide.");
                }
            } else {
                System.out.println("Entrée invalide.");
                scanner.nextLine();
            }
        }
    }

    private static void startGame(boolean autoResolve) {
        System.out.print("Entrez la taille de la grille (ex: 6 pour 6x6): ");
        if (!scanner.hasNextInt()) {
            System.out.println("Taille invalide.");
            scanner.nextLine();
            return;
        }
        int size = scanner.nextInt();
        scanner.nextLine();

        if (size < 4 || size % 2 != 0) {
            System.out.println("Taille non standard. Recommandé: 4, 6, 8, 10, etc. (nombre pair >= 4).");
        }

        BinairoGrid initialGrid = createGrid(size);

        if (autoResolve) {
            solver.solveAutomatic(initialGrid);
        } else {
            solver.solveManual(initialGrid);
        }
    }

    private static BinairoGrid createGrid(int size) {
        BinairoGrid grid = new BinairoGrid(size);

            System.out.println("Pour le moment , la grille vas etre vide a l'état initial");


        return grid;
    }
}