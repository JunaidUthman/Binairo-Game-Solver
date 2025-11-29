package com.lsiproject.app;


import javax.swing.*;
import java.awt.*;
import java.util.Scanner;
import java.util.stream.IntStream;

public class BinairoGUI extends JFrame {

    private static final BinairoSolver solver = new BinairoSolver();
    private JPanel gridPanel;
    private JLabel statusLabel;
    private BinairoGrid currentGrid;
    private int gridSize = 6;
    private boolean isManualMode = false;
    // Variable pour stocker l'état initial avant toute résolution ou jeu
    private BinairoGrid initialDisplayedGrid = null;

    // Éléments de configuration
    private JCheckBox mvrCheck;
    private JCheckBox degreeCheck;
    private JCheckBox lcvCheck;
    private JCheckBox ac3Check;
    private JCheckBox fcCheck;
    private JRadioButton humanPlayRadio;
    private JRadioButton aiSolveRadio;

    public BinairoGUI() {
        setTitle("Jeu Binairo (Takuzu) - Résolution CSP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Utiliser un FlowLayout pour garantir une taille minimale si la grille est petite
        setLayout(new BorderLayout(10, 10));

        // Initialisation des éléments de l'UI
        statusLabel = new JLabel("Bienvenue ! Configurez la résolution et cliquez sur 'Démarrer'.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

        setupConfigurationPanel();
        setupGridPanel();

        add(statusLabel, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);

        // Appel à pack() UNIQUEMENT ici, pour la première fois.
        pack();

        // Définir une taille minimale pour éviter qu'elle ne devienne trop petite plus tard.
        setMinimumSize(new Dimension(800, 600));

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Configure le panneau de configuration pour les options et les heuristiques.
     */
    private void setupConfigurationPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration et Démarrage"));

        // 1. Choix du mode (Manuel vs AI)
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup modeGroup = new ButtonGroup();
        humanPlayRadio = new JRadioButton("Résolution Manuelle");
        aiSolveRadio = new JRadioButton("Résolution AI / Comparaison");
        modeGroup.add(humanPlayRadio);
        modeGroup.add(aiSolveRadio);
        humanPlayRadio.setSelected(true);
        modePanel.add(new JLabel("Mode de Jeu: "));
        modePanel.add(humanPlayRadio);
        modePanel.add(aiSolveRadio);
        configPanel.add(modePanel);

        // 2. Choix de la taille (simplifié ici, pourrait être un JSpinner)
        JComboBox<Integer> sizeSelector = new JComboBox<>(new Integer[]{4, 6, 8, 10, 12});
        sizeSelector.setSelectedItem(gridSize);
        sizeSelector.addActionListener(e -> {
            // Mise à jour de la taille de la grille de départ
            gridSize = (int) sizeSelector.getSelectedItem();
            resetGrid();
        });
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sizePanel.add(new JLabel("Taille de la Grille:"));
        sizePanel.add(sizeSelector);
        configPanel.add(sizePanel);

        // 3. Panneau des Heuristiques (pour le mode AI / Validation)
        JPanel heuristicPanel = new JPanel(new GridLayout(0, 2));
        heuristicPanel.setBorder(BorderFactory.createTitledBorder("Heuristiques pour AI/Validation"));

        mvrCheck = new JCheckBox("MVR (Variable la plus Contrainte)", true);
        degreeCheck = new JCheckBox("Degrés (Départage)", true);
        lcvCheck = new JCheckBox("LCV (Valeur la moins Contraignante)", true);
        ac3Check = new JCheckBox("AC-3 (Prétraitement)", true);
        fcCheck = new JCheckBox("FC (Forward Checking)", true);

        heuristicPanel.add(mvrCheck);
        heuristicPanel.add(degreeCheck);
        heuristicPanel.add(lcvCheck);
        heuristicPanel.add(ac3Check);
        heuristicPanel.add(fcCheck);
        configPanel.add(heuristicPanel);

        // 4. Boutons d'Action (Démarrage et Création)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton startButton = new JButton("1. Démarrer la Résolution");
        startButton.addActionListener(e -> startResolutionFlow());

        JButton manualInitButton = new JButton("2. Création Manuelle");
        manualInitButton.addActionListener(e -> setupManualInput());

        JButton exampleButton = new JButton("3. Grille d'Exemple");
        exampleButton.addActionListener(e -> loadExampleGrid());

        JButton helpButton = new JButton("4. Aide / Suggestion");
        helpButton.addActionListener(e -> proposeSuggestion());

        actionPanel.add(startButton);
        actionPanel.add(manualInitButton);
        actionPanel.add(exampleButton);
        actionPanel.add(helpButton);

        configPanel.add(actionPanel);

        // 5. Panneau de Sauvegarde/Chargement (Nouveau)
        JPanel saveLoadPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        saveLoadPanel.setBorder(BorderFactory.createTitledBorder("Sauvegarde/Reprise"));

        JButton saveButton = new JButton("Sauvegarder la Partie");
        saveButton.addActionListener(e -> handleSaveGame());

        JButton loadButton = new JButton("Charger une Partie");
        loadButton.addActionListener(e -> handleLoadGame());

        saveLoadPanel.add(saveButton);
        saveLoadPanel.add(loadButton);

        configPanel.add(saveLoadPanel);

        add(configPanel, BorderLayout.EAST);
    }

    /**
     * Initialise le panneau de la grille.
     */
    private void setupGridPanel() {
        gridPanel = new JPanel();
        gridPanel.setBorder(BorderFactory.createTitledBorder("Grille Binairo"));
        resetGrid();
    }

    /**
     * Réinitialise la grille logique et le panneau d'affichage.
     */
    private void resetGrid() {
        // Crée une nouvelle grille vide de la taille actuelle
        currentGrid = new BinairoGrid(gridSize);
        initialDisplayedGrid = currentGrid; // L'état initial est la grille vide
        displayGrid(currentGrid, false);
    }

    /**
     * Charge une grille d'exemple (pour 6x6)
     */
    private void loadExampleGrid() {
        BinairoGrid newGrid = new BinairoGrid(gridSize);

        if (gridSize == 6 || gridSize == 8 || gridSize == 10) {

            // Logique de chargement des exemples
            if (gridSize == 6) {
                newGrid.setValue(0, 0, 1); newGrid.setValue(0, 3, 0);
                newGrid.setValue(1, 1, 0); newGrid.setValue(1, 5, 1);
                newGrid.setValue(2, 4, 0); newGrid.setValue(2, 5, 1);
                newGrid.setValue(3, 0, 0); newGrid.setValue(3, 2, 1);
                newGrid.setValue(4, 3, 0); newGrid.setValue(4, 5, 0);
                newGrid.setValue(5, 1, 1); newGrid.setValue(5, 5, 0);
            } else if (gridSize == 8) {
                newGrid.setValue(0, 2, 1); newGrid.setValue(0, 4, 0);
                newGrid.setValue(1, 1, 0); newGrid.setValue(1, 6, 1);
                newGrid.setValue(2, 0, 1); newGrid.setValue(2, 5, 0);
                newGrid.setValue(3, 3, 0); newGrid.setValue(3, 7, 1);
                newGrid.setValue(4, 0, 0); newGrid.setValue(4, 4, 1);
                newGrid.setValue(5, 2, 0); newGrid.setValue(5, 7, 1);
                newGrid.setValue(6, 1, 1); newGrid.setValue(6, 6, 0);
                newGrid.setValue(7, 3, 1); newGrid.setValue(7, 5, 0);
            } else if (gridSize == 10) {
                // Pour la concision, seulement quelques indices 10x10
                newGrid.setValue(0, 3, 1); newGrid.setValue(0, 7, 0);
                newGrid.setValue(1, 1, 0); newGrid.setValue(1, 5, 1); newGrid.setValue(1, 9, 0);
                newGrid.setValue(2, 0, 1); newGrid.setValue(2, 4, 0); newGrid.setValue(2, 8, 1);
                newGrid.setValue(3, 2, 0); newGrid.setValue(3, 6, 1);
                newGrid.setValue(4, 1, 1); newGrid.setValue(4, 5, 0); newGrid.setValue(4, 9, 1);
                newGrid.setValue(5, 0, 0); newGrid.setValue(5, 4, 1); newGrid.setValue(5, 8, 0);
                newGrid.setValue(6, 2, 1); newGrid.setValue(6, 6, 0);
                newGrid.setValue(7, 1, 0); newGrid.setValue(7, 5, 1); newGrid.setValue(7, 9, 1);
                newGrid.setValue(8, 0, 1); newGrid.setValue(8, 4, 0); newGrid.setValue(8, 8, 1);
                newGrid.setValue(9, 2, 0); newGrid.setValue(9, 6, 1);
            }

            statusLabel.setText("Grille d'exemple " + gridSize + "x" + gridSize + " chargée.");
        } else {
            JOptionPane.showMessageDialog(this, "Aucun exemple prédéfini pour cette taille. Grille vide chargée.");
        }
        currentGrid = newGrid;
        initialDisplayedGrid = new BinairoGrid(newGrid); // Stocke la copie de l'état initial
        // Assurer que le nouveau 'gridSize' est utilisé
        this.gridSize = newGrid.getSize();
        displayGrid(currentGrid, true);
    }

    /**
     * Demande à l'utilisateur d'entrer manuellement les indices pour initialiser la grille.
     */
    private void setupManualInput() {
        String input = JOptionPane.showInputDialog(this,
                "Entrez les indices au format 'Ligne Colonne Valeur' séparés par des espaces (ex: 1 1 0 1 2 1 2 3 0...):",
                "Initialisation Manuelle", JOptionPane.QUESTION_MESSAGE);

        if (input == null || input.trim().isEmpty()) return;

        resetGrid();

        try (Scanner s = new Scanner(input)) {
            while (s.hasNextInt()) {
                int r = s.nextInt() - 1;
                int c = s.nextInt() - 1;
                int v = s.nextInt();

                if (r >= 0 && r < gridSize && c >= 0 && c < gridSize && (v == 0 || v == 1)) {
                    currentGrid.setValue(r, c, v);
                } else {
                    statusLabel.setText("Attention: Ignoré l'indice (" + (r+1) + "," + (c+1) + ") invalide.");
                }
            }
            statusLabel.setText("Grille initialisée manuellement.");
            initialDisplayedGrid = new BinairoGrid(currentGrid); // Stocke l'état initial après l'entrée
            displayGrid(currentGrid, true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur de format dans l'entrée manuelle.", "Erreur", JOptionPane.ERROR_MESSAGE);
            resetGrid();
        }
    }

    /**
     * Gère la sauvegarde de la partie actuelle.
     */
    private void handleSaveGame() {
        if (currentGrid == null) {
            JOptionPane.showMessageDialog(this, "Aucune partie en cours à sauvegarder.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String fileName = solver.saveGame(currentGrid);

        if (fileName != null) {
            JOptionPane.showMessageDialog(this, "Partie sauvegardée sous: " + fileName, "Sauvegarde Réussie", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Partie sauvegardée.");
        } else {
            JOptionPane.showMessageDialog(this, "Échec de la sauvegarde. Vérifiez les permissions.", "Erreur de Sauvegarde", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Affiche la liste des parties sauvegardées et permet le chargement.
     */
    private void handleLoadGame() {
        String[] savedGames = solver.listSavedGames();

        if (savedGames == null || savedGames.length == 0) {
            JOptionPane.showMessageDialog(this, "Aucune partie sauvegardée trouvée.", "Erreur", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Utiliser JOptionPane pour la sélection
        String selectedFile = (String) JOptionPane.showInputDialog(this,
                "Choisissez la partie à reprendre :",
                "Charger une Partie",
                JOptionPane.QUESTION_MESSAGE,
                null,
                savedGames,
                savedGames[0]);

        if (selectedFile != null) {
            BinairoGrid loadedGrid = solver.loadGame(selectedFile);

            if (loadedGrid != null) {
                // Mettre à jour l'état de la GUI
                // CRITICAL FIX: Met à jour la variable de classe gridSize avant d'appeler displayGrid
                this.gridSize = loadedGrid.getSize();
                currentGrid = loadedGrid;
                initialDisplayedGrid = new BinairoGrid(loadedGrid); // L'état chargé est le nouvel état initial
                isManualMode = true; // Une partie chargée est toujours en mode manuel par défaut
                displayGrid(currentGrid, true);

                statusLabel.setText("Partie chargée : " + selectedFile);
                JOptionPane.showMessageDialog(this, "Partie chargée avec succès. Vous êtes en mode manuel.", "Chargement Réussi", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Échec du chargement du fichier.", "Erreur de Chargement", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Démarre le flux principal de résolution (Validation -> AI ou Manuel).
     */
    private void startResolutionFlow() {
        // CORRECTION MAJEURE: Si la grille initiale n'a jamais été créée (au premier lancement), initialDisplayedGrid est null.
        if (initialDisplayedGrid == null) {
            JOptionPane.showMessageDialog(this, "Veuillez d'abord charger une grille (Manuelle, Exemple ou Chargement).", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // CLONAGE CRUCIAL: Utiliser l'état initial stocké pour le solveur.
        BinairoGrid initialGridState = new BinairoGrid(initialDisplayedGrid);

        // 1. Définir la configuration pour la VALIDATION
        boolean v_mvr = mvrCheck.isSelected();
        boolean v_deg = degreeCheck.isSelected();
        boolean v_lcv = lcvCheck.isSelected();
        boolean v_ac3 = ac3Check.isSelected();
        boolean v_fc = fcCheck.isSelected();

        // La validation utilise la meilleure config pour garantir la détection de la résolubilité.
        solver.configureSolver(v_mvr, v_deg, v_lcv, v_ac3, v_fc);

        statusLabel.setText("Validation de la résolubilité...");

        // Le solveur travaille sur une COPIE de initialGridState, donc currentGrid n'est pas modifié.
        GridResolution resolution = new GridResolution(initialGridState, solver.checkResolvability(initialGridState));

        if (!resolution.isResolvable()) {
            statusLabel.setText("🛑 ÉCHEC: La grille actuelle n'est PAS résoluble.");
            // Afficher la grille initiale non résoluble
            displayGrid(initialGridState, true);
            return;
        }

        // 2. Lancement du mode choisi
        if (aiSolveRadio.isSelected()) {
            handleAISolveGUI(resolution);
        } else {
            handleHumanPlayGUI(resolution);
        }
    }

    /**
     * Gère la résolution manuelle graphique.
     */
    private void handleHumanPlayGUI(GridResolution resolution) {
        isManualMode = true;
        // La grille pour le jeu manuel doit être une nouvelle copie de l'état initial (non résolu)
        currentGrid = new BinairoGrid(resolution.getInitialGrid());
        // Mise à jour de l'état initial affiché (au cas où l'utilisateur veut recommencer plus tard)
        initialDisplayedGrid = new BinairoGrid(currentGrid);

        // Mise à jour CRITIQUE de la taille de la grille affichée
        this.gridSize = currentGrid.getSize();

        statusLabel.setText("Mode Manuel: Cliquez sur une case vide pour changer sa valeur (0 ou 1).");
        displayGrid(currentGrid, true);
    }

    /**
     * Gère l'affichage de la solution AI et la comparaison de performance.
     */
    private void handleAISolveGUI(GridResolution resolution) {
        isManualMode = false;

        // Afficher la solution trouvée lors de la validation
        currentGrid = resolution.getSolution();

        // Mise à jour CRITIQUE de la taille de la grille affichée
        this.gridSize = currentGrid.getSize();

        displayGrid(currentGrid, false);

        // Rétablissement de la grille initiale après l'affichage de la solution pour un nouveau lancement.
        initialDisplayedGrid = new BinairoGrid(resolution.getInitialGrid());

        // Afficher les métriques de la configuration de validation dans une boîte de dialogue
        String metrics = "<html>" + solver.getPerformanceMetrics() + "</html>";
        JOptionPane.showMessageDialog(this, metrics, "Résultat de la Résolution AI", JOptionPane.INFORMATION_MESSAGE);

        statusLabel.setText("Solution AI trouvée et métriques affichées.");
    }

    /**
     * Tente de trouver la prochaine case ayant une seule option possible (Inférence locale).
     */
    private void proposeSuggestion() {
        if (!isManualMode || currentGrid.isFull()) {
            JOptionPane.showMessageDialog(this, "L'aide n'est disponible qu'en mode manuel ou la grille est déjà complète.", "Aide non disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Utiliser la meilleure configuration d'inférence (MVR, FC) pour trouver la suggestion.
        solver.configureSolver(true, false, false, false, true);

        // Le solveur a besoin d'une méthode pour retourner une assignation simple (r, c, val)
        BinairoAssignment suggestion = solver.getInferenceSuggestion(currentGrid);

        if (suggestion != null) {
            JOptionPane.showMessageDialog(this,
                    "Suggestion: Placez " + suggestion.value + " à la position (" + (suggestion.row + 1) + "," + (suggestion.col + 1) + ")",
                    "Aide Trouvée", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Aide: Suggestion trouvée en (" + (suggestion.row + 1) + "," + (suggestion.col + 1) + ")");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Aucune inférence simple n'est disponible pour le moment. Essayez d'analyser les contraintes.",
                    "Aide Non Trouvée", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Aide: Aucune suggestion simple.");
        }
    }

    /**
     * Dessine la grille dans le panneau.
     */
    private void displayGrid(BinairoGrid grid, boolean isInteractive) {

        // FIX CRITICAL: Utiliser la taille réelle de la grille passée, et non la variable de classe.
        int actualSize = grid.getSize();

        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(actualSize, actualSize));

        for (int r = 0; r < actualSize; r++) {
            for (int c = 0; c < actualSize; c++) {
                int val = grid.getValue(r, c);
                JButton cellButton = new JButton(val == BinairoGrid.EMPTY ? "" : String.valueOf(val));
                cellButton.setFont(new Font("Arial", Font.BOLD, 20));

                if (val == BinairoGrid.EMPTY && isInteractive && isManualMode) {
                    // Rendre les cases vides cliquables en mode manuel
                    cellButton.setBackground(Color.LIGHT_GRAY);
                    cellButton.addActionListener(new CellClickListener(r, c));
                } else if (val != BinairoGrid.EMPTY) {
                    // Style des indices remplis
                    cellButton.setBackground(val == 0 ? new Color(220, 240, 255) : new Color(255, 230, 230));
                }

                gridPanel.add(cellButton);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
        // pack() a été retiré ici pour maintenir une taille de fenêtre fixe/minimale.
    }

    /**
     * Détermine la raison spécifique de l'erreur.
     */
    private String getSpecificValidationError(BinairoGrid nextGrid, int r, int c) {

        // --- 1. Vérification R1 (Triple) ---
        if (!nextGrid.checkLocalConstraints(r, c)) {
            return "R1 (Triple): Maximum deux chiffres identiques côte à côte.";
        }

        // --- 2. Vérification R2 Partielle (Limite de N/2) ---
        if (!nextGrid.checkPartialBalance(r, true)) {
            return "R2 (Équilibre Ligne): Le nombre de 0 ou 1 dépasse déjà N/2.";
        }
        if (!nextGrid.checkPartialBalance(c, false)) {
            return "R2 (Équilibre Colonne): Le nombre de 0 ou 1 dépasse déjà N/2.";
        }

        // --- 3. Vérification Finale R2 et R3 (Unicité) ---
        if (nextGrid.isRowFull(r)) {
            if (!nextGrid.checkBalance(r, true)) {
                return "R2 (Équilibre Final Ligne): La ligne doit contenir N/2 de chaque chiffre.";
            }
            if (!nextGrid.checkDuplicateRow(r)) {
                return "R3 (Unicité Ligne): La ligne est identique à une autre ligne complétée.";
            }
        }

        if (nextGrid.isColFull(c)) {
            if (!nextGrid.checkBalance(c, false)) {
                return "R2 (Équilibre Final Colonne): La colonne doit contenir N/2 de chaque chiffre.";
            }
            if (!nextGrid.checkDuplicateCol(c)) {
                return "R3 (Unicité Colonne): La colonne est identique à une autre colonne complétée.";
            }
        }

        // --- 4. Vérification de cohérence générale
        if (!nextGrid.isCompletelyValid()) {
            return "Générale: L'état de la grille devient incohérent (erreur de domaine ou contrainte non locale).";
        }

        return null;
    }

    /**
     * Écouteur pour les clics de cellule en mode manuel.
     */
    private class CellClickListener extends AbstractAction {
        private final int r, c;

        public CellClickListener(int r, int c) {
            this.r = r;
            this.c = c;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (!isManualMode || currentGrid.getValue(r, c) != BinairoGrid.EMPTY) return;

            // Demander la valeur
            String input = JOptionPane.showInputDialog(BinairoGUI.this,
                    "Entrez 0 ou 1 pour la case (" + (r + 1) + "," + (c + 1) + "):", "Entrée Manuelle",
                    JOptionPane.QUESTION_MESSAGE);

            if (input != null && (input.equals("0") || input.equals("1"))) {
                int newVal = Integer.parseInt(input);

                // Créer une nouvelle grille pour tester le mouvement
                BinairoGrid nextGrid = new BinairoGrid(currentGrid);
                nextGrid.setValue(r, c, newVal);

                // Vérification des règles après le coup (R1, R2 partielle, etc.)
                String errorDescription = getSpecificValidationError(nextGrid, r, c);

                if (errorDescription == null) {
                    // Succès : le coup est valide
                    currentGrid = nextGrid;
                    displayGrid(currentGrid, true);

                    if (currentGrid.isFull() && currentGrid.isCompletelyValid()) {
                        statusLabel.setText("🎉 GAGNÉ! La grille est résolue et valide!");
                        isManualMode = false;
                    } else {
                        statusLabel.setText("Coup valide : (" + (r+1) + "," + (c+1) + ") = " + newVal);
                    }
                } else {
                    // Échec : le coup viole une règle spécifique
                    JOptionPane.showMessageDialog(BinairoGUI.this,
                            "Coup invalide! " + errorDescription,
                            "Erreur de Règle", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Coup invalide à (" + (r+1) + "," + (c+1) + "). Réessayez.");
                }
            }
        }
    }

    public static void main(String[] args) {
        // Définir le look and feel du système pour une meilleure intégration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new BinairoGUI();
    }
}