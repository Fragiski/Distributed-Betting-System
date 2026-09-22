package com.example.casinoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SettingsActivity extends AppCompatActivity {

    private String playerName;
    private EditText editUsername, editPassword;
    private MaterialButton btnUpdate, btnDeleteAccount;
    private SwitchCompat switchSounds, switchMusic;
    private View rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rootLayout = findViewById(android.R.id.content);
        editUsername = findViewById(R.id.editNewUsername);
        editPassword = findViewById(R.id.editNewPassword);
        btnUpdate = (MaterialButton) findViewById(R.id.btnUpdateAccount);
        btnDeleteAccount = (MaterialButton) findViewById(R.id.btnDeleteAccount);
        switchSounds = findViewById(R.id.switchSounds);
        switchMusic = findViewById(R.id.switchMusic);

        setupFooterButtons();
        handleWindowInsets();

        //Λήψη δεδομένων και ρυθμίσεων από τα SharedPreferences
        SharedPreferences prefs = getSharedPreferences("CasinoPrefs", MODE_PRIVATE);
        playerName = prefs.getString("playerName", "Guest");

        // Φόρτωμα των επιλογών του παίκτη για τους ήχους (προεπιλογή: true)
        if (switchSounds != null) {
            switchSounds.setChecked(prefs.getBoolean("sound_effects", true));
        }
        if (switchMusic != null) {
            switchMusic.setChecked(prefs.getBoolean("ambient_music", true));
        }

        setupListeners();
    }

    private void setupListeners() {
        // Listener για το κουμπί UPDATE
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> animateAndNavigate(v, () -> {
                String newName = editUsername.getText().toString().trim();
                String newPass = editPassword.getText().toString().trim();

                if (newName.isEmpty() && newPass.isEmpty()) {
                    Toast.makeText(this, "Παρακαλώ συμπληρώστε τουλάχιστον ένα πεδίο για αλλαγή", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Κατασκευή μηνύματος για τον Master
                String payload = "UPDATE_PROFILE:" + playerName + "," + newName + "," + newPass;
                performNetworkTask(payload, "UPDATE_PROFILE");
            }));
        }

        // Listener για το κουμπί DELETE ACCOUNT
        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> animateAndNavigate(v, () -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permanent Delete 🚨");
                builder.setMessage("Are you sure you want to permanently delete your account? All your balance and history will be lost forever.");

                builder.setPositiveButton("DELETE", (dialog, which) -> {
                    String payload = "DELETE_ACCOUNT:" + playerName;
                    performNetworkTask(payload, "DELETE_ACCOUNT");
                });

                builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                if (alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
            }));
        }

        // Αποθήκευση των Switches στα SharedPreferences όταν αλλάζουν κατάσταση
        if (switchSounds != null) {
            switchSounds.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = getSharedPreferences("CasinoPrefs", MODE_PRIVATE).edit();
                editor.putBoolean("sound_effects", isChecked);
                editor.apply();

                String status = isChecked ? "enabled" : "disabled";
                Toast.makeText(this, "Visual & Sound effects " + status, Toast.LENGTH_SHORT).show();
            });
        }

        if (switchMusic != null) {
            switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // 1. Αποθήκευση της ρύθμισης
                SharedPreferences.Editor editor = getSharedPreferences("CasinoPrefs", MODE_PRIVATE).edit();
                editor.putBoolean("ambient_music", isChecked);
                editor.apply();

                // 2. Επικοινωνία με το MusicService
                Intent musicIntent = new Intent(this, MusicService.class);
                if (isChecked) {

                    startService(musicIntent);
                } else {

                    stopService(musicIntent);
                }
                String status = isChecked ? "enabled" : "disabled";
                Toast.makeText(this, "Ambient music " + status, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void performNetworkTask(String payload, String operationType) {
        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(operationType);
                out.writeObject(payload);
                out.flush();

                String response = (String) in.readObject();

                runOnUiThread(() -> {
                    if ("SUCCESS".equalsIgnoreCase(response)) {
                        if ("DELETE_ACCOUNT".equalsIgnoreCase(operationType)) {
                            Toast.makeText(this, "Account Deleted Successfully", Toast.LENGTH_LONG).show();

                            SharedPreferences.Editor editor = getSharedPreferences("CasinoPrefs", MODE_PRIVATE).edit();
                            editor.remove("playerName");
                            editor.apply();

                            Intent intent = new Intent(this, PlayerLoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else if ("UPDATE_PROFILE".equalsIgnoreCase(operationType)) {
                            // Αν το update πέτυχε, παίρνουμε το νέο όνομα για να ενημερώσουμε το SharedPreferences
                            String data = payload.substring("UPDATE_PROFILE:".length());
                            String[] parts = data.split(",");
                            String newName = parts.length > 1 ? parts[1] : "";

                            if (!newName.isEmpty()) {
                                SharedPreferences.Editor editor = getSharedPreferences("CasinoPrefs", MODE_PRIVATE).edit();
                                editor.putString("playerName", newName);
                                editor.apply();
                                playerName = newName;
                            }
                            Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_LONG).show();
                            editUsername.setText("");
                            editPassword.setText("");
                        }
                    } else {
                        Toast.makeText(this, "Operation Failed: " + response, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleWindowInsets() {
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                return insets;
            });
        }
    }

    private void setupFooterButtons() {
        View navLobby = findViewById(R.id.navLobby);
        View navGames = findViewById(R.id.navGames);
        View navSettings = findViewById(R.id.navSettings);
        View navAccount = findViewById(R.id.navAccount);

        if (navLobby != null) {
            navLobby.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerLoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (navGames != null) {
            navGames.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchAndPlayActivity.class);
                intent.putExtra("playerName", playerName);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> Toast.makeText(this, "Already in settings", Toast.LENGTH_SHORT).show());
        }

        if (navAccount != null) {
            navAccount.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerProfile.class);
                intent.putExtra("playerName", playerName);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }

    private void animateAndNavigate(View view, Runnable action) {
        android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.button_click);
        anim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation animation) {}
            @Override public void onAnimationRepeat(android.view.animation.Animation animation) {}
            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                action.run();
            }
        });
        view.startAnimation(anim);
    }
}