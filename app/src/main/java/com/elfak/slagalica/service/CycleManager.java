package com.elfak.slagalica.service;

import android.content.Context;
import android.util.Log;

import com.elfak.slagalica.model.User;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
        Random random = new Random();
        double lat = r.getCenterLat() + (random.nextDouble() - 0.5) * r.getRadius();
        double lon = r.getCenterLon() + (random.nextDouble() - 0.5) * r.getRadius();
        return new double[]{lat, lon};
    }

    /**
     * DEBUG-ONLY: Seeds test data for Student 2 KO verification.
     * Call this manually from a debug button or initialization block.
     */
    public void seedTestData(String currentUid) {
        WriteBatch batch = db.batch();
        Random random = new Random();

        // 1. Create test users for all 5 regions to populate leaderboard
        for (com.elfak.slagalica.model.Region r : com.elfak.slagalica.model.Region.values()) {
            for (int i = 1; i <= 3; i++) {
                String testId = "test_user_" + r.name() + "_" + i;
                Map<String, Object> u = new HashMap<>();
                u.put("korisnickoIme", "Test " + r.getShortName() + " " + i);
                u.put("region", r.getFullName());
                u.put("mesecneZvezde", random.nextInt(500));
                u.put("zvezde", random.nextInt(2000));
                u.put("mesecCiklusId", CiklusUtil.trenutniMesecId());
                double[] coords = generateRandomCoords(r);
                u.put("latitude", coords[0]);
                u.put("longitude", coords[1]);
                u.put("liga", com.elfak.slagalica.model.League.getByStars((int)u.get("zvezde")).ordinal());
                
                batch.set(db.collection("users").document(testId), u);
            }
        }

        // 2. Set current user stats for specific tests
        Map<String, Object> me = new HashMap<>();
        me.put("previousCycleRegionRank", 1); // For gold frame
        me.put("lastTokenBonusTimestamp", 0); // Make bonus available
        me.put("zvezde", 99); // For threshold testing
        batch.update(db.collection("users").document(currentUid), me);

        batch.commit().addOnSuccessListener(v -> Log.d(TAG, "Test data seeded successfully"));
    }
}