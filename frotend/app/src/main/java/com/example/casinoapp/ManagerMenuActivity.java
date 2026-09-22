package com.example.casinoapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManagerMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manager_menu);

        // 1. ADD GAME
        setAnimatedClick(R.id.btnAddGame, () -> showAddGameDialog());

        // 2. VIEW PROFITS (MapReduce Query)
        setAnimatedClick(R.id.btnViewProfits, () -> showViewProfitsDialog());

        // 3. SOFT DELETE
        setAnimatedClick(R.id.btnDeleteGame, () ->
                showSimpleInputDialog("Soft Delete", "Enter Game Name", "MANAGER_DELETE"));

        // 4. RESTORE GAME
        setAnimatedClick(R.id.btnRestoreGame, () ->
                showSimpleInputDialog("Restore Game", "Enter Game Name", "MANAGER_RESTORE"));

        // 5. MODIFY SETTINGS (Risk/Limits)
        setAnimatedClick(R.id.btnModifyGame, () -> showModifyChoiceDialog());

        // EXIT
        setAnimatedClick(R.id.btnLogout, () -> finish());
    }

    // --- ΔΙΚΤΥΑΚΗ ΕΠΙΚΟΙΝΩΝΙΑ (Η ΓΕΝΙΚΕΥΜΕΝΗ ΜΕΘΟΔΟΣ) ---
    private void performNetworkTask(String command,String title, Object... payloads) {
        new Thread(() -> {
            try (Socket socket = new Socket("10.0.2.2", 5556);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // 1. Στέλνουμε την εντολή
                out.writeObject(command);

                // 2. Στέλνουμε τα δεδομένα ανάλογα με τον τύπο τους
                for (Object p : payloads) {
                    if (p instanceof Double) {
                        out.writeDouble((Double) p);
                    } else {
                        out.writeObject(p);
                    }
                }

                out.flush();

                // Λήψη απάντησης από τον Master
                try {
                    // Μέσα στην performNetworkTask, στο σημείο της απάντησης:
                    Object response = in.readObject();
                    String resultText = response.toString();

                    runOnUiThread(() -> {
                        System.out.println("DEBUG SERVER RESPONSE: " + resultText);
                        Toast.makeText(this, "Received: " + resultText, Toast.LENGTH_LONG).show();
                        handleManagerResponse(resultText,title);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Request sent successfully!", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // --- DIALOGS ---

        // 1. Dialog για το Add Game (χρησιμοποιεί τα Assets)
        private void showAddGameDialog() {

            final EditText input = new EditText(this);
            input.setHint("e.g. Starburst.json");
            input.setTextColor(android.graphics.Color.parseColor("#F0E68C")); // Λευκά γράμματα
            input.setHintTextColor(android.graphics.Color.parseColor("#888888")); // Γκρι Hint
            input.setPadding(30, 40, 30, 40); // Λίγο padding για να μην κολλάει στις άκρες

            // 2. Καλούμε τον DialogHelper
            // Περνάμε το 'this', τον τίτλο, το 'input' και τη λογική σου στο onClick
            DialogHelper.showCustomDialog(this, "Add Game from Assets", input, v -> {

                String content = loadJSONFromAsset(input.getText().toString());
                if (content != null) {
                    // Μετατρέπουμε το JSON σε Game και το στέλνουμε ως αντικείμενο
                    Game gameObj = parseJsonToGame(content);
                    performNetworkTask("MANAGER_ADD","Game Added", gameObj);
                } else {
                    Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
                }
            });
        }

    // 2. Dialog για το View Profits
    private void showViewProfitsDialog() {
        // 1. Φτιάχνουμε το Layout που θα περιέχει τα δύο EditText
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20); // Προσαρμοσμένο padding για το message.xml

        // Πεδίο για το Type (Provider ή Player)
        final EditText inputType = new EditText(this);
        inputType.setHint("Query Type (Provider/Player)");
        inputType.setTextColor(android.graphics.Color.parseColor("#F0E68C"));
        inputType.setHintTextColor(android.graphics.Color.parseColor("#888888"));
        layout.addView(inputType);

        // Πεδίο για το Name (π.χ. NetEnt ή user123)
        final EditText inputTarget = new EditText(this);
        inputTarget.setHint("Target Name (e.g. NetEnt)");
        inputTarget.setTextColor(android.graphics.Color.parseColor("#F0E68C"));
        inputTarget.setHintTextColor(android.graphics.Color.parseColor("#888888"));
        layout.addView(inputTarget);

        // DialogHelper for input
        DialogHelper.showCustomDialog(this, "View Aggregated Profits", layout, v -> {


            String target = inputTarget.getText().toString().trim();
            String type = inputType.getText().toString().trim();

            if (!target.isEmpty() && !type.isEmpty()) {
                // Δημιουργία του αντικειμένου Request
                ManagerQueryRequest request = new ManagerQueryRequest(1, type, target);

                // Αποστολή στο δίκτυο
                performNetworkTask("MANAGER_AGGREGATE", "Aggregated profits", request);
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //3. Γενικό Dialog για Delete/Restore
    private void showSimpleInputDialog(String title, String hint, String command) {

        final EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(android.graphics.Color.parseColor("#F0E68C"));
        input.setHintTextColor(android.graphics.Color.parseColor("#888888")); // Γκρι Hint
        input.setPadding(30, 40, 30, 40); // Padding για να μην κολλάει στο πλαίσιο

        DialogHelper.showCustomDialog(this, title, input, v -> {

            String payload = input.getText().toString().trim();

            if (!payload.isEmpty()) {

                performNetworkTask(command, title, payload);
            }
        });
    }

    //4. Dialog για Modify Game Settings
    private void showModifyChoiceDialog() {
        // 1. Φτιάχνουμε ένα κάθετο layout για να βάλουμε τις επιλογές μας ως κουμπιά
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        // Στυλ για τα κουμπιά επιλογής (Risk Level)
        android.widget.Button btnRisk = new android.widget.Button(this);
        btnRisk.setText("Risk Level");
        btnRisk.setBackgroundResource(R.drawable.bg_button_gold);
        btnRisk.setTextColor(Color.parseColor("#F0E68C"));
        btnRisk.setAllCaps(true);
        layout.addView(btnRisk);

        // Προσθήκη μικρού κενού ανάμεσα στα κουμπιά
        android.view.View spacer = new android.view.View(this);
        spacer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(1, 20));
        layout.addView(spacer);

        // Στυλ για τα κουμπιά επιλογής (Bet Limits)
        android.widget.Button btnLimits = new android.widget.Button(this);
        btnLimits.setText("Bet Limits");
        btnLimits.setBackgroundResource(R.drawable.bg_button_gold);
        btnLimits.setTextColor(android.graphics.Color.parseColor("#F0E68C"));
        btnLimits.setAllCaps(true);
        layout.addView(btnLimits);

        // 2. Εμφάνιση μέσω DialogHelper
        AlertDialog menuDialog = DialogHelper.showCustomDialog(this, "Choose modification", layout, v -> {
        });

        // 3. Σύνδεση των ενεργειών στα κουμπιά
        btnRisk.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.button_click);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override
                public void onAnimationEnd(Animation a) {
                    menuDialog.dismiss();
                    showRiskDialog();
                }
            });
            v.startAnimation(anim);
        });

        btnLimits.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.button_click);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override
                public void onAnimationEnd(Animation a) {
                    menuDialog.dismiss();
                    showLimitsDialog();
                }
            });
            v.startAnimation(anim);
        });
    }

    // Risk Level modify
    private void showRiskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CasinoDialogTheme);
        builder.setTitle("Modify Risk Level");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Game Name");
        inputName.setTextColor(android.graphics.Color.parseColor("#F0E68C"));
        inputName.setHintTextColor(android.graphics.Color.parseColor("#888888")); // Γκρι Hint
        layout.addView(inputName);

        final EditText inputRisk = new EditText(this);
        inputRisk.setHint("New Risk (Low/Medium/High)");
        inputRisk.setTextColor(android.graphics.Color.parseColor("#F0E68C")); //
        inputRisk.setHintTextColor(android.graphics.Color.parseColor("#888888")); // Γκρι Hint
        layout.addView(inputRisk);

        builder.setView(layout);

        builder.setPositiveButton("OK", (d, w) -> {
            String gameName = inputName.getText().toString();
            String newRisk = inputRisk.getText().toString();
            performNetworkTask("MANAGER_MODIFY_RISK", "Risk Updated", gameName, newRisk);

            d.dismiss();
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
        }
        dialog.show();

        // Χρώμα στα κουμπιά
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#F0E68C"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#F0E68C"));
    }

    // Bet Limits modify
    private void showLimitsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CasinoDialogTheme);
        builder.setTitle("Modify Bet Limits");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Game Name");
        inputName.setTextColor(android.graphics.Color.parseColor("#F0E68C")); // Λευκά γράμματα
        inputName.setHintTextColor(android.graphics.Color.parseColor("#888888")); // Γκρι Hint
        layout.addView(inputName);

        final EditText inputMin = new EditText(this);
        inputMin.setHint("New Min Bet");
        inputMin.setTextColor(android.graphics.Color.parseColor("#F0E68C")); // Λευκά γράμματα
        inputMin.setHintTextColor(android.graphics.Color.parseColor("#888888")); // Γκρι Hint
        inputMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputMin);

        final EditText inputMax = new EditText(this);
        inputMax.setHint("New Max Bet");
        inputMax.setTextColor(android.graphics.Color.parseColor("#F0E68C")); // Λευκά γράμματα
        inputMax.setHintTextColor(android.graphics.Color.parseColor("#888888")); // Γκρι Hint
        inputMax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputMax);

        builder.setView(layout);

        builder.setPositiveButton("OK", (d, w) -> {
            try {
                String gName = inputName.getText().toString();
                String minStr = inputMin.getText().toString();
                String maxStr = inputMax.getText().toString();

                if (gName.isEmpty() || minStr.isEmpty() || maxStr.isEmpty()) {
                    Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show();
                    return;
                }

                double min = Double.parseDouble(minStr);
                double max = Double.parseDouble(maxStr);

                performNetworkTask("MANAGER_MODIFY_LIMITS", gName, min, max);
            } catch (Exception e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
        }

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#F0E68C"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#F0E68C"));
    }


    // --- HELPER METHODS ---

    // (JSON & PARSING)
    private String loadJSONFromAsset(String fileName) {
        try {
            java.io.InputStream is = getAssets().open(fileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (Exception ex) {
            return null;
        }
    }

    private Game parseJsonToGame(String json) {
        return new Game(
                extractString(json, "GameName"),
                extractString(json, "ProviderName"),
                extractInt(json, "Stars"),
                extractInt(json, "NoOfVotes"),
                extractString(json, "GameLogo"),
                extractDouble(json, "MinBet"),
                extractDouble(json, "MaxBet"),
                extractString(json, "RiskLevel"),
                extractString(json, "HashKey")
        );
    }

    //for extracting elements from JSON files
    private String extractString(String j, String k) {
        Matcher m = Pattern.compile("\"" + k + "\"\\s*:\\s*\"(.*?)\"").matcher(j);
        return m.find() ? m.group(1) : "";
    }

    private int extractInt(String j, String k) {
        Matcher m = Pattern.compile("\"" + k + "\"\\s*:\\s*([0-9]+)").matcher(j);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private double extractDouble(String j, String k) {
        Matcher m = Pattern.compile("\"" + k + "\"\\s*:\\s*([0-9.]+)").matcher(j);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private void handleManagerResponse(String resultText, String title) {
        // Χρησιμοποιούμε το CasinoDialogTheme για το στυλ
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CasinoDialogTheme);

        // 1. Το κεντρικό Container (Το μεγάλο μαύρο ορθογώνιο)
        android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(this);
        rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        rootLayout.setGravity(android.view.Gravity.CENTER);
        rootLayout.setBackgroundResource(R.drawable.bg_dialog);
        rootLayout.setPadding(60, 70, 60, 50);

        // 2. Ο Τίτλος (π.χ. RISK UPDATED)
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title.toUpperCase());
        tvTitle.setTextColor(Color.parseColor("#F0E68C"));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 60);
        rootLayout.addView(tvTitle);

        // 3. Το Μήνυμα (SUCCESS / NOT_FOUND
        TextView tvResult = new TextView(this);

        if (resultText.equals("NOT_FOUND")) {
            tvResult.setText("Try again!");
            tvTitle.setText("RISK NOT UPDATED");
        } else {
            tvResult.setText(resultText.trim());
        }

        tvResult.setTextColor(Color.parseColor("#F0E68C"));
        tvResult.setTextSize(25);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setGravity(android.view.Gravity.CENTER);
        tvResult.setPadding(0, 20, 0, 60);
        rootLayout.addView(tvResult);

        // 4. Το κουμπί OK κεντραρισμένο
        android.widget.Button btnOk = new android.widget.Button(this);
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnOk.setLayoutParams(btnParams);
        btnOk.setText("OK");
        btnOk.setTextColor(Color.parseColor("#F0E68C"));
        btnOk.setBackgroundResource(android.R.color.transparent);
        btnOk.setTextSize(18);
        btnOk.setTypeface(null, Typeface.BOLD);
        rootLayout.addView(btnOk);

        builder.setView(rootLayout);
        AlertDialog dialog = builder.create();

        // Κλείσιμο του dialog όταν πατηθεί το OK
        btnOk.setOnClickListener(v -> dialog.dismiss());

        // Απαραίτητο για να μη φαίνεται η "κάσα" του Android πίσω από τις στρογγυλεμένες γωνίες
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
    }

    private void setAnimatedClick(int resId, Runnable action) {
        View view = findViewById(resId);
        if (view != null) {
            view.setOnClickListener(v -> {
                // Φόρτωση του XML animation
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.button_click);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        action.run();
                    }
                });
                v.startAnimation(anim);
            });
        }
    }
}