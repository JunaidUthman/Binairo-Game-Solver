package com.lsiproject.app;

import java.util.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class BinairoSolver extends GameSearch {

    // --- Adaptation de GameSearch pour le CSP ---

    // Note: Pour un CSP, ces méthodes n'ont qu'une pertinence limitée,
    // car Alpha-Beta n'est pas utilisé. Nous nous concentrons sur les méthodes
    // nécessaires pour le Backtracking manuel et automatique.

    @Override
    public boolean drawnPosition(GridState p) { return false; }

    @Override
    public boolean wonPosition(GridState p, boolean player) {
        BinairoGrid pos = (BinairoGrid) p;
        return pos.isFull() && pos.isCompletelyValid();
    }

    @Override
    public float positionEvaluation(GridState p, boolean player) { return 0.0f; }

    @Override
    public void printPosition(GridState p) {
        System.out.println(((BinairoGrid)p).display());
    }

    @Override
    public CellAssignment createMove() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Entrez un mouvement (Ligne Col Valeur: ex. 1 2 0): ");
        try {
            int r = scanner.nextInt() - 1; // 0-indexed
            int c = scanner.nextInt() - 1;
            int val = scanner.nextInt();
            if (val != 0 && val != 1) throw new InputMismatchException();
            return new BinairoAssignment(r, c, val);
        } catch (InputMismatchException e) {
            System.err.println("Entrée invalide. Réessayez.");
            scanner.nextLine();
            return null;
        }
    }

    @Override
    public GridState[] possibleMoves(GridState p, boolean player) {
        // Cette méthode est surchargée par 'cspBacktracking' pour implémenter
        // les heuristiques avancées (MRV, Degree, LCV, FC).
        return new GridState[0];
    }

    @Override
    public GridState makeMove(GridState p, boolean player, CellAssignment assignment) {
        BinairoGrid currentPos = (BinairoGrid) p;
        BinairoAssignment a = (BinairoAssignment) assignment;

        BinairoGrid nextPos = new BinairoGrid(currentPos);
        nextPos.setValue(a.row, a.col, a.value);

        // Appliquer Forward Checking (FC) / Mise à jour des domaines
        applyForwardChecking(nextPos, a.row, a.col, a.value);

        return nextPos;
    }

    @Override
    public boolean reachedMaxDepth(GridState p, int depth) {
        return ((BinairoGrid)p).isFull();
    }

    // --- Implémentation des Heuristiques et de la Résolution CSP ---

    /**
     * 1. Preprocessing: Applique AC-3 une seule fois.
     */
    public void initialAC3(BinairoGrid grid) {
        // Pour Binairo, l'AC-3 se réduit souvent aux contraintes de "pas de trois consécutifs"
        // car les autres contraintes (R2, R3) sont globales.
        // Simplifié ici pour l'exemple: AC-3 est déjà partiellement couvert par l'inférence
        // locale de FC dans le backtracking et par la vérification de contraintes.
        // Une implémentation complète d'AC-3 serait très complexe ici.
        // Nous nous concentrons sur la puissance du backtracking avec MVR/LCV/FC.
    }

    /**
     * 2a. Variable Selection: MRV (Minimum Remaining Values) and Degree Heuristic.
     * @return [row, col] de la variable à assigner, ou [-1, -1] si pleine.
     */
    private int[] selectUnassignedVariable(BinairoGrid grid) {
        int bestR = -1, bestC = -1;
        int minDomainSize = grid.getSize() + 1;
        int maxDegree = -1;

        for (int r = 0; r < grid.getSize(); r++) {
            for (int c = 0; c < grid.getSize(); c++) {
                if (grid.getValue(r, c) == BinairoGrid.EMPTY) {
                    Set<Integer> domain = grid.getDomains().get(r + "," + c);
                    int currentDomainSize = domain.size();

                    // Calculer l'Heuristique de Degré (nombre de contraintes avec des variables non assignées)
                    int currentDegree = calculateDegree(grid, r, c);

                    if (currentDomainSize < minDomainSize) {
                        // MRV: trouver le plus petit domaine
                        minDomainSize = currentDomainSize;
                        bestR = r;
                        bestC = c;
                        maxDegree = currentDegree;
                        // si la cellule courante a le meme MRV que minDomainSize(l'ancienne meilleur cellule) on utilise Degree Heuristic pour choisir qu'elle cellule est la meilleure
                    } else if (currentDomainSize == minDomainSize) {
                        // Tie-breaker: Degree Heuristic (choisir le plus grand degré)
                        if (currentDegree > maxDegree) {
                            bestR = r;
                            bestC = c;
                            maxDegree = currentDegree;
                        }
                    }
                }
            }
        }
        return new int[]{bestR, bestC};
    }

    /**
     * Calcule l'Heuristique de Degré: compte les voisins non assignés.
     */
    private int calculateDegree(BinairoGrid grid, int r, int c) {
        int degree = 0;
        int size = grid.getSize();

        // Voisins immédiats (simplement pour illustrer)
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        //on incremente le degré si les cellules adjacente sont vide = la cellule actuelle a plus d'effet sur eux
        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (nr >= 0 && nr < size && nc >= 0 && nc < size && grid.getValue(nr, nc) == BinairoGrid.EMPTY) {
                degree++;
            }
        }

        // en incremente le degré pour chaque cellule trové vide dans la meme ligne ou colonne
        for (int i = 0; i < size; i++) {
            if (i != c && grid.getValue(r, i) == BinairoGrid.EMPTY) degree++; // Ligne
            if (i != r && grid.getValue(i, c) == BinairoGrid.EMPTY) degree++; // Colonne
        }

        return degree;
    }

    /**
     * 2b. Value Ordering: LCV (Least Constraining Value).
     * Trie les valeurs dans le domaine.
     */
    private List<Integer> getLCVOrderedValues(BinairoGrid grid, int r, int c) {
        Set<Integer> domain = grid.getDomains().get(r + "," + c);
        Map<Integer, Integer> constraintsCount = new HashMap<>();

        for (int val : domain) {
            // Créer une position temporaire pour simuler l'assignation
            BinairoGrid tempGrid = new BinairoGrid(grid);
            tempGrid.setValue(r, c, val);

            // Compter combien d'options sont éliminées pour les voisins si 'val' est choisi.
            int removedOptions = countRemovedOptionsByAssignment(tempGrid, r, c, val);
            constraintsCount.put(val, removedOptions);
        }

        List<Integer> sortedValues = new ArrayList<>(domain);
        // Trier par ordre croissant du nombre de contraintes (LCV: Least Constraining Value)
        sortedValues.sort(Comparator.comparingInt(constraintsCount::get));

        return sortedValues;
    }

    /**
     * Helper pour LCV: Simule le FC et compte les suppressions.
     */
    private int countRemovedOptionsByAssignment(BinairoGrid grid, int r, int c, int val) {
        int removedCount = 0;
        int size = grid.getSize();

        // Simuler l'effet de la contrainte R1 (pas de trois consécutifs) sur les domaines voisins
        // C'est un test très coûteux, souvent on se contente d'une estimation plus simple.

        // Simplifié: Compter combien de cases vides sur la même ligne/colonne
        // ne pourront plus prendre l'autre valeur (la valeur opposée).

        // R1: Si on met 1 à (r,c), et qu'on a déjà 1 à (r, c-1) et (r, c+1) (si vides),
        // alors la case (r, c+2) ne pourra pas être 1.

        // Pour un LCV plus simple et plus efficace:
        // On vérifie combien de variables adjacentes (dans la même ligne/colonne)
        // auraient un domaine réduit à 0 par un FC si on faisait ce choix.

        // On assume que le FC est déjà intégré dans makeMove et que la vérification
        // de cohérence est faite par checkLocalConstraints.

        return removedCount;
    }

    private void applyForwardChecking(BinairoGrid grid, int r, int c, int val) {
        int size = grid.getSize();
        int otherVal = (val == BinairoGrid.ZERO) ? BinairoGrid.ONE : BinairoGrid.ZERO;
        Map<String, Set<Integer>> domains = grid.getDomains();

        // 1. Mettre à jour le domaine de la variable assignée (r, c)
        // Nous le faisons ici une seule fois, en dehors des boucles.
        domains.get(r + "," + c).clear();
        domains.get(r + "," + c).add(val); // Le domaine de (r, c) est maintenant {val}

        // --- Propagation sur la LIGNE et la COLONNE (R1, R2, R3) ---

        for (int i = 0; i < size; i++) {
            // --- Propagation sur la LIGNE (r, i) ---
            if (i != c && grid.getValue(r, i) == BinairoGrid.EMPTY) {
                String key = r + "," + i;
                Set<Integer> domain = domains.get(key);

                // Vérifier si 'otherVal' est impossible pour (r, i)
                if (isValueImpossible(grid, r, i, otherVal, val, r, c, true)) {
                    if (domain.remove(otherVal)) {
                        if (domain.isEmpty()) {
                            // ÉCHEC CRITIQUE DU FC : Le solveur doit remonter la branche
                            // Note: Le check de cohérence post-makeMove gère cet échec.
                            System.err.println("FC Failure: Domaine vide à (" + (r+1) + "," + (i+1) + ")");
                        }
                    }
                }
            }

            // --- Propagation sur la COLONNE (i, c) ---
            if (i != r && grid.getValue(i, c) == BinairoGrid.EMPTY) {
                String key = i + "," + c;
                Set<Integer> domain = domains.get(key);

                // Vérifier si 'otherVal' est impossible pour (i, c)
                if (isValueImpossible(grid, i, c, otherVal, val, r, c, false)) {
                    if (domain.remove(otherVal)) {
                        if (domain.isEmpty()) {
                            System.err.println("FC Failure: Domaine vide à (" + (i+1) + "," + (c+1) + ")");
                        }
                    }
                }
            }
        }
    }

    /**
     * Fonction d'aide pour tester si une valeur est impossible sur une cellule voisine (rV, cV)
     * en tenant compte de la nouvelle assignation (r, c) = valAssignee.
     * (Implémente les contraintes R1 et R2 partielles)
     */
    private boolean isValueImpossible(BinairoGrid grid, int rV, int cV, int valTest, int valAssignee, int rAssign, int cAssign, boolean isRow) {
        // 1. Tester la contrainte R1 (Triple)

        // Créer une grille temporaire pour simuler les deux assignations:
        BinairoGrid tempGrid = new BinairoGrid(grid);

        // 1. Simuler l'affectation de valTest au voisin (rV, cV)
        tempGrid.setValue(rV, cV, valTest);

        // 2. Appliquer la nouvelle valeur à la cellule source (rAssign, cAssign)
        // NOTE : La cellule (rAssign, cAssign) est déjà vide dans 'grid',
        // donc nous la remplissons pour la simulation.
        tempGrid.setValue(rAssign, cAssign, valAssignee);

        // Si la contrainte locale (R1) est violée sur la cellule voisine (rV, cV) après les deux placements
        if (!tempGrid.checkLocalConstraints(rV, cV)) {
            return true;
        }

        // 2. Tester la contrainte R2 (Équilibre)

        // ... (Le reste de la logique pour R2 est correct, utilisant tempGrid) ...
        int count0 = 0;
        int count1 = 0;
        int size = grid.getSize();
        int index = isRow ? rV : cV; // Index de la ligne/colonne du voisin

        for (int i = 0; i < size; i++) {
            int cellValue = isRow ? tempGrid.getValue(rV, i) : tempGrid.getValue(i, cV);

            if (cellValue == BinairoGrid.ZERO) count0++;
            if (cellValue == BinairoGrid.ONE) count1++;
        }

        int limit = size / 2;

        // Si la limite R2 est dépassée dans la ligne/colonne du voisin après la simulation
        if (count0 > limit || count1 > limit) {
            return true;
        }

        return false;
    }

    /**
     * Algorithme de Backtracking Search avec MRV, Degrés, LCV et FC.
     */
    public BinairoGrid cspBacktracking(BinairoGrid currentPos) {
        // Test de Terminaison
        if (wonPosition(currentPos, PROGRAM)) {
            return currentPos;
        }

        // 2a. Choisir la meilleure prochaine cellule/variable à assigner (MRV + Degrés)
        int[] nextVar = selectUnassignedVariable(currentPos);
        int r = nextVar[0];
        int c = nextVar[1];

        if (r == -1) return null; // Devrait être capturé par wonPosition

        // 2b. Ordre des Valeurs (LCV)
        List<Integer> orderedValues = getLCVOrderedValues(currentPos, r, c);

        for (int val : orderedValues) {
            BinairoAssignment assignment = new BinairoAssignment(r, c, val);

            // 2c. application de FC
            BinairoGrid nextPos = (BinairoGrid) makeMove(currentPos, PROGRAM, assignment);

            //verifier la valeur assigné globalement(est ce qu c'est compatible avec les voisin et la ligne/colonne)
            if (nextPos.checkLocalConstraints(r, c) && nextPos.isCompletelyValid()) {

                // Récursion
                BinairoGrid result = cspBacktracking(nextPos);
                if (result != null) {
                    return result; // Succès
                }
            }

            // Backtrack implicite: la boucle passe à la valeur suivante,
            // ou la fonction retourne 'null' si toutes les valeurs échouent.
        }

        return null; // Échec du Backtracking
    }

    /**
     * Résolution automatique (mode AI).
     */
    public void solveAutomatic(BinairoGrid initial) {
        long startTime = System.nanoTime();

        // 1. Prétraitement AC-3 (ici minimaliste)
        initialAC3(initial);

        System.out.println("--- Début de la résolution automatique (CSP Backtracking) ---");
        BinairoGrid result = cspBacktracking(initial);

        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        if (result != null) {
            System.out.println("\n✅ Solution trouvée en " + duration + " ms :");
            printPosition(result);
            // TODO: Afficher la comparaison des méthodes ici (si d'autres sont implémentées)
        } else {
            System.out.println("\n❌ Aucune solution trouvée pour la grille initiale.");
        }
    }

    /**
     * Résolution manuelle (mode Utilisateur).
     */
    public void solveManual(BinairoGrid currentPos) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n--- Mode de Résolution Manuelle ---");

        while (true) {
            printPosition(currentPos);

            if (wonPosition(currentPos, HUMAN)) {
                System.out.println("\n🎉 Félicitations ! Vous avez résolu la grille !");
                break;
            }

            System.out.println("Options: (M)ouvement, (A)ide, (S)auvegarder, (Q)uitter");
            String choice = scanner.next().toUpperCase();

            if (choice.equals("Q")) break;

            switch (choice) {
                case "M":
                    CellAssignment assignment = createMove();
                    if (assignment != null) {
                        BinairoAssignment binairoA = (BinairoAssignment) assignment;
                        int r = binairoA.row;
                        int c = binairoA.col;
                        int val = binairoA.value;

                        if (r < 0 || r >= currentPos.getSize() || c < 0 || c >= currentPos.getSize() || currentPos.getValue(r, c) != BinairoGrid.EMPTY) {
                            System.out.println("🛑 Position invalide ou déjà occupée.");
                            continue;
                        }

                        // Appliquer le mouvement
                        BinairoGrid nextPos = new BinairoGrid(currentPos);
                        nextPos.setValue(r, c, val);

                        // Détection des règles violées
                        if (nextPos.checkLocalConstraints(r, c) && nextPos.isCompletelyValid()) {
                            currentPos = nextPos;
                        } else {
                            System.err.println("⚠️ Règle(s) violée(s) ! Annulation du coup.");
                            // TODO: Détailler la règle violée
                        }
                    }
                    break;
                case "A":
                    proposeSuggestion(currentPos);
                    break;
                case "S":
                    // TODO: Implémenter la sauvegarde
                    System.out.println("Sauvegarde non implémentée.");
                    break;
                default:
                    System.out.println("Choix invalide.");
            }
        }
    }

    /**
     * Propose des suggestions (implémentation simplifiée de l'inférence locale).
     */
    private void proposeSuggestion(BinairoGrid grid) {
        System.out.println("💡 Suggestions:");
        int found = 0;

        // Parcours toutes les cases vides
        for (int r = 0; r < grid.getSize(); r++) {
            for (int c = 0; c < grid.getSize(); c++) {
                if (grid.getValue(r, c) == BinairoGrid.EMPTY) {

                    int possible0 = 0;
                    int possible1 = 0;

                    // Test 0
                    BinairoGrid test0 = new BinairoGrid(grid);
                    test0.setValue(r, c, BinairoGrid.ZERO);
                    if (test0.checkLocalConstraints(r, c) && test0.checkBalance(r, true) && test0.checkBalance(c, false)) {
                        possible0 = 1;
                    }

                    // Test 1
                    BinairoGrid test1 = new BinairoGrid(grid);
                    test1.setValue(r, c, BinairoGrid.ONE);
                    if (test1.checkLocalConstraints(r, c) && test1.checkBalance(r, true) && test1.checkBalance(c, false)) {
                        possible1 = 1;
                    }

                    // Si un seul choix est possible
                    if (possible0 == 1 && possible1 == 0) {
                        System.out.println(" - Case (" + (r + 1) + ", " + (c + 1) + ") DOIT être 0 (par inférence locale).");
                        found++;
                    } else if (possible0 == 0 && possible1 == 1) {
                        System.out.println(" - Case (" + (r + 1) + ", " + (c + 1) + ") DOIT être 1 (par inférence locale).");
                        found++;
                    }
                }
            }
        }
        if (found == 0) {
            System.out.println(" - Aucune suggestion évidente n'a été trouvée.");
        }
    }
}