package com.example.casinoapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlayerLoginActivity extends AppCompatActivity {
    EditText editName, editPassword;
    View btnLogin, btnRegister, btnLogout;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_login);

        // Σύνδεση με τα στοιχεία του XML (activity_player_login.xml)
        editName = findViewById(R.id.editPlayerName);
        editPassword = findViewById(R.id.editPlayerPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogout = findViewById(R.id.btnLogout);

        setupPasswordToggle();

        // 1. Λειτουργία Σύνδεσης: Ελέγχει αν υπάρχει ο παίκτης
        btnLogin.setOnClickListener(v -> animateAndNavigate(v, () -> {
            String name = editName.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            if (!name.isEmpty() && !password.isEmpty()) {
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
                handleNetworkAction("CHECK_PLAYER_EXISTS", name, password);
            } else {
                Toast.makeText(this, "Please enter name and password!", Toast.LENGTH_SHORT).show();
            }
        }));

        // 2. Λειτουργία Εγγραφής: Δημιουργεί νέο παίκτη
        btnRegister = findViewById(R.id.btnRegister);
        if (btnRegister == null) {
            Toast.makeText(this, "Σφάλμα: Το btnRegister δεν βρέθηκε στο XML!", Toast.LENGTH_LONG).show();
        } else {
            btnRegister.setOnClickListener(v -> animateAndNavigate(v, () -> {
                Intent intent;
                intent = new Intent(PlayerLoginActivity.this, Player_register.class);
                startActivity(intent);
            }));
        }

        // EXIT
        btnLogout.setOnClickListener(v -> animateAndNavigate(v, () -> {
            finish();
        }));
    }

    private void handleNetworkAction(String action, String playerName, String password) {
        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // Στέλνουμε την εντολή, το όνομα και τον κωδικό
                out.writeObject(action);
                out.writeObject(playerName);
                out.writeObject(password);
                out.flush();

                // Λήψη απάντησης
                Object response = in.readObject();

                runOnUiThread(() -> {

                        boolean exists = false;
                        if (response instanceof Boolean) {
                            exists = (Boolean) response;
                        } else if (response instanceof ReducerResult) {
                            // If the server returns a ReducerResult as a unified response
                            exists = ((ReducerResult) response).getTotalAmount() > 0;
                        } else if (response != null) {
                            String respStr = response.toString();
                            exists = respStr.equalsIgnoreCase("true") || respStr.contains("SUCCESS");
                        }

                        if (exists) {
                            Toast.makeText(this, "Σύνδεση επιτυχής!", Toast.LENGTH_SHORT).show();
                            goToDashboard(playerName);
                        } else {
                            Toast.makeText(this, "Player not found!", Toast.LENGTH_LONG).show();
                        }


                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Σφάλμα σύνδεσης με τον Server!", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void goToDashboard(String name) {
        getSharedPreferences("CasinoPrefs", MODE_PRIVATE)
                .edit()
                .putString("playerName", name)
                .apply();

        Intent intent = new Intent(this, SearchAndPlayActivity.class);
        intent.putExtra("playerName", name);
        startActivity(intent);
    }

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

    // --- ΛΕΙΤΟΥΡΓΙΑ PASSWORD TOGGLE ---
    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle() {
        final EditText passwordEntry = findViewById(R.id.editPlayerPassword);

        passwordEntry.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2; // eye icon

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (passwordEntry.getRight() - passwordEntry.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) { // checking if eye icon was clicked

                    if (isPasswordVisible) { // make password invisible
                        passwordEntry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                        passwordEntry.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0);
                        isPasswordVisible = false;
                    } else {
                        passwordEntry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

                        passwordEntry.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
                        isPasswordVisible = true;
                    }

                    passwordEntry.setSelection(passwordEntry.getText().length());
                    v.performClick();
                    return true;
                }
            }
            return false;
        });
    }
}