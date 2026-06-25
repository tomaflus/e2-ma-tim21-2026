package com.elfak.slagalica.service;

import com.elfak.slagalica.model.Region;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnRankLoaded { void onLoaded(List<Map.Entry<Region, Integer>> ranking); }
    public interface OnRegionStatsLoaded { void onLoaded(int active, int total, int first, int second, int third); }
    public interface OnUsersLoaded { void onLoaded(List<User> users); }

    public void calculateRegionalRanking(OnRankLoaded listener) {
        String currentCycle = CiklusUtil.trenutniMesecId();
        db.collection("users")
                .whereEqualTo("mesecCiklusId", currentCycle)
                .get()
                .addOnSuccessListener(snapshots -> {
                    Map<Region, Integer> regionStars = new HashMap<>();
                    // Initialize all regions with 0 stars
                    for (Region r : Region.values()) regionStars.put(r, 0);

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String regionName = doc.getString("region");
                        Long stars = doc.getLong("mesecneZvezde");
                        if (regionName != null && stars != null) {
                            Region r = Region.getByName(regionName);
                            regionStars.put(r, regionStars.getOrDefault(r, 0) + stars.intValue());
                        }
                    }
                    List<Map.Entry<Region, Integer>> sorted = new ArrayList<>(regionStars.entrySet());
                    Collections.sort(sorted, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
                    listener.onLoaded(sorted);
                });
    }

    public void getRegionStats(Region r, OnRegionStatsLoaded listener) {
        db.collection("users").whereEqualTo("region", r.getFullName()).get().addOnSuccessListener(snapshots -> {
            int total = snapshots.size();
            int active = 0;
            for (QueryDocumentSnapshot doc : snapshots) {
                Boolean online = doc.getBoolean("online");
                if (online != null && online) active++;
            }
            
            // Mocking top-3 counts for now as there's no historical stats collection mentioned
            // In a real app, this would be fetched from a 'region_history' collection
            int first = total / 15; 
            int second = total / 12;
            int third = total / 10;
            listener.onLoaded(active, total, first, second, third);
        });
    }

    public void loadUsersWithCoords(OnUsersLoaded listener) {
        db.collection("users")
                .whereGreaterThan("latitude", 0)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User user = doc.toObject(User.class);
                        user.setId(doc.getId());
                        users.add(user);
                    }
                    listener.onLoaded(users);
                });
    }
}