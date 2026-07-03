package com.elfak.slagalica.service;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.League;
import com.elfak.slagalica.model.User;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LeagueService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;

    public LeagueService(Context context) {
        this.context = context;
    }

    public void checkLeagueTransition(User user) {
        League current = League.getByStars(user.getZvezde());
        if (current.ordinal() != user.getLiga()) {
            League oldLeague = League.values()[user.getLiga()];
            boolean isPromotion = current.ordinal() > user.getLiga();
            
            db.collection("users").document(user.getId())
                    .update("liga", current.ordinal())
                    .addOnSuccessListener(v -> showLeagueDialog(oldLeague, current, isPromotion));
        }
    }

    private void showLeagueDialog(League oldL, League newL, boolean isPromotion) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_league_change, null);
        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMsg = dialogView.findViewById(R.id.tvMessage);
        ImageView ivTrophy = dialogView.findViewById(R.id.ivTrophy);
        
        ImageView ivOld = dialogView.findViewById(R.id.ivOldLeague);
        TextView tvOld = dialogView.findViewById(R.id.tvOldLeagueName);
        ImageView ivNew = dialogView.findViewById(R.id.ivNewLeague);
        TextView tvNew = dialogView.findViewById(R.id.tvNewLeagueName);
        
        ivOld.setImageResource(oldL.getIconRes());
        tvOld.setText(oldL.getName().toUpperCase());
        ivNew.setImageResource(newL.getIconRes());
        tvNew.setText(newL.getName().toUpperCase());

        if (isPromotion) {
            tvTitle.setText("Čestitamo!");
            tvTitle.setTextColor(0xFFFFD700); // Gold
            tvMsg.setText("Napredovali ste u novu ligu!");
            ivTrophy.setImageResource(R.drawable.ic_star);
            ivTrophy.setColorFilter(0xFFFFD700);
            dialogView.findViewById(R.id.btnOk).setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6A1B9A));
            ((TextView)dialogView.findViewById(R.id.btnOk)).setText("SUPER!");
        } else {
            tvTitle.setText("Pali ste u nižu ligu");
            tvTitle.setTextColor(0xFFE57373); // Red
            tvMsg.setText("Više nemate dovoljno zvezda za prethodnu ligu.");
            ivTrophy.setImageResource(R.drawable.ic_arrow_up);
            ivTrophy.setRotation(180);
            ivTrophy.setColorFilter(0xFFE57373);
            dialogView.findViewById(R.id.btnOk).setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE57373));
            ((TextView)dialogView.findViewById(R.id.btnOk)).setText("RAZUMEM");
        }

        dialogView.findViewById(R.id.btnOk).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void claimDailyBonus(User user) {
        long now = System.currentTimeMillis();
        long lastBonus = user.getLastTokenBonusTimestamp();
        
        if (now - lastBonus < 24 * 60 * 60 * 1000) {
            Toast.makeText(context, "Dnevni bonus već preuzet!", Toast.LENGTH_SHORT).show();
            return;
        }

        League league = League.getByStars(user.getZvezde());
        int bonusAmount = 5 + league.ordinal();

        Map<String, Object> updates = new HashMap<>();
        updates.put("tokeni", FieldValue.increment(bonusAmount));
        updates.put("lastTokenBonusTimestamp", now);

        db.collection("users").document(user.getId()).update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(context, "Preuzeto " + bonusAmount + " tokena bonusa!", Toast.LENGTH_LONG).show();
                });
    }
}
