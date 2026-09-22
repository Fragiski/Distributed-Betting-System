package com.example.casinoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.casinoapp.PlayRequest;
import com.example.casinoapp.R;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class GamePlayActivity extends AppCompatActivity {
    private String gameName, playerName;
    private TextView txtBetGameName, tvResult;
    private EditText edtBetAmount;
    private double playerBalance;

    // Δήλωση των νέων διαδραστικών κουμπιών
    private Button btnChip10, btnChip50, btnChip100;
    private Button btnBetMin, btnBetDouble, btnBetMax;
    private Button btnPlaceBet;
    private Button btnBack;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_play);

        initUI();

        // 1. ΛΗΨΗ ΔΕΔΟΜΕΝΩΝ: Ανάκτηση του Serializable Game αντικειμένου
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CHOSEN_GAME")) {
            Game chosenGame = (Game) intent.getSerializableExtra("CHOSEN_GAME");
            if (chosenGame != null) {
                gameName = chosenGame.getGameName();
            }
        } else {
            // Fallback αν κάτι πάει λαθος στη μεταφορά
            gameName = intent != null ? intent.getStringExtra("gameName") : "Unknown Game";
        }

        // Λήψη του ονόματος παίκτη
        playerName = intent != null ? intent.getStringExtra("playerName") : "Player1";
        if (playerName == null) playerName = "Player1";


        if (txtBetGameName != null && gameName != null) {
            txtBetGameName.setText("Playing: " + gameName);
        } else if (txtBetGameName != null) {
            txtBetGameName.setText("Playing: Starburst"); // Fallback
        }

        setupListeners();

        fetchActualBalance();
    }

    private void initUI() {
        // Σύνδεση με τα νέα IDs του στρωμένου XML
        txtBetGameName = findViewById(R.id.txtBetGameName);

        edtBetAmount = findViewById(R.id.edtBetAmount);
        tvResult = findViewById(R.id.tvResult); // Για την εμφάνιση των αποτελεσμάτων του Master

        // Αρχικοποίηση των νέων διαδραστικών στοιχείων
        btnChip10 = findViewById(R.id.btnChip10);
        btnChip50 = findViewById(R.id.btnChip50);
        btnChip100 = findViewById(R.id.btnChip100);

        btnBetMin = findViewById(R.id.btnBetMin);
        btnBetDouble = findViewById(R.id.btnBetDouble);
        btnBetMax = findViewById(R.id.btnBetMax);

        btnPlaceBet = findViewById(R.id.btnPlaceBet);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupListeners() {
        // --- 1. ΜΑΡΚΕΣ ---
        setAnimatedClick(R.id.btnChip10, () -> addAmountToBet(10));
        setAnimatedClick(R.id.btnChip50, () -> addAmountToBet(50));
        setAnimatedClick(R.id.btnChip100, () -> addAmountToBet(100));

        // --- 2. ΚΟΥΜΠΙΑ ΣΤΡΑΤΗΓΙΚΗΣ ---

        // MIN
        setAnimatedClick(R.id.btnBetMin, () -> edtBetAmount.setText("10"));

        // X2 (Double)
        setAnimatedClick(R.id.btnBetDouble, () -> {
            int currentBet = getEnteredBetAsInt();
            int doubledBet = currentBet * 2;
            if (doubledBet <= playerBalance) {
                edtBetAmount.setText(String.valueOf(doubledBet));
            } else {
                edtBetAmount.setText(String.valueOf(playerBalance));
            }
        });

        // ALL IN (MAX)
        setAnimatedClick(R.id.btnBetMax, () -> {
            double currentBalance = GamePlayActivity.this.playerBalance;

            if (currentBalance <= 0.0) {
                Toast.makeText(this, "Δεν έχετε διαθέσιμα FUN για All In! (Υπόλοιπο: " + currentBalance + ")", Toast.LENGTH_SHORT).show();
                edtBetAmount.setText("0");
                return;
            }

            double maxTableLimit = 1000.0;
            double finalBet;

            if (currentBalance > maxTableLimit) {
                finalBet = maxTableLimit;
                Toast.makeText(this, "All In στο μέγιστο όριο του παιχνιδιού: " + maxTableLimit + " FUN", Toast.LENGTH_SHORT).show();
            } else {
                finalBet = currentBalance;
                Toast.makeText(this, "All In: " + finalBet + " FUN!", Toast.LENGTH_SHORT).show();
            }

            edtBetAmount.setText(String.valueOf(finalBet));
        });

        // --- 3. ΚΕΝΤΡΙΚΟ ΚΟΥΜΠΙ: PLACE BET ---
        setAnimatedClick(R.id.btnPlaceBet, () -> {
            String betStr = edtBetAmount.getText().toString().trim();
            if (!betStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(betStr);
                    if (amount <= 0) {
                        Toast.makeText(this, "Please enter a positive amount", Toast.LENGTH_SHORT).show();
                    } else if (amount > playerBalance) {
                        Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_SHORT).show();
                    } else {
                        sendPlayRequest(amount);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter bet amount", Toast.LENGTH_SHORT).show();
            }
        });

        // RETURN TO LOBBY
        setAnimatedClick(R.id.btnBack, () -> {
            Intent intent = new Intent(this, SearchAndPlayActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }


    // Βοηθητική μέθοδος για την προσθήκη των μαρκών
    private void addAmountToBet(int amount) {
        int currentBet = getEnteredBetAsInt();
        int newBet = currentBet + amount;

        if (newBet <= playerBalance) {
            edtBetAmount.setText(String.valueOf(newBet));
        } else {
            edtBetAmount.setText(String.valueOf(playerBalance));
        }
    }

    // Ασφαλής ανάγνωση του EditText ως ακέραιος για τους υπολογισμούς των Buttons
    private int getEnteredBetAsInt() {
        String text = edtBetAmount.getText().toString().trim();
        if (text.isEmpty()) return 0;
        try {
            return (int) Double.parseDouble(text); // Χειρίζεται και τυχόν δεκαδικά που πληκτρολόγησε ο χρήστης
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void sendPlayRequest(double betAmount) {
        PlayRequest request = new PlayRequest(gameName, betAmount, playerName);

        if (tvResult != null) {
            tvResult.setText("Sending bet to Master...");
        }

        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                out.writeObject("PLAYER_PLAY");
                out.writeObject(request);
                out.flush();

                String result = (String) in.readObject();
                String lowerResult = result.toLowerCase();

                boolean containsWinWord = lowerResult.contains("win") || lowerResult.contains("won");

                String superClean = lowerResult.replaceAll("[^a-z0-9]", "");
                boolean isFakeWin = superClean.contains("won00") || superClean.contains("won0")
                        || superClean.contains("win00") || superClean.contains("win0");

                boolean isErrorResponse = lowerResult.contains("error")
                        || lowerResult.contains("high")
                        || lowerResult.contains("limit")
                        || lowerResult.contains("insufficient")
                        || lowerResult.contains("fail");

                boolean isLossOrError = isErrorResponse
                        || lowerResult.contains("lost")
                        || lowerResult.contains("no win")
                        || isFakeWin;

                // sound settings
                android.content.SharedPreferences prefs = getSharedPreferences("CasinoPrefs", MODE_PRIVATE);
                boolean effectsEnabled = prefs.getBoolean("sound_effects", true);

                if (effectsEnabled) {
                    if (isLossOrError) {
                        playSingleSound(R.raw.lose_sound);
                    } else if (containsWinWord) {
                        playSequentialSounds(R.raw.win_trumpet, R.raw.coins_win);
                    }
                }

                runOnUiThread(() -> {
                    if (tvResult != null) {
                        tvResult.setText(result);
                    }
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();

                    if (isErrorResponse) {
                        startLossAnimation();
                    } else if (containsWinWord && !isFakeWin && !lowerResult.contains("lost") && !lowerResult.contains("no win")) {
                        startCoinRain();

                        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+\\.\\d+|\\d+)\\s*fun");
                        java.util.regex.Matcher m = p.matcher(lowerResult);

                        double winAmount = 0.0;
                        if (m.find()) {
                            try {
                                String cleanAmount = m.group(1);
                                winAmount = Double.parseDouble(cleanAmount);
                            } catch (NumberFormatException e) {
                                android.util.Log.e("CasinoError", "Parsing failed, using betAmount as fallback");
                                winAmount = betAmount;
                            }
                        } else {
                            winAmount = betAmount;
                        }

                        double finalWin = winAmount;
                        playerBalance += finalWin;
                    } else {
                        startLossAnimation();
                        playerBalance -= (int) betAmount;
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (tvResult != null) {
                        tvResult.setText("Connection Error. Please try again.");
                    }
                    Toast.makeText(GamePlayActivity.this, "Failed to connect to Master", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Βροχή Νομισμάτων (Coin Rain Animation)
    private void startCoinRain() {
        //settings
        android.content.SharedPreferences prefs = getSharedPreferences("CasinoPrefs", MODE_PRIVATE);
        boolean effectsEnabled = prefs.getBoolean("sound_effects", true);

        if (!effectsEnabled) {
            return;
        }

        // Χρησιμοποιούμε το ID της κεντρικής RelativeLayout που βάλαμε στο XML
        final ViewGroup root = findViewById(android.R.id.content);
        if (root == null) return;

        for (int i = 0; i < 300; i++) { // Αυξηση τα νομίσματων σε 25 για πιο εντυπωσιακό εφέ
            ImageView coin = new ImageView(this);
            coin.setImageResource(R.drawable.ic_coin);

            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(100, 100); //pixels
            coin.setLayoutParams(params);

            coin.setX((float) (Math.random() * root.getWidth()));
            coin.setY(-200);

            root.addView(coin);

            //motion
            coin.animate()
                    .translationY(root.getHeight() + 1500) //+ 1.500 pixels στον άξονα Y'
                    .setDuration(2000 + (long)(Math.random() * 1000))
                    .setStartDelay((long) (Math.random() * 600))
                    .withEndAction(() -> root.removeView(coin))
                    .start();
        }
    }

    private void startLossAnimation() {
        final View cardView = findViewById(R.id.mainBetCard);

        // visibility
        if (cardView != null) {
            float originalAlpha = cardView.getAlpha(); // get visibility

            cardView.animate()
                    .alpha(0.3f)
                    .setDuration(200)
                    .withEndAction(() -> cardView.animate()
                            .alpha(originalAlpha) // επαναφορά
                            .setDuration(200)
                            .start())
                    .start();
        }

        // shake
        if (tvResult != null) {
            //Γυρίζει το κείμενο σε έντονο κόκκινο στην έναρξη της ήττας
            tvResult.setTextColor(android.graphics.Color.parseColor("#FF4D4D")); // red color

            //Ξεκινάει το κούνημα (Shake)
            tvResult.animate()
                    .translationXBy(30f) // +30 pixels on x'
                    .setDuration(120)
                    .withEndAction(() -> tvResult.animate()
                            .translationXBy(-60f)
                            .setDuration(120)
                            .withEndAction(() -> tvResult.animate()
                                    .translationXBy(60f) //again
                                    .setDuration(120)
                                    .withEndAction(() -> tvResult.animate()
                                            .translationX(0f) // initial position
                                            .setDuration(120)
                                            .withEndAction(() -> {
                                                // επαναφέρουμε το αρχικό χρώμα του XML
                                                tvResult.setTextColor(android.graphics.Color.parseColor("#B0C4DE"));
                                            })
                                            .start())
                                    .start())
                            .start())
                    .start();
        }
    }

    private void fetchActualBalance() {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                out.writeObject("GET_BALANCE");
                out.writeObject(playerName);
                out.flush();

                Object responseObj = in.readObject();
                if (responseObj != null) {
                    String response = responseObj.toString();

                    //Καθαρίζει από "100.0 FUN." σε σκέτο "100.0" χωρίς την τελεία της πρότασης
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+\\.\\d+|\\d+)");
                    java.util.regex.Matcher m = p.matcher(response);

                    if (m.find()) {
                        String cleanAmount = m.group(1);
                        double parsedBalance = Double.parseDouble(cleanAmount);

                        // Ενημέρωση μεταβλητής
                        GamePlayActivity.this.playerBalance = parsedBalance;

                        runOnUiThread(() -> {
                            if (tvResult != null) {
                                tvResult.setText("Balance: " + GamePlayActivity.this.playerBalance + " FUN");
                            }
                        });
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("CasinoError", "Error fetching balance: " + e.getMessage());
                // Safe fallback αν πέσει το δίκτυο
                GamePlayActivity.this.playerBalance = 1000.0;
            }
        }).start();
    }

    // Kουμπιά
    private void setAnimatedClick(int resId, Runnable action) {
        View view = findViewById(resId);
        if (view != null) {
            view.setOnClickListener(v -> {
                android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click);
                anim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                    @Override public void onAnimationStart(android.view.animation.Animation a) {}
                    @Override public void onAnimationRepeat(android.view.animation.Animation a) {}
                    @Override
                    public void onAnimationEnd(android.view.animation.Animation a) {
                        action.run(); // Εκτέλεση της λογικής μετά το εφέ
                    }
                });
                v.startAnimation(anim);
            });
        }
    }

    //ΗΧΟΙ
    private void playSingleSound(int soundResId) {
        new Thread(() -> {
            try {
                android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(this, soundResId);
                if (mediaPlayer != null) {
                    mediaPlayer.setOnCompletionListener(android.media.MediaPlayer::release); // end sound
                    mediaPlayer.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void playSequentialSounds(int firstSoundResId, int secondSoundResId) {
        new Thread(() -> {
            try {
                android.media.MediaPlayer firstPlayer = android.media.MediaPlayer.create(this, firstSoundResId); //trumpets first
                if (firstPlayer != null) {
                    firstPlayer.setOnCompletionListener(mp1 -> {
                        mp1.release();
                        android.media.MediaPlayer secondPlayer = android.media.MediaPlayer.create(this, secondSoundResId); //coins
                        if (secondPlayer != null) {
                            secondPlayer.setOnCompletionListener(android.media.MediaPlayer::release);
                            secondPlayer.start();
                        }
                    });
                    firstPlayer.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}