package com.example.casinoapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

public class DialogHelper {
    // Displays a custom AlertDialog with embedded animations and a reusable layout
    public static AlertDialog showCustomDialog(Context context, String title, View contentView, View.OnClickListener confirmAction) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.dialog_helper, null);

        // Bind UI components from the inflated layout
        TextView tvTitle = layout.findViewById(R.id.dialogTitle);
        FrameLayout container = layout.findViewById(R.id.dialogContent);
        Button btnConfirm = layout.findViewById(R.id.btnDialogConfirm);

        // Set the dynamic title text
        if (tvTitle != null) tvTitle.setText(title);

        // Inject the dynamically provided content view into the container
        if (container != null) {
            container.removeAllViews();
            container.addView(contentView);
        }

        AlertDialog dialog = builder.setView(layout).create();

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                // 1. Load the localized button click scaling/shrink animation
                Animation anim = AnimationUtils.loadAnimation(context, R.anim.button_click);

                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // 2. Μόλις τελειώσει το οπτικό εφέ, εκτελούμε την ενέργεια
                        confirmAction.onClick(v);
                        dialog.dismiss();
                    }
                });

                v.startAnimation(anim);
            });
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
        return dialog;
    }
}