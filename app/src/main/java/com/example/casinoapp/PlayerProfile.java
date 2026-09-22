package com.example.casinoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

public class PlayerProfile extends AppCompatActivity {
    String playerName;
    TextView tvBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Ενεργοποίηση EdgeToEdge
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_profile);

        // 2. Παίρνουμε το playerName από τα SharedPreferences
        SharedPreferences prefs = getSharedPreferences("CasinoPrefs", MODE_PRIVATE);
        playerName = prefs.getString("playerName", "Guest");

        // 3. Αρχικοποίηση ΟΛΩΝ των UI στοιχείων
        tvBalance = findViewById(R.id.tvBalance);
        TextView tvLoginName = findViewById(R.id.tvLoginName);
        View rootLayout = findViewById(R.id.player_dashboard_root);
        View btnSmallAdd = findViewById(R.id.btnSmallAddTokens);
        View footer = findViewById(R.id.footer);

        // 4. setup των κουμπιών
        setupFooterButtons();

        // 5. Διαχείριση Window Insets για να μην κλείνει η εφαρμογή
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                return insets;
            });
        }

        // 6. Ενημέρωση UI με τα δεδομένα του παίκτη
        if (tvLoginName != null && playerName != null) {
            tvLoginName.setText(playerName.toUpperCase());
            performNetworkTask("GET_PLAYER_INFO", playerName);
        }

        // 7. Click Listeners
        if (btnSmallAdd != null) {
            btnSmallAdd.setOnClickListener(v -> showAddBalanceDialog());
        }

        if (footer != null) {
            footer.bringToFront();
        }
    }

    // Αυτόματη ανανέωση του υπολοίπου
    @Override
    protected void onResume() {
        super.onResume();
        // Κάθε φορά που ο παίκτης επιστρέφει στο Dashboard
        // η εφαρμογή ζητάει αυτόματα το ενημερωμένο υπόλοιπο από τον Master
        performNetworkTask("GET_BALANCE", playerName);
    }

    private void performNetworkTask(String command, Object... payloads) {
        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                out.writeObject(command);

                for (Object p : payloads) {
                    if (p instanceof Double) out.writeDouble((Double) p);
                    else out.writeObject(p);
                }
                out.flush();

                Object response = in.readObject();

                runOnUiThread(() -> {
                    if (command.equals("GET_BALANCE")) {
                        String fullResponse = response.toString();
                        String amountOnly = fullResponse.replaceAll("[^0-9.]", "");

                        tvBalance.setText(amountOnly + " FUN");

                        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                        tvBalance.startAnimation(fadeIn);

                    } else if (command.equals("ADD_BALANCE")) {
                        performNetworkTask("GET_BALANCE", playerName);

                    } else if (command.equals("GET_PLAYER_INFO")) {
                        // Λαμβάνουμε το αντικείμενο Player
                        Player p = (Player) response;

                        Log.d("DEBUG", "Received date: " + p.getDate());

                        // Ενημέρωση των TextViews στο XML
                        TextView tvFullName = findViewById(R.id.tvFullNameProfile);
                        TextView tvEmail = findViewById(R.id.tvEmailProfile);
                        TextView tvBirthDate = findViewById(R.id.tvBirthDate);

                        tvFullName.setText(p.getFullName());
                        tvEmail.setText(p.getEmail());
                        tvBirthDate.setText(p.getDate());

                        tvBalance.setText(String.format("%.2f FUN", p.getBalance()));
                    }

                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Connection Error to Master", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showAddBalanceDialog() {
        EditText input = new EditText(this);
        input.setHint("Ποσό σε FUN");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        DialogHelper.showCustomDialog(this, "ΚΑΤΑΘΕΣΗ", input, v -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                performNetworkTask("ADD_BALANCE", playerName, Double.parseDouble(val));
            }
        });
    }

    private void setupFooterButtons() {
        // Κουμπί LOBBY / GAMES
        View navLobby = findViewById(R.id.navLobby);
        if (navLobby != null) {
            navLobby.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerLoginActivity.class);
                intent.putExtra("playerName", playerName);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        // Κουμπί SETTINGS
        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("playerName", playerName);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        // Κουμπί PROFILE
        View navAccount = findViewById(R.id.navAccount);
        if (navAccount != null) {
            navAccount.setOnClickListener(v -> {
               Toast.makeText(this, "Already in profile", Toast.LENGTH_SHORT).show();
            });
        }

        // κουμπί navGames
        View navGames = findViewById(R.id.navGames);
        if (navGames != null) {
            navGames.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchAndPlayActivity.class);
                intent.putExtra("playerName", playerName);
                startActivity(intent);
            });
        }
    }
}