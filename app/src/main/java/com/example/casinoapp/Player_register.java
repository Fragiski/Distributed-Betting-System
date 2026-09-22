package com.example.casinoapp;

import android.app.DatePickerDialog;
import java.util.Calendar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Locale;

public class Player_register extends AppCompatActivity {

    EditText editFullName, editEmail, editPlayerName, editPassword,editBirthDate;
    View btnRegister, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_register);

        editFullName = findViewById(R.id.FirstName);
        editEmail = findViewById(R.id.Email);
        editPlayerName = findViewById(R.id.PlayerName);
        editPassword = findViewById(R.id.Password);
        editBirthDate = findViewById(R.id.BirthDate);

        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        editBirthDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.MyDatePickerStyle,
                    (view, selectedYear, selectedMonth, selectedDay) -> {

                        // 1. Υπολογισμός ηλικίας
                        Calendar today = Calendar.getInstance();
                        Calendar dob = Calendar.getInstance();
                        dob.set(selectedYear, selectedMonth, selectedDay);

                        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

                        // Έλεγχος αν δεν έχει κλείσει ακόμα τα γενέθλια του φέτος
                        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                            age--;
                        }

                        // 2. Έλεγχος αν είναι άνω των 21
                        if (age >= 21) {
                            String date = String.format(Locale.getDefault(), "%d-%02d-%02d",
                                    selectedYear, (selectedMonth + 1), selectedDay);
                            editBirthDate.setText(date);
                        } else {
                            // 3. Μήνυμα σφάλματος αν είναι κάτω από 21
                            Toast.makeText(this, "You must be over 21 years old to register!", Toast.LENGTH_LONG).show();
                            editBirthDate.setText(""); // Καθαρίζουμε το πεδίο
                        }

                    }, year, month, day);

            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        btnBack.setOnClickListener(v -> animateAndNavigate(v, () -> {
            Intent intent = new Intent(Player_register.this, PlayerLoginActivity.class);
            startActivity(intent);
            finish();
        }));

        // Κουμπί Εγγραφής
        btnRegister.setOnClickListener(v -> animateAndNavigate(v, () -> {
            String fullName = editFullName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String pName = editPlayerName.getText().toString().trim();
            String pass = editPassword.getText().toString().trim();
            String bDate = editBirthDate.getText().toString().trim();

            if (!fullName.isEmpty() && !email.isEmpty() && !pName.isEmpty() && !pass.isEmpty() &&!bDate.isEmpty()) {
                Toast.makeText(this, "Έλεγχος και εγγραφή...", Toast.LENGTH_SHORT).show();
                // Στέλνουμε την εντολή ADD_PLAYER
                handleNetworkAction("ADD_PLAYER", pName, pass, fullName, email,bDate);
            } else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void handleNetworkAction(String action, String pName, String pass, String fullName, String email,String bDate) {
        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                out.writeObject(action);
                out.writeObject(pName);
                out.writeObject(pass);
                out.writeObject(fullName);
                out.writeObject(email);
                out.writeObject(bDate);
                out.flush();

                Object response = in.readObject();

                runOnUiThread(() -> {
                    String result = (response != null) ? response.toString() : "Error";

                    // Αν ο Server βρει ότι ο παίκτης υπάρχει, πρέπει να στείλει "ALREADY_EXISTS"
                    if (result.equals("ALREADY_EXISTS")) {
                        Toast.makeText(this, "User already exists!", Toast.LENGTH_LONG).show();
                    }
                    // Αν όλα πάνε καλά, ο Server στέλνει "SUCCESS"
                    else if (result.contains("SUCCESS")) {
                        Toast.makeText(this, "Registration completed!", Toast.LENGTH_SHORT).show();
                        goToDashboard(pName);
                    }
                    else {
                        Toast.makeText(this, "Error: " + result, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error connection with Server", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void goToDashboard(String name) {
        Intent intent = new Intent(this, PlayerLoginActivity.class);
        intent.putExtra("playerName", name);
        startActivity(intent);
        finish();
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
}