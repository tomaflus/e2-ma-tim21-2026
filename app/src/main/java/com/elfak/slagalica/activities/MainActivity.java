package com.elfak.slagalica.activities;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.fragment.NavHostFragment;

import com.elfak.slagalica.R;
import com.elfak.slagalica.helpers.NotifikacijaHelper;
import com.elfak.slagalica.model.FriendInvite;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int KOD_DOZVOLE_NOTIFIKACIJA = 100;
    private ListenerRegistration inviteListener;
    private ListenerRegistration acceptedListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog currentInviteDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotifikacijaHelper.kreirajKanale(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insetsController.hide(WindowInsetsCompat.Type.systemBars());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        KOD_DOZVOLE_NOTIFIKACIJA);
            }
        }

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                updateOnlineStatus(true);
                startInviteListeners();
            } else {
                updateOnlineStatus(false);
                stopInviteListeners();
            }
        });
    }

    private void updateOnlineStatus(boolean online) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).update("online", online);
    }

    private void startInviteListeners() {
        if (inviteListener != null) return;
        String myId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        inviteListener = FirebaseFirestore.getInstance().collection("invites")
                .whereEqualTo("receiverId", myId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        FriendInvite invite = snapshots.getDocuments().get(0).toObject(FriendInvite.class);
                        if (invite != null) showInviteDialog(invite, snapshots.getDocuments().get(0).getId());
                    }
                });

        acceptedListener = FirebaseFirestore.getInstance().collection("invites")
                .whereEqualTo("senderId", myId)
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        String gameId = snapshots.getDocuments().get(0).getString("gameId");
                        if (gameId != null) {
                            FirebaseFirestore.getInstance().collection("invites").document(snapshots.getDocuments().get(0).getId()).delete();
                            navigateToGame(gameId);
                        }
                    }
                });
    }

    private void stopInviteListeners() {
        if (inviteListener != null) { inviteListener.remove(); inviteListener = null; }
        if (acceptedListener != null) { acceptedListener.remove(); acceptedListener = null; }
    }

    private void showInviteDialog(FriendInvite invite, String docId) {
        if (currentInviteDialog != null && currentInviteDialog.isShowing()) return;

        currentInviteDialog = new AlertDialog.Builder(this)
                .setTitle("Poziv na partiju")
                .setMessage("Poziv od " + invite.getSenderName() + " za prijateljsku partiju")
                .setPositiveButton("Prihvati", (d, w) -> acceptInvite(invite, docId))
                .setNegativeButton("Odbij", (d, w) -> rejectInvite(docId))
                .setCancelable(false)
                .create();

        currentInviteDialog.show();
        handler.postDelayed(() -> {
            if (currentInviteDialog != null && currentInviteDialog.isShowing()) {
                currentInviteDialog.dismiss();
                rejectInvite(docId);
            }
        }, 10000);
    }

    private void acceptInvite(FriendInvite invite, String docId) {
        String partijaId = UUID.randomUUID().toString();
        Map<String, Object> game = new HashMap<>();
        game.put("partijaId", partijaId);
        game.put("player1", invite.getSenderId());
        game.put("player2", invite.getReceiverId());
        game.put("status", "STARTED");

        FirebaseFirestore.getInstance().collection("partije").document(partijaId).set(game).addOnSuccessListener(v -> {
            FirebaseFirestore.getInstance().collection("invites").document(docId).update("status", "ACCEPTED", "gameId", partijaId);
            navigateToGame(partijaId);
        });
    }

    private void rejectInvite(String docId) {
        FirebaseFirestore.getInstance().collection("invites").document(docId).update("status", "REJECTED");
    }

    private void navigateToGame(String partijaId) {
        Bundle args = new Bundle();
        args.putString("partijaId", partijaId);
        
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navHostFragment.getNavController().navigate(R.id.igraFragment, args);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateOnlineStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopInviteListeners();
    }
}