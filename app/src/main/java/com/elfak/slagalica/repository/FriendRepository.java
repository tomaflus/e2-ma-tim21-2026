package com.elfak.slagalica.repository;

import com.elfak.slagalica.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public interface OnFriendsLoaded { void onLoaded(List<User> friends); }
    public interface OnSearchFinished { void onFound(List<User> users); }
    public interface OnSuccessListener { void onSuccess(); }

    public void addFriendMutual(String friendId, OnSuccessListener listener) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        if (uid.equals(friendId)) return;

        WriteBatch batch = db.batch();
        batch.update(db.collection("users").document(uid), "prijateljiIds", FieldValue.arrayUnion(friendId));
        batch.update(db.collection("users").document(friendId), "prijateljiIds", FieldValue.arrayUnion(uid));

        batch.commit().addOnSuccessListener(v -> {
            if (listener != null) listener.onSuccess();
        });
    }

    public void searchUsers(String query, OnSearchFinished listener) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("korisnickoIme", query)
                .whereLessThanOrEqualTo("korisnickoIme", query + "\uf8ff")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<User> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User user = doc.toObject(User.class);
                        user.setId(doc.getId());
                        results.add(user);
                    }
                    listener.onFound(results);
                });
    }

    public void loadFriends(List<String> ids, OnFriendsLoaded listener) {
        if (ids == null || ids.isEmpty()) {
            listener.onLoaded(new ArrayList<>());
            return;
        }
        db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), ids).get().addOnSuccessListener(snapshots -> {
            List<User> friends = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                User user = doc.toObject(User.class);
                user.setId(doc.getId());
                friends.add(user);
            }
            listener.onLoaded(friends);
        });
    }
    
    public void addFriendByUsername(String username, OnSuccessListener listener) {
        db.collection("users").whereEqualTo("korisnickoIme", username).limit(1).get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                addFriendMutual(snapshots.getDocuments().get(0).getId(), listener);
            }
        });
    }

    public interface OnInviteCreatedListener { void onCreated(String inviteId); }

    public void createInvite(String receiverId, String senderName, String gameId, OnInviteCreatedListener listener) {
        if (auth.getCurrentUser() == null) return;
        String senderId = auth.getCurrentUser().getUid();
        com.elfak.slagalica.model.FriendInvite invite = new com.elfak.slagalica.model.FriendInvite(senderId, senderName, receiverId);
        if (gameId != null) invite.setGameId(gameId);
        db.collection("invites").add(invite).addOnSuccessListener(ref -> {
            if (listener != null) listener.onCreated(ref.getId());
        });
    }

    public void cancelInvite(String inviteId, OnSuccessListener listener) {
        db.collection("invites").document(inviteId).delete().addOnSuccessListener(v -> {
            if (listener != null) listener.onSuccess();
        });
    }

    public void respondToInvite(String inviteId, String status, String gameId, OnSuccessListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        if (gameId != null) updates.put("gameId", gameId);
        db.collection("invites").document(inviteId).update(updates).addOnSuccessListener(v -> {
            if (listener != null) listener.onSuccess();
        });
    }
}