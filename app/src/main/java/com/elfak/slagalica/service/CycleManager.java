package com.elfak.slagalica.service;

import android.content.Context;
import android.util.Log;

import com.elfak.slagalica.model.User;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class CycleManager {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "CycleManager";

    public interface OnCycleUpdateListener {
        void onUpdateComplete();
    }

    public void checkAndInitializeCycle(User user, OnCycleUpdateListener listener) {
        String currentCycleId = CiklusUtil.trenutniMesecId();
        String userCycleId = user.getMesecCiklusId();

        if (userCycleId == null || userCycleId.isEmpty()) {
            // First time initialization
            initializeFirstTime(user, currentCycleId, listener);
        } else if (!userCycleId.equals(currentCycleId)) {
            // Cycle changed
            handleCycleChange(user, currentCycleId, listener);
        } else {
            listener.onUpdateComplete();
        }
    }

    private void initializeFirstTime(User user, String cycleId, OnCycleUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("mesecCiklusId", cycleId);
        updates.put("nedeljaCiklusId", CiklusUtil.trenutniNedeljaId());
        
        // Random persistent coordinates for Student 2 KO
        com.elfak.slagalica.model.Region r = com.elfak.slagalica.model.Region.getByName(user.getRegion());
        double[] coords = generateRandomCoords(r);
        updates.put("latitude", coords[0]);
        updates.put("longitude", coords[1]);

        db.collection("users").document(user.getId()).update(updates)
                .addOnSuccessListener(v -> listener.onUpdateComplete())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to initialize cycle", e);
                    listener.onUpdateComplete();
                });
    }

    private void handleCycleChange(User user, String newCycleId, OnCycleUpdateListener listener) {
        WriteBatch batch = db.batch();
        Map<String, Object> updates = new HashMap<>();
        
        // 6e: 30% star penalty for non-ranked users
        if (!user.isRangiranMesec()) {
            int newStars = (int) (user.getZvezde() * 0.7);
            updates.put("zvezde", newStars);
            updates.put("liga", com.elfak.slagalica.model.League.getByStars(newStars).ordinal());
        }

        // Reset monthly stars
        updates.put("mesecneZvezde", 0);
        updates.put("mesecCiklusId", newCycleId);
        updates.put("rangiranMesec", false);

        batch.update(db.collection("users").document(user.getId()), updates);
        
        batch.commit()
                .addOnSuccessListener(v -> listener.onUpdateComplete())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update cycle change", e);
                    listener.onUpdateComplete();
                });
    }

    private double[] generateRandomCoords(com.elfak.slagalica.model.Region r) {
        java.util.Random random = new java.util.Random();
        double lat = r.getCenterLat() + (random.nextDouble() - 0.5) * r.getRadius();
        double lon = r.getCenterLon() + (random.nextDouble() - 0.5) * r.getRadius();
        return new double[]{lat, lon};
    }
}