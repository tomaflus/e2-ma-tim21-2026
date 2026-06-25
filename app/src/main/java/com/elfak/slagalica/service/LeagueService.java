package com.elfak.slagalica.service;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.League;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.UserRepository;
import com.elfak.slagalica.util.CiklusUtil;
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
            boolean isPromotion = current.ordinal() > user.getLiga();
            
            db.collection("users").document(user.getId())
                    .update("liga", current.ordinal())
                    .addOnSuccessListener(v -> showLeagueDialog(current, isPromotion));
        }
    }

    private void showLeagueDialog(League league, boolean isPromotion) {
        String title = isPromotion ? "Promocija u ligu!" : "Ispadanje iz lige";
        String msg = isPromotion 
                ? context.getString(R.string.msg_league_up, league.getName())
                : context.getString(R.string.msg_league_down, league.getName());
        
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("U redu", null)
                .setIcon(league.getIconRes())
                .show();
    }

    public void claimDailyBonus(User user) {
        long now = System.currentTimeMillis();
        long lastBonus = user.getLastTokenBonusTimestamp();
        
        // Simple once-per-day check (24h)
        if (now - lastBonus < 24 * 60 * 60 * 1000) {
            Toast.makeText(context, "Dnevni bonus već preuzet!", Toast.LENGTH_SHORT).show();
            return;
        }

        League league = League.values()[user.getLiga()];
        int bonusAmount = 5 + league.ordinal(); // 5 base + league level (0 to 5)

        Map<String, Object> updates = new HashMap<>();
        updates.put("tokeni", FieldValue.increment(bonusAmount));
        updates.put("lastTokenBonusTimestamp", now);

        db.collection("users").document(user.getId()).update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(context, "Preuzeto " + bonusAmount + " tokena bonusa!", Toast.LENGTH_LONG).show();
                });
    }
}