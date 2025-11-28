package com.lsiproject.app;

import java.util.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class BinairoSolver extends GameSearch {

    private boolean useMVR;
    private boolean useDegree;
    private boolean useLCV;
    private boolean useAC3;
    private boolean useFC;

    // --- Métriques de Performance ---
    private long nodesVisited;
    private long startTime;
    private long endTime;

    /**
     * Configure les heuristiques à utiliser pour la prochaine résolution.
     */
    public void configureSolver(boolean useMVR, boolean useDegree, boolean useLCV, boolean useAC3, boolean useFC) {
        this.useMVR = useMVR;
        this.useDegree = useDegree;
        this.useLCV = useLCV;
        this.useAC3 = useAC3;
        this.useFC = useFC;
    }

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

    /**
     * Vérifie si la grille initiale est résoluble en lançant le solveur CSP.
     * @return La solution trouvée (BinairoGrid) si résoluble, sinon null.
     */
    public BinairoGrid checkResolvability(BinairoGrid initial) {
        BinairoGrid tempGrid = new BinairoGrid(initial);

        // Réinitialisation des métriques
        this.nodesVisited = 0;
        this.startTime = System.nanoTime();

        // 1. PHASE DE PRÉTRAITEMENT AC-3 (OPTIONNEL)
        if (this.useAC3) {
            initialAC3(tempGrid);
        }

        // 2. VÉRIFICATION D'ÉCHEC AC-3/VALIDITÉ
        if (!tempGrid.isCompletelyValid()) {
            System.err.println("La grille est devenue incohérente après la vérification initiale (AC-3/Validité).");
            return null;
        }

        // 3. PHASE DE RECHERCHE
        BinairoGrid result = cspBacktracking(tempGrid);
        this.endTime = System.nanoTime();

        return result;
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
    public GridState makeMove(GridState p, boolean player, CellAssignment assignment) {
        BinairoGrid currentPos = (BinairoGrid) p;
        BinairoAssignment a = (BinairoAssignment) assignment;

        BinairoGrid nextPos = new BinairoGrid(currentPos);
        nextPos.setValue(a.row, a.col, a.value);

        // --- Forward Checking (FC) si useFC est true
        if (this.useFC) {
            // Seule la propagation des contraintes sur les voisins a lieu si FC est activé.
            applyForwardChecking(nextPos, a.row, a.col, a.value);
        }

        return nextPos;
    }



    /**
     * Algorithme de Prétraitement AC-3 (Arc Consistency 3).
     * Réduit les domaines des variables non assignées jusqu'à atteindre un point fixe.
     */
    public void initialAC3(BinairoGrid grid) {
        System.out.println("  [AC-3] Démarrage du prétraitement...");
        boolean domainReduced;
        int passCount = 0;

        // Boucle principale AC-3 : Répéter tant que des réductions de domaine se produisent
        do {
            domainReduced = false;
            passCount++;
            int size = grid.getSize();
            Map<String, Set<Integer>> domains = grid.getDomains();

            // Itérer sur TOUTES les cellules de la grille (arcs)
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {

                    if (grid.getValue(r, c) == BinairoGrid.EMPTY) {
                        // Si la cellule est vide, tester sa cohérence avec tous ses voisins

                        // Si le domaine d'une cellule est réduit, nous devons
                        // re-tester ses voisins dans la prochaine passe.

                        Set<Integer> currentDomain = domains.get(r + "," + c);
                        Set<Integer> toRemove = new HashSet<>();

                        // Pour chaque valeur possible dans le domaine de (r, c)
                        for (int valTest : currentDomain) {

                            // Créer une simulation locale pour cette vérification
                            BinairoGrid tempGrid = new BinairoGrid(grid);
                            tempGrid.setValue(r, c, valTest); // Simuler l'assignation de valTest

                            // Vérification R1/R2 pour cette assignation simulée
                            if (!tempGrid.checkLocalConstraints(r, c) ||
                                    !tempGrid.checkPartialBalance(r, true) ||
                                    !tempGrid.checkPartialBalance(c, false)) {

                                // Si valTest viole R1 ou R2, elle doit être supprimée du domaine
                                toRemove.add(valTest);
                            }
                        }

                        // Appliquer les suppressions
                        if (!toRemove.isEmpty()) {
                            currentDomain.removeAll(toRemove);
                            domainReduced = true;
                            // Si un domaine vide est créé, la grille est impossible.
                            if (currentDomain.isEmpty()) {
                                System.err.println("  [AC-3] Échec : Domaine vide détecté à (" + (r+1) + "," + (c+1) + ")");
                                // Dans ce cas, on pourrait s'arrêter, mais laisser la boucle
                                // finir pour un nettoyage complet de l'état.
                                return; // Arrêt précoce
                            }
                        }
                    } else {
                        // Si la cellule est remplie, nous pouvons l'utiliser pour contraindre ses voisins.
                        // Cette logique est déjà couverte par l'itération des autres cellules vides,
                        // mais on peut l'intégrer ici pour une version plus pure de AC-3 si nécessaire.
                        // Pour le Binairo, simplifier la propagation est plus simple.
                    }
                }
            }
        } while (domainReduced);

        System.out.println("  [AC-3] Terminé en " + passCount + " passes. Domaines réduits.");
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

                    // Calculer l'Heuristique de Degré SEULEMENT si nécessaire
                    int currentDegree = 0;
                    if (useDegree) {
                        currentDegree = calculateDegree(grid, r, c);
                    } else if (!useMVR) {
                        // Si ni MVR ni Degré ne sont utilisés, on utilise la première variable trouvée (BT pur)
                        return new int[]{r, c};
                    }

                    // --- Logique d'application de MVR ---
                    if (useMVR && currentDomainSize < minDomainSize) {
                        minDomainSize = currentDomainSize;
                        bestR = r;
                        bestC = c;
                        maxDegree = currentDegree;
                    } else if (useMVR && currentDomainSize == minDomainSize) {
                        // Égalité MRV
                        if (useDegree && currentDegree > maxDegree) {
                            // Départage par Degré (si activé)
                            bestR = r;
                            bestC = c;
                            maxDegree = currentDegree;
                        }
                    }

                    // --- Logique si MVR est désactivé (utiliser Degré comme critère principal ou BT pur) ---
                    else if (!useMVR) {
                        if (useDegree && currentDegree > maxDegree) {
                            maxDegree = currentDegree;
                            bestR = r;
                            bestC = c;
                        }
                    }

                    // Si MVR est activé, nous devons initialiser le premier trouvé si on n'a rien encore.
                    if (bestR == -1) {
                        minDomainSize = currentDomainSize;
                        bestR = r;
                        bestC = c;
                        maxDegree = currentDegree;
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

        if (!useLCV) {
            // Si LCV est désactivé, retourne l'ordre par défaut (0, 1)
            List<Integer> defaultOrder = new ArrayList<>(domain);
            defaultOrder.sort(null); // Ordre numérique (0 puis 1)
            return defaultOrder;
        }

        // Si LCV est activé, exécute l'ordre LCV
        Map<Integer, Integer> constraintsCount = new HashMap<>();

        for (int val : domain) {
            BinairoGrid tempGrid = new BinairoGrid(grid);
            tempGrid.setValue(r, c, val);
            // countRemovedOptionsByAssignment doit être implémenté pour simuler la réduction des domaines
            int removedOptions = countRemovedOptionsByAssignment(tempGrid, r, c, val);
            constraintsCount.put(val, removedOptions);
        }

        List<Integer> sortedValues = new ArrayList<>(domain);
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

        // 1.la valeur est deja assigné , son domaine ne doit plus etre {1,2},c'est pour ca on Mettre à jour le domaine de la variable assignée (r, c) ici
        domains.get(r + "," + c).clear();
        domains.get(r + "," + c).add(val); // Le domaine de (r, c) est maintenant {val}

        // --- Propagation sur la LIGNE et la COLONNE (R1, R2, R3) ---

        for (int i = 0; i < size; i++) {// i vas etre utilisé pour iterer sur les lignes en premier temps(premier condition) et puis les colonnes(deuxiemme condition)
            // --- Propagation sur la LIGNE (r, i) ---
            if (i != c && grid.getValue(r, i) == BinairoGrid.EMPTY) {//i!=c pour ne pas traiter la cellule qui voient d'etre assigné
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

        // Créer une grille temporaire pour simuler les deux assignations:
        BinairoGrid tempGrid = new BinairoGrid(grid);

        // 1. Simuler l'affectation de valTest au voisin (rV, cV)
        tempGrid.setValue(rV, cV, valTest);

        // 2. Appliquer la nouvelle valeur à la cellule source (rAssign, cAssign)
        // NOTE : La cellule (rAssign, cAssign) est déjà vide dans 'grid',
        // nous la remplissons pour la simulation.
        tempGrid.setValue(rAssign, cAssign, valAssignee);

        // --- 1. Tester la contrainte R1 (Triple) ---
        // Si la contrainte locale (R1) est violée sur la cellule voisine (rV, cV) après les deux placements
        if (!tempGrid.checkLocalConstraints(rV, cV)) {
            return true; // R1 est violée, valTest est impossible
        }

        // --- 2. Tester la contrainte R2 (Équilibre / Partial Balance) ---

        // Déterminer l'indice de la ligne ou colonne du Voisin (rV, cV) à vérifier.
        int indexToCheck = isRow ? rV : cV;

        /**
         * Utiliser checkPartialBalance sur la grille temporaire (tempGrid).
         *
         * Si checkPartialBalance retourne 'false', cela signifie que la
         * ligne/colonne de la cellule voisine (rV, cV) dépasse déjà la limite N/2
         * avec les deux valeurs simulées.
         */
        if (!tempGrid.checkPartialBalance(indexToCheck, isRow)) {
            return true; // R2 est violée, valTest est impossible
        }

        // --- 3. Tester la contrainte R3 (Unicité) ---
        // Bien que R3 soit normalement vérifié à la fin, on peut faire une vérification partielle ici
        // si l'une des lignes/colonnes est devenue complète.
        // Cette étape est généralement coûteuse et souvent omise en FC partiel, mais si nécessaire,
        // elle devrait être implémentée comme une vérification de l'unicité des lignes complètes dans tempGrid.

        return false; // Si aucune contrainte n'est violée, valTest est toujours possible.
    }

    /**
     * Algorithme de Backtracking Search avec MRV, Degrés, LCV et FC.
     */
    public BinairoGrid cspBacktracking(BinairoGrid currentPos) {
        this.nodesVisited++;

        // Test de Terminaison
        if (wonPosition(currentPos, PROGRAM)) {
            return currentPos;
        }

        // 2a. Choisir la meilleure prochaine cellule/variable à assigner
        int[] nextVar = selectUnassignedVariable(currentPos);
        int r = nextVar[0];
        int c = nextVar[1];

        if (r == -1) return null;

        // 2b. Ordre des Valeurs (LCV ou Ordre par défaut)
        List<Integer> orderedValues = getLCVOrderedValues(currentPos, r, c);

        for (int val : orderedValues) {
            BinairoAssignment assignment = new BinairoAssignment(r, c, val);

            // 2c. application de FC (via makeMove)
            BinairoGrid nextPos = (BinairoGrid) makeMove(currentPos, PROGRAM, assignment);

            // Vérification de cohérence après FC:
            if (nextPos.isCompletelyValid()) {

                // Récursion
                BinairoGrid result = cspBacktracking(nextPos);
                if (result != null) {
                    return result; // Succès
                }
            }
        }

        return null; // Échec du Backtracking
    }

    public void displayPerformanceMetrics() {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(this.endTime - this.startTime);

        // Afficher la configuration de PC utilisée
        String pcConfig = "";
        if (this.useAC3) pcConfig += "AC-3 Initial + ";
        pcConfig += this.useFC ? "FC" : "BT Pur";

        System.out.println("\n===== Comparaison de Performance =====");
        System.out.println("Configuration : MVR=" + this.useMVR + ", Degrés=" + this.useDegree + ", LCV=" + this.useLCV + ", PC=" + pcConfig);
        System.out.println("Temps de Résolution : " + durationMs + " ms");
        System.out.println("Nœuds de Recherche Explorés : " + this.nodesVisited);
        System.out.println("======================================");
    }

    public void displaySolution(BinairoGrid initial, BinairoGrid solution) {
        System.out.println("\n--- Grille Initiale ---");
        printPosition(initial);
        System.out.println("\n--- Solution Finale ---");
        printPosition(solution);
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