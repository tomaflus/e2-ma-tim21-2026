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

    /**
     * Checks if the user has transitioned to a new monthly cycle.
     * If so, resets their monthly stars and assigns a rank from the previous cycle.
     */
    public void handleCycleTransition(User user, Runnable onComplete) {
        String currentCycle = CiklusUtil.trenutniMesecId();
        if (user.getMesecCiklusId() != null && user.getMesecCiklusId().equals(currentCycle)) {
            onComplete.run();
            return;
        }

        // Cycle changed! 
        String previousCycle = user.getMesecCiklusId();
        
        // In a real production app, we would fetch the final ranking of the previous cycle.
        // For this project, we'll calculate it from the users collection using the previous cycle ID.
        db.collection("users")
            .whereEqualTo("mesecCiklusId", previousCycle)
            .get()
            .addOnSuccessListener(snapshots -> {
                Map<Region, Integer> regionStars = new HashMap<>();
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
                
                int rank = 0;
                for (int i = 0; i < sorted.size(); i++) {
                    if (sorted.get(i).getKey().getFullName().equals(user.getRegion())) {
                        rank = i + 1;
                        break;
                    }
                }
                
                // Update user with new cycle data
                Map<String, Object> updates = new HashMap<>();
                updates.put("mesecCiklusId", currentCycle);
                updates.put("mesecneZvezde", 0);
                updates.put("previousCycleRegionRank", rank);
                
                db.collection("users").document(user.getId()).update(updates)
                    .addOnSuccessListener(aVoid -> onComplete.run())
                    .addOnFailureListener(e -> onComplete.run());
            })
            .addOnFailureListener(e -> onComplete.run());
    }

    public void getRegionStats(Region r, OnRegionStatsLoaded listener) {
        // 1. Get current stats from users collection
        db.collection("users").whereEqualTo("region", r.getFullName()).get().addOnSuccessListener(snapshots -> {
            final int total = snapshots.size();
            int activeCount = 0;
            for (QueryDocumentSnapshot doc : snapshots) {
                Boolean online = doc.getBoolean("online");
                if (online != null && online) activeCount++;
            }
            final int active = activeCount;
            
            // 2. Get historical placements from region_stats collection
            db.collection("region_stats").document(r.name()).get().addOnSuccessListener(doc -> {
                int first = 0, second = 0, third = 0;
                if (doc.exists()) {
                    Long f = doc.getLong("firstPlaces");
                    Long s = doc.getLong("secondPlaces");
                    Long t = doc.getLong("thirdPlaces");
                    if (f != null) first = f.intValue();
                    if (s != null) second = s.intValue();
                    if (t != null) third = t.intValue();
                }
                listener.onLoaded(active, total, first, second, third);
            }).addOnFailureListener(e -> listener.onLoaded(active, total, 0, 0, 0));
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

    // DEBUG SEED METHODS
    public void seedDebugData(Runnable onComplete) {
        if (!com.elfak.slagalica.BuildConfig.DEBUG) {
            onComplete.run();
            return;
        }

        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(userDoc -> {
                String userRegion = userDoc.getString("region");
                executeSeedBatch(userRegion, onComplete);
            }).addOnFailureListener(e -> executeSeedBatch(null, onComplete));
        } else {
            executeSeedBatch(null, onComplete);
        }
    }

    private void seedTestUser(com.google.firebase.firestore.WriteBatch batch, String username, int rank, String cycle) {
        String id = "debug_" + username.toLowerCase();
        org.osmdroid.util.GeoPoint p = com.elfak.slagalica.util.RegionUtil.getRandomPointInRegion(Region.BEOGRAD.getFullName());
        Map<String, Object> u = new HashMap<>();
        u.put("id", id);
        u.put("korisnickoIme", username);
        u.put("email", username.toLowerCase() + "@test.com");
        u.put("region", Region.BEOGRAD.getFullName());
        u.put("previousCycleRegionRank", rank);
        u.put("mesecCiklusId", cycle);
        u.put("tokeni", 100);
        u.put("zvezde", 1000);
        u.put("online", true);
        u.put("latitude", p.getLatitude());
        u.put("longitude", p.getLongitude());
        batch.set(db.collection("users").document(id), u);
    }

    private void executeSeedBatch(String userRegion, Runnable onComplete) {
        String currentCycle = com.elfak.slagalica.util.CiklusUtil.trenutniMesecId();
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        java.util.Random random = new java.util.Random(42);

        // 1. Logically valid historical region stats
        int numCycles = 15;
        Map<Region, Integer> firsts = new HashMap<>();
        Map<Region, Integer> seconds = new HashMap<>();
        Map<Region, Integer> thirds = new HashMap<>();
        for (Region r : Region.values()) {
            firsts.put(r, 0);
            seconds.put(r, 0);
            thirds.put(r, 0);
        }

        List<Region> regionList = new ArrayList<>(java.util.Arrays.asList(Region.values()));
        Region prevFirst = null, prevSecond = null, prevThird = null;

        for (int i = 0; i < numCycles; i++) {
            Collections.shuffle(regionList, random);
            Region r1 = regionList.get(0);
            Region r2 = regionList.get(1);
            Region r3 = regionList.get(2);
            firsts.put(r1, firsts.get(r1) + 1);
            seconds.put(r2, seconds.get(r2) + 1);
            thirds.put(r3, thirds.get(r3) + 1);

            if (i == numCycles - 1) {
                prevFirst = r1;
                prevSecond = r2;
                prevThird = r3;
            }
        }

        for (Region r : Region.values()) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("firstPlaces", firsts.get(r));
            stats.put("secondPlaces", seconds.get(r));
            stats.put("thirdPlaces", thirds.get(r));
            batch.set(db.collection("region_stats").document(r.name()), stats);
        }

        // 2. Seed 15 players per region
        for (Region r : Region.values()) {
            for (int i = 0; i < 15; i++) {
                String id = "fake_user_" + r.name() + "_" + i;
                org.osmdroid.util.GeoPoint p = com.elfak.slagalica.util.RegionUtil.getRandomPointInRegion(r.getFullName());

                Map<String, Object> fakeUser = new HashMap<>();
                fakeUser.put("id", id);
                fakeUser.put("korisnickoIme", "Igrač " + r.getShortName().charAt(0) + (i + 1));
                fakeUser.put("region", r.getFullName());
                fakeUser.put("mesecneZvezde", 100 + random.nextInt(2000));
                fakeUser.put("mesecCiklusId", currentCycle);
                fakeUser.put("latitude", p.getLatitude());
                fakeUser.put("longitude", p.getLongitude());
                fakeUser.put("online", random.nextBoolean());
                fakeUser.put("liga", random.nextInt(5) + 1);
                fakeUser.put("zvezde", 500 + random.nextInt(5000));
                fakeUser.put("previousCycleRegionRank", 0);

                batch.set(db.collection("users").document(id), fakeUser);
            }
        }

        // 3. Specific Debug Test Users
        seedTestUser(batch, "GoldTestUser", 1, currentCycle);
        seedTestUser(batch, "SilverTestUser", 2, currentCycle);
        seedTestUser(batch, "BronzeTestUser", 3, currentCycle);

        // 4. Update current user rank based on the LAST cycle from history
        if (userRegion != null) {
            int myRank = 0;
            if (userRegion.equals(prevFirst.getFullName())) myRank = 1;
            else if (userRegion.equals(prevSecond.getFullName())) myRank = 2;
            else if (userRegion.equals(prevThird.getFullName())) myRank = 3;

            batch.update(db.collection("users").document(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()),
                    "previousCycleRegionRank", myRank);
        }

        batch.commit().addOnSuccessListener(unused -> onComplete.run());
    }
}
