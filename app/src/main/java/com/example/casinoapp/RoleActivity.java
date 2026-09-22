package com.example.casinoapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RoleActivity extends AppCompatActivity {

    private static final String MANAGER_PIN = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.role_choice);

        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);

        View btnPlayer = findViewById(R.id.btnPlayerRole);
        View btnManager = findViewById(R.id.btnManagerRole);

        // Για τον Player
        btnPlayer.setOnClickListener(v -> {
            animateAndNavigate(v, () -> {
                Intent intent = new Intent(RoleActivity.this, PlayerLoginActivity.class);
                startActivity(intent);
            });
        });

        // Για τον Manager
        btnManager.setOnClickListener(v -> {
            animateAndNavigate(v, () -> {
                showManagerPinDialog();
            });
        });
    }

    private void showManagerPinDialog() {
        // 1. Φτιάχνουμε το περιεχόμενο (EditText) για το PIN
        final EditText input = new EditText(this);
        input.setHint("ENTER PIN");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.parseColor("#F0E68C"));
        // Ρύθμιση για αριθμούς και απόκρυψη χαρακτήρων (password)
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setPadding(30, 40, 30, 40);

        // 2. Χρήση του DialogHelper για το παράθυρο
        DialogHelper.showCustomDialog(this, "LOG INTO MANAGER", input, v -> {
            String enteredPin = input.getText().toString();

            if (enteredPin.equals(MANAGER_PIN)) {
                Toast.makeText(this, "Authentication done!", Toast.LENGTH_SHORT).show();
                // Μόνο αν το PIN είναι σωστό προχωράμε στο Manager Menu
                Intent intent = new Intent(RoleActivity.this, ManagerMenuActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "WRONG PIN! Access denied.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void animateAndNavigate(View view, Runnable navigationLogic) {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.button_click);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {

                navigationLogic.run();
            }
        });
        view.startAnimation(anim);
    }
}