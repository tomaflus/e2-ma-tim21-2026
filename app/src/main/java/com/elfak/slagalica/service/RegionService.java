package com.elfak.slagalica.service;

import com.elfak.slagalica.model.Region;
import com.google.firebase.firestore.FirebaseFirestore;
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

    public void calculateRegionalRanking(OnRankLoaded listener) {
        db.collection("users").get().addOnSuccessListener(snapshots -> {
            Map<Region, Integer> regionStars = new HashMap<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                String regionName = doc.getString("region");
                Long stars = doc.getLong("zvezde");
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
            int first = total / 10; 
            int second = total / 8;
            int third = total / 5;
            listener.onLoaded(active, total, first, second, third);
        });
    }
}