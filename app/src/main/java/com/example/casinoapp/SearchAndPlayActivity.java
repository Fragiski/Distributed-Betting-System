package com.example.casinoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class SearchAndPlayActivity extends AppCompatActivity {

    // UI Elements
    private Spinner spinnerBetCategory;
    private Spinner spinnerRisk;
    private EditText editStars;
    private GridView gameGridView;
    private LinearLayout searchOptionsLayout;
    private String playerName;
    private ProgressBar searchProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_and_play);

        // 1. Αρχικοποίηση UI στοιχείων
        initUI();

        setupFooterButtons();

        // 2.Λήψη δεδομένων παίκτη από το Intent
        playerName = getIntent().getStringExtra("playerName");

        SharedPreferences prefs = getSharedPreferences("CasinoPrefs", MODE_PRIVATE);
        playerName = prefs.getString("playerName", "Guest"); //"Guest" προεπιλογή αν κάτι πάει στραβά

        // 3. Setup Spinners με προεπιλεγμένες τιμές
        setupSpinners();

        // 4. Set Listeners για τα νέα κουμπιά
        setupListeners();

        // ΑΥΤΟΜΑΤΟ ΦΟΡΤΩΜΑ: Καλούμε άμεσα την εμφάνιση των παιχνιδιών κατά την είσοδο
        viewAllGames();

    }

    private void initUI() {
        // Σύνδεση των Spinners και του EditText από το νέο XML
        spinnerBetCategory = findViewById(R.id.spinnerCategory);
        spinnerRisk = findViewById(R.id.spinnerRisk);
        editStars = findViewById(R.id.editStars);

        // Σύνδεση του GridView και του ProgressBar
        gameGridView = findViewById(R.id.gameGridView);
        searchProgressBar = findViewById(R.id.searchProgressBar);

        // Σύνδεση του Glass Panel των φίλτρων που ανοιγοκλείνει
        searchOptionsLayout = findViewById(R.id.searchOptionsLayout);
    }

    // LISTENERS
    private void setupListeners() {

        // Φίλτρα
        View btnToggleFilters = findViewById(R.id.btnToggleFilters);

        if (btnToggleFilters != null && searchOptionsLayout != null) {
            btnToggleFilters.setOnClickListener(v -> animateAndNavigate(v, () -> {
                // Αν είναι ορατό το κλείνουμε, αν είναι κλειστό το ανοίγουμε
                if (searchOptionsLayout.getVisibility() == View.VISIBLE) {
                    searchOptionsLayout.setVisibility(View.GONE);
                } else {
                    searchOptionsLayout.setVisibility(View.VISIBLE);
                }
            }));
        }

        // Κουμπί APPLY FILTERS μέσα στο panel
        View btnApplyFilters = findViewById(R.id.btnApplyFilters);
        if (btnApplyFilters != null) {
            btnApplyFilters.setOnClickListener(v -> {
                String starsStr = editStars.getText().toString().trim();

                // ΑΝ ΕΙΝΑΙ ΑΔΕΙΟ: Βάζουμε αυτόματα -1 για να μην φιλτράρει με βάση τα αστέρια
                int starsValue = starsStr.isEmpty() ? -1 : Integer.parseInt(starsStr);

                if (gameGridView != null) {
                    gameGridView.setAdapter(null);
                }

                String category = spinnerBetCategory.getSelectedItem().toString();
                String risk = spinnerRisk.getSelectedItem().toString();

                // Εκτέλεση αναζήτησης με τη διορθωμένη τιμή
                performSearch(category, starsValue, risk);

                // Κλείνουμε αυτόματα το panel μετά την αναζήτηση
                if (searchOptionsLayout != null) {
                    searchOptionsLayout.setVisibility(View.GONE);
                }
            });
        }

        // ΚΟΥΜΠΙ ΚΑΘΑΡΙΣΜΟΥ (RESET FILTERS)
        View btnClearFilters = findViewById(R.id.btnClearFilters);
        if (btnClearFilters != null) {
            btnClearFilters.setOnClickListener(v -> {
                // 1. Επαναφέρουμε τα Spinners στο "ALL"
                if (spinnerBetCategory != null) spinnerBetCategory.setSelection(0);
                if (spinnerRisk != null) spinnerRisk.setSelection(0);

                // 2. Αδειάζουμε το πεδίο των αστεριών
                if (editStars != null) editStars.setText("");

                // 3. Ζητάμε από τον Master να φέρει όλα τα παιχνίδια
                viewAllGames();

                // 4. Κλείνουμε το panel
                if (searchOptionsLayout != null) {
                    searchOptionsLayout.setVisibility(View.GONE);
                }
            });
        }

        // Κλικ πάνω σε παιχνίδι για έναρξη
        // Λογική κλικ στα παιχνίδια του GridView για την προβολή αναλυτικών στοιχείων
        gameGridView.setOnItemClickListener((parent, view1, position, id) -> {
            Game selectedGame = (Game) parent.getItemAtPosition(position);

            if (selectedGame != null) {

                animateAndNavigate(view1, () -> {
                android.app.Dialog dialog = new android.app.Dialog(this);
                dialog.setContentView(R.layout.dialog_game_details);

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                    // Αναγκάζουμε το Dialog να γίνει φαρδύ και να μην συμπιέζεται
                    dialog.getWindow().setLayout(
                            (int) (getResources().getDisplayMetrics().widthPixels * 0.90), // 90% του πλάτους της οθόνης
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                }

                // Σύνδεση UI στοιχείων του Dialog
                TextView dialogName = dialog.findViewById(R.id.dialogGameName);
                TextView dialogLimits = dialog.findViewById(R.id.dialogLimits);
                TextView dialogRisk = dialog.findViewById(R.id.dialogRisk);
                TextView dialogJackpot = dialog.findViewById(R.id.dialogJackpot);
                ImageView dialogIcon = dialog.findViewById(R.id.dialogGameIcon);
                View btnClose = dialog.findViewById(R.id.btnDialogClose);
                View btnPlay = dialog.findViewById(R.id.btnDialogPlay);
                View btnReview = dialog.findViewById(R.id.btnDialogReview);

                if (btnReview != null) {
                    btnReview.setOnClickListener(v -> animateAndNavigate(v, () -> {
                        dialog.dismiss(); // Κλείνουμε το info dialog
                        showReviewDialog(selectedGame.getGameName()); // Ανοίγουμε το dialog της αξιολόγησης
                    }));
                }

                // Τροφοδοσία δεδομένων
                dialogName.setText(selectedGame.getGameName());
                dialogLimits.setText(selectedGame.getMinBet() + " - " + selectedGame.getMaxBet() + " FUN");
                dialogRisk.setText(selectedGame.getRiskLevel());
                dialogJackpot.setText(selectedGame.getJackpot() + " FUN");

                if (selectedGame.getRiskLevel().equalsIgnoreCase("High")) {
                    dialogRisk.setTextColor(android.graphics.Color.parseColor("#FF4D4D"));
                } else {
                    dialogRisk.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                }

                String imgName = "imggameicon"; // Default τιμή τα κεράσια

                if (selectedGame.getGameName() != null) {
                    String gameNameCheck = selectedGame.getGameName().toLowerCase().trim();

                    // Χειροκίνητο mapping για όσα παιχνίδια έχουν ιδιαίτερα ονόματα ή prefixes
                    if (gameNameCheck.contains("starburst")) {
                        imgName = "img_starburst";
                    } else if (gameNameCheck.contains("gates") && gameNameCheck.contains("olympus")) {
                        imgName = "gates_of_olympus";
                    } else if (gameNameCheck.contains("cyber") && gameNameCheck.contains("punks")) {
                        imgName = "img_cyberpunksslots";
                    } else if (gameNameCheck.contains("fruit")) {
                        imgName = "img_fruit_bonanza";
                    } else if (gameNameCheck.contains("gonzo")) {
                        imgName = "img_gonzos_quest";
                    } else if (gameNameCheck.contains("thunder")) {
                        imgName = "img_olympus_thunder";
                    } else if (gameNameCheck.contains("pharaoh")) {
                        imgName = "img_pharaohs_treasure";
                    } else if (gameNameCheck.contains("sweet") && gameNameCheck.contains("bonanza")) {
                        imgName = "img_sweet_bonanza";
                    } else {
                        // Για οποιοδήποτε άλλο μελλοντικό παιχνίδι, αντικαθιστούμε τα κενά με κάτω παύλες
                        imgName = gameNameCheck.replaceAll(" ", "_");
                    }
                }

                int resId = getResources().getIdentifier(imgName, "drawable", getPackageName());

                if (resId != 0 && dialogIcon != null) {
                    dialogIcon.setImageResource(resId);
                } else if (dialogIcon != null) {
                    dialogIcon.setImageResource(R.drawable.imggameicon); // Fallback αν κάτι πάει στραβά
                }

                    if (btnClose != null) {
                        btnClose.setOnClickListener(v -> animateAndNavigate(v, () -> dialog.dismiss()));
                    }

                if (btnPlay != null) {
                    btnPlay.setOnClickListener(v -> animateAndNavigate(v, () -> {
                        dialog.dismiss();
                        Intent intent = new Intent(this, GamePlayActivity.class);
                        intent.putExtra("CHOSEN_GAME", selectedGame);
                        intent.putExtra("playerName", playerName);
                        startActivity(intent);
                    }));
                }

                dialog.show();
                });
            }
        });


    }


    /**
     * Professional network task method to communicate with Master
     */
    private void performNetworkTask(String command, Object payload) {
        // Show loading before Thread starts
        runOnUiThread(() -> searchProgressBar.setVisibility(View.VISIBLE));

        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // Send protocol command
                out.writeObject(command);

                // Send payload if exists
                if (payload != null) {
                    out.writeObject(payload);
                }
                out.flush();

                // Expecting results (either List<Game> or ReducerResult)
                Object response = in.readObject();
                List<Game> games;

                if (response instanceof ReducerResult) {
                    games = ((ReducerResult) response).getFinalGames();
                } else {
                    games = (List<Game>) response;
                }

                runOnUiThread(() -> {
                    updateGameList(games);
                    searchProgressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connection Error", Toast.LENGTH_SHORT).show();
                    //Hide loading if error occurs
                    searchProgressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    /**
     * Retrieves all available games by sending universal filters
     * compatible with the Master's Search logic.
     */
    private void viewAllGames() {
        // According to ConsoleClient_3, "ALL" and -1 stars
        // triggers a return of all active games.
        SearchFilters allFilters = new SearchFilters("ALL", -1, "ALL");


        // We use the same PLAYER_SEARCH label as the console client
        performNetworkTask("PLAYER_SEARCH", allFilters);
    }


    /**
     * Performs a filtered search based on user selection.
     */
    private void performSearch(String category, int stars, String risk) {
        // Create the filters object based on UI input
        SearchFilters filters = new SearchFilters(category, stars, risk);


        // Send the search request to the Master
        performNetworkTask("PLAYER_SEARCH", filters);
    }

    private void updateGameList(List<Game> games) {
        if (searchProgressBar != null) {
            searchProgressBar.setVisibility(View.GONE);
        }

        if (games == null || games.isEmpty()) {
            gameGridView.setVisibility(View.GONE);
            Toast.makeText(this, "Δεν βρέθηκαν παιχνίδια", Toast.LENGTH_LONG).show();
            return;
        }

        GameAdapter adapter = new GameAdapter(this, games);
        gameGridView.setAdapter(adapter);
        gameGridView.setVisibility(View.VISIBLE);
    }

    /**
     * Εμφανίζει το custom παράθυρο για να αφήσει review ο παίκτης.
     */
    private void showReviewDialog(String gameName) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);

        android.app.AlertDialog reviewDialog = builder.create();
        if (reviewDialog.getWindow() != null) {
            reviewDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView txtTitle = dialogView.findViewById(R.id.txtReviewTitle);
        if (txtTitle != null) txtTitle.setText("Rate: " + gameName);

        RatingBar ratingBar = dialogView.findViewById(R.id.dialogRatingBar);
        EditText editComment = dialogView.findViewById(R.id.editReviewComment);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelReview);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitReview);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> animateAndNavigate(v, () -> {
                reviewDialog.dismiss();
            }));
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> animateAndNavigate(v, () -> {
                int stars = (int) ratingBar.getRating();
                String comment = editComment.getText().toString().trim();

                if (stars == 0) {
                    Toast.makeText(this, "Παρακαλώ επιλέξτε τουλάχιστον 1 αστέρι!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Δημιουργία του request αντικειμένου για το Review
                ReviewRequest reviewRequest = new ReviewRequest(playerName, gameName, stars, comment);

                // Αποστολή στον Master Server με το πρωτόκολλο "PLAYER_REVIEW"
                performNetworkTask("PLAYER_REVIEW", reviewRequest);

                Toast.makeText(this, "Η αξιολόγηση στάλθηκε!", Toast.LENGTH_SHORT).show();
                reviewDialog.dismiss();
            }));
        }

        reviewDialog.show();
    }

    //HELPER METHODS

    private void setupSpinners() {
        // Price categories
        String[] prices = {"ALL", "$", "$$", "$$$"};
        spinnerBetCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, prices));

        // Risk levels
        String[] risks = {"ALL", "Low", "Medium", "High"};
        spinnerRisk.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, risks));
    }

    // --- FOOTER NAVIGATION LISTENERS ---
    private void setupFooterButtons() {

        // Κουμπί Lobby (navLobby)
        View navLobby = findViewById(R.id.navLobby);
        if (navLobby != null) {
            navLobby.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerLoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Κουμπί Games (navGames)
        View navGames = findViewById(R.id.navGames);
        if (navGames != null) {
            navGames.setOnClickListener(v -> {
                // Αν είμαστε ήδη στην SearchAndPlay, απλά κλείνουμε τα φίλτρα αν είναι ανοιχτά
                if (this instanceof SearchAndPlayActivity) {
                    if (searchOptionsLayout != null) {
                        searchOptionsLayout.setVisibility(View.GONE);
                    }
                } else {
                    Intent intent = new Intent(this, SearchAndPlayActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("playerName", playerName);
                    startActivity(intent);
                }
            });
        }

        // Κουμπί Settings (navSettings)
        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("playerName", playerName);
                startActivity(intent);
            });
        }

        // Κουμπί Account (navAccount) - Logout
        View navAccount = findViewById(R.id.navAccount);
        if (navAccount != null) {
            navAccount.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerProfile.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("playerName", playerName);
                startActivity(intent);
            });
        }
    }

    // Animation button
    private void animateAndNavigate(View view, Runnable action) {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.button_click);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                action.run();
            }
        });
        view.startAnimation(anim);
    }
}