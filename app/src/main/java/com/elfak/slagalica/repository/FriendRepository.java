package com.elfak.slagalica.repository;

import com.elfak.slagalica.model.FriendInvite;
import com.elfak.slagalica.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class FriendRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public interface OnFriendsLoaded { void onLoaded(List<User> friends); }
    public interface OnSearchFinished { void onFound(List<User> users); }
    public interface OnInviteSent { void onSent(String inviteId); }

    public void addFriendMutual(String friendId, UserRepository.OnSuccessListener listener) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        if (uid.equals(friendId)) return;

        WriteBatch batch = db.batch();
        batch.update(db.collection("users").document(uid), "prijateljiIds", FieldValue.arrayUnion(friendId));
        batch.update(db.collection("users").document(friendId), "prijateljiIds", FieldValue.arrayUnion(uid));

        batch.commit().addOnSuccessListener(v -> listener.onSuccess());
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

    public void createInvite(String receiverId, String senderName, String gameId,
                             OnInviteSent onSent, UserRepository.OnErrorListener onError) {
        if (auth.getCurrentUser() == null) return;
        String senderId = auth.getCurrentUser().getUid();
        FriendInvite invite = new FriendInvite(senderId, senderName, receiverId);
        invite.setGameId(gameId);
        db.collection("invites")
                .add(invite)
                .addOnSuccessListener(ref -> { if (onSent != null) onSent.onSent(ref.getId()); })
                .addOnFailureListener(e -> { if (onError != null) onError.onError(e.getMessage()); });
    }
}