package com.example.casinoapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.List;

public class GameAdapter extends BaseAdapter {
    private Context context;
    private List<Game> gameList;

    public GameAdapter(Context context, List<Game> gameList) {
        this.context = context;
        this.gameList = gameList;
    }

    @Override
    public int getCount() { return gameList.size(); }

    @Override
    public Object getItem(int i) { return gameList.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.game_item, viewGroup, false);
        }

        // Fetch the corresponding Game object for the current row/grid cell
        Game currentGame = (Game) getItem(i);

        // 1. Σύνδεση όλων των UI στοιχείων από το game_item.xml
        ImageView imgGameIcon = view.findViewById(R.id.imgGameIcon);
        TextView name = view.findViewById(R.id.txtGameName);
        TextView details = view.findViewById(R.id.txtGameDetails);
        TextView starsView = view.findViewById(R.id.txtStars);

        android.widget.ImageButton btnGameInfo = view.findViewById(R.id.btnGameInfo);

        if (currentGame != null) {
            name.setText(currentGame.getGameName());

            // 2. Εμφάνιση Provider και Risk Level
            details.setText(currentGame.getProviderName() + " | " + currentGame.getRiskLevel());

            // 3. ΔΥΝΑΜΙΚΟ ΦΟΡΤΩΜΑ ΕΙΚΟΝΑΣ ΓΙΑ ΟΛΑ ΤΑ ΠΑΙΧΝΙΔΙΑ ΣΤΟ GRID
            if (currentGame.getGameName() != null) {
                String gameNameLower = currentGame.getGameName().toLowerCase().trim();

                if (gameNameLower.contains("starburst")) {
                   imgGameIcon.setImageResource(R.drawable.img_starburst);
                } else if (gameNameLower.contains("gates") && gameNameLower.contains("olympus")) {
                    imgGameIcon.setImageResource(R.drawable.gates_of_olympus);
                } else if (gameNameLower.contains("cyber") && gameNameLower.contains("punks")) {
                    imgGameIcon.setImageResource(R.drawable.img_cyberpunksslots);
                } else if (gameNameLower.contains("fruit")) {
                    imgGameIcon.setImageResource(R.drawable.img_fruit_bonanza);
                } else if (gameNameLower.contains("gonzo")) {
                    imgGameIcon.setImageResource(R.drawable.img_gonzos_quest);
                } else if (gameNameLower.contains("thunder")) {
                    imgGameIcon.setImageResource(R.drawable.img_olympus_thunder);
                } else if (gameNameLower.contains("pharaoh")) {
                    imgGameIcon.setImageResource(R.drawable.img_pharaohs_treasure);
                } else if (gameNameLower.contains("sweet") && gameNameLower.contains("bonanza")) {
                    imgGameIcon.setImageResource(R.drawable.img_sweet_bonanza);
                } else {
                    // Προεπιλεγμένη εικόνα (Κεράσια)
                    imgGameIcon.setImageResource(R.drawable.imggameicon);
                }
            } else {
                imgGameIcon.setImageResource(R.drawable.imggameicon);
            }

            // 4. Εμφάνιση των Αστεριών (Rating)
            int rating = currentGame.getStars();
            StringBuilder starString = new StringBuilder();
            for (int j = 0; j < rating; j++) {
                starString.append("⭐");
            }
            starsView.setText(starString.toString());

            if (btnGameInfo != null) {
                btnGameInfo.setFocusable(false);
                btnGameInfo.setClickable(false);
            }
        }

        // 6. Animation εισαγωγής της κάρτας
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_row);
        view.startAnimation(animation);

        return view;
    }
}