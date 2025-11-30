package com.lsiproject.app;

import java.util.Random;
import java.util.Scanner;

public class Main {

    private static Scanner scanner = new Scanner(System.in);
    private static BinairoSolver solver = new BinairoSolver();

    public static void main(String[] args) {
        System.out.println("Bienvenue dans le jeu Binairo (Takuzu/Binero)");

        while (true) {
            System.out.println("\n===== Menu Principal =====");
            System.out.println("1. Résolution Manuelle (Jouer par l'utilisateur)");
            System.out.println("2. Résolution Automatique (par l'AI)");
            System.out.println("3. Quitter");
            System.out.print("Votre choix: ");

            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        handleHumanPlay();
                        break;
                    case 2:
                        handleAISolve();
                        break;
                    case 3:
                        System.out.println("Au revoir !");
                        return;
                    default:
                        System.out.println("Choix invalide.");
                }
            } else {
                System.out.println("❌ Entrée invalide.");
                scanner.nextLine();
            }
        }
    }

    /**
     * Gère le flux pour la résolution par l'utilisateur.
     * La grille doit être validée et résoluble avant de commencer le jeu manuel.
     */
    private static void handleHumanPlay() {
        System.out.println("\n--- Mode Résolution Manuelle Utilisateur ---");
        int size = promptForGridSize();
        if (size == -1) return;

        solver.configureSolver(false, false, false, false,false, false);
        GridResolution resolution = promptForGridCreation(size);

        if (resolution.isResolvable()) {
            System.out.println("\n🎉 La grille est résoluble. Vous pouvez commencer à jouer.");
            solver.solveManual(resolution.getInitialGrid());
        } else {
            System.err.println("\n🛑 La grille générée n'est pas possible à résoudre. Veuillez réessayer avec une autre grille.");
        }
    }

    /**
     * Gère le flux pour la résolution automatique par l'AI.
     * La grille est créée, validée, résolue et affichée.
     */
    private static void handleAISolve() {
        System.out.println("\n--- Mode Résolution Automatique AI ---");
        int size = promptForGridSize();
        if (size == -1) return;

        solver.configureSolver(true, true, true, true,true, true);
        GridResolution resolution = promptForGridCreation(size);

        if (resolution.isResolvable()) {
            System.out.println("\n✅ Grille valide et résoluble. Affichage de la solution AI :");
            // Si c'est résoluble, on a déjà la solution stockée (optimisation)
            solver.displaySolution(resolution.getInitialGrid(), resolution.getSolution());
            solver.displayPerformanceMetrics();
        } else {
            System.err.println("\n🛑 La grille n'est pas résoluble. L'AI ne peut pas trouver de solution.");
        }
    }

    /**
     * Demande la taille de la grille et valide que c'est un nombre pair >= 4.
     * @return La taille valide ou -1 si l'utilisateur annule.
     */
    private static int promptForGridSize() {
        int size = 0;
        boolean validSize = false;
        while (!validSize) {
            System.out.print("Entrez la taille de la grille (un nombre PAIR >= 4): ");
            if (scanner.hasNextInt()) {
                size = scanner.nextInt();
                scanner.nextLine();
                if (size % 2 == 0 && size >= 4) {
                    validSize = true;
                } else {
                    System.out.println("❌ Dimension invalide. Doit être PAIR et >= 4.");
                }
            } else {
                System.out.println("❌ Entrée invalide. Veuillez entrer un nombre.");
                scanner.nextLine();
                return -1;
            }
        }
        return size;
    }

    /**
     * Gère le choix du mode de création de la grille et sa validation de résolubilité.
     * @return Un objet GridResolution contenant la grille initiale et la solution (si résoluble).
     */
    private static GridResolution promptForGridCreation(int size) {
        BinairoGrid grid = null;

        while (true) {
            System.out.println("\nComment créer la grille initiale ?");
            System.out.println("1. Manuelle (entrer les indices)");
            System.out.println("2. Aléatoire (basique)");
            System.out.print("Votre choix: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Choix invalide.");
                scanner.nextLine();
                continue;
            }
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1: grid = createManualGrid(size); break;
                case 2: grid = createRandomGrid(size); break;
                default: System.out.println("Choix non reconnu."); continue;
            }

            if (grid != null) {
                System.out.println("\n--- Grille Initiale ---");
                solver.printPosition(grid);

                // --- VÉRIFICATION DE LA VALIDITÉ ET DE LA RÉSOLUBILITÉ ---
                System.out.println("Vérification de la résolubilité (lancement du solveur CSP)...");
                // On appelle le solveur une seule fois pour la
                BinairoGrid solution = solver.checkResolvability(grid);

                if (solution != null) {
                    return new GridResolution(grid, solution);
                } else {
                    System.err.println("❌ Le solveur n'a trouvé AUCUNE solution. Veuillez choisir une autre grille.");
                }
            }
        }
    }

    // --- Fonctions de Création de Grille ---

    private static BinairoGrid createManualGrid(int size) {
        BinairoGrid grid = new BinairoGrid(size);
        System.out.println("\n--- Mode Création Manuelle ---");
        System.out.println("Entrez les indices au format 'Ligne Colonne Valeur' (ex: 1 2 0). Tapez 'FIN' pour terminer.");
        solver.printPosition(grid);

        while (true) {
            System.out.print("Indice (L C V ou FIN): ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("FIN")) break;

            try (Scanner lineScanner = new Scanner(line)) {
                int r = lineScanner.nextInt() - 1;
                int c = lineScanner.nextInt() - 1;
                int v = lineScanner.nextInt();

                if (r < 0 || r >= size || c < 0 || c >= size || (v != 0 && v != 1)) {
                    System.out.println("Coordonnées ou valeur invalides.");
                    continue;
                }
                grid.setValue(r, c, v);
            } catch (Exception e) {
                System.out.println("Format invalide. Réessayez.");
            }
            solver.printPosition(grid);
        }
        return grid;
    }

    private static BinairoGrid createRandomGrid(int size) {
        BinairoGrid grid = new BinairoGrid(size);
        Random rand = new Random();
        int numIndices = size * size / 5;

        for (int i = 0; i < numIndices; i++) {
            int r = rand.nextInt(size);
            int c = rand.nextInt(size);
            int v = rand.nextInt(2); // 0 ou 1

            // Placer la valeur seulement si elle est cohérente localement pour éviter les échecs triviaux
            if (grid.getValue(r, c) == BinairoGrid.EMPTY) {
                grid.setValue(r, c, v);
                // Si la placement viole R1, on l'enlève
                if (!grid.checkLocalConstraints(r, c)) {
                    grid.setValue(r, c, BinairoGrid.EMPTY);
                }
            }
        }
        System.out.println("\n--- Grille Aléatoire Générée ---");
        solver.printPosition(grid);
        return grid;
    }
}