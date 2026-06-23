package com.elfak.slagalica.activities;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

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
import com.elfak.slagalica.model.Notifikacija;
import com.elfak.slagalica.repository.PartijaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int KOD_DOZVOLE_NOTIFIKACIJA = 100;
    private ListenerRegistration inviteListener;
    private ListenerRegistration acceptedListener;
    private ListenerRegistration rejectedListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog currentInviteDialog;
    private boolean isInForeground = false;
    private String lastShownInviteId = null;

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

    @Override
    protected void onStart() {
        super.onStart();
        isInForeground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isInForeground = false;
        updateOnlineStatus(false);
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
                    if (snapshots == null || snapshots.isEmpty()) return;
                    String docId = snapshots.getDocuments().get(0).getId();
                    if (docId.equals(lastShownInviteId)) return;
                    FriendInvite invite = snapshots.getDocuments().get(0).toObject(FriendInvite.class);
                    if (invite == null) return;
                    lastShownInviteId = docId;
                    if (isInForeground) {
                        showInviteDialog(invite, docId);
                    } else {
                        NotifikacijaHelper.prikaziNotifikacijuPoziv(this, invite.getSenderName());
                        sacuvajNotifikacijuUBazu(myId, invite.getSenderName());
                    }
                });

        acceptedListener = FirebaseFirestore.getInstance().collection("invites")
                .whereEqualTo("senderId", myId)
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null || snapshots.isEmpty()) return;
                    String docId = snapshots.getDocuments().get(0).getId();
                    String gameId = snapshots.getDocuments().get(0).getString("gameId");
                    if (gameId != null) {
                        FirebaseFirestore.getInstance().collection("invites").document(docId).delete();
                        navigateToGame(gameId, true);
                    }
                });

        rejectedListener = FirebaseFirestore.getInstance().collection("invites")
                .whereEqualTo("senderId", myId)
                .whereEqualTo("status", "REJECTED")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null || snapshots.isEmpty()) return;
                    String docId = snapshots.getDocuments().get(0).getId();
                    FirebaseFirestore.getInstance().collection("invites").document(docId).delete();
                    runOnUiThread(() ->
                        Toast.makeText(this, "Poziv je odbijen", Toast.LENGTH_SHORT).show()
                    );
                });
    }

    private void stopInviteListeners() {
        if (inviteListener != null) { inviteListener.remove(); inviteListener = null; }
        if (acceptedListener != null) { acceptedListener.remove(); acceptedListener = null; }
        if (rejectedListener != null) { rejectedListener.remove(); rejectedListener = null; }
    }

    private void showInviteDialog(FriendInvite invite, String docId) {
        if (currentInviteDialog != null && currentInviteDialog.isShowing()) return;

        currentInviteDialog = new AlertDialog.Builder(this)
                .setTitle("Poziv na partiju")
                .setMessage(invite.getSenderName() + " te poziva na prijateljsku partiju")
                .setPositiveButton("Prihvati", (d, w) -> acceptInvite(invite, docId))
                .setNegativeButton("Odbij", (d, w) -> rejectInvite(invite, docId))
                .setCancelable(false)
                .create();

        currentInviteDialog.show();
        handler.postDelayed(() -> {
            if (currentInviteDialog != null && currentInviteDialog.isShowing()) {
                currentInviteDialog.dismiss();
                rejectInvite(invite, docId);
            }
        }, 10000);
    }

    private void acceptInvite(FriendInvite invite, String docId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String myId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String gameId = invite.getGameId();
        if (gameId == null) return;

        FirebaseFirestore.getInstance().collection("users").document(myId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String myName = snapshot.getString("korisnickoIme");
                    if (myName == null || myName.isEmpty()) myName = "Igrač 2";
                    String finalMyName = myName;

                    new PartijaRepository().pridruziSePrijateljskojPartiji(
                        gameId, myId, finalMyName,
                        () -> {
                            FirebaseFirestore.getInstance().collection("invites")
                                    .document(docId).update("status", "ACCEPTED");
                            navigateToGame(gameId, false);
                        },
                        err -> Toast.makeText(this, "Greška pri prihvatanju", Toast.LENGTH_SHORT).show()
                    );
                });
    }

    private void rejectInvite(FriendInvite invite, String docId) {
        FirebaseFirestore.getInstance().collection("invites")
                .document(docId).update("status", "REJECTED");
        String gameId = invite.getGameId();
        if (gameId != null) {
            FirebaseFirestore.getInstance().collection("partije")
                    .document(gameId).update("status", "NAPUSTENA");
        }
    }

    private void navigateToGame(String partijaId, boolean jeIgrac1) {
        Bundle args = new Bundle();
        args.putString("partijaId", partijaId);
        args.putBoolean("jeIgrac1", jeIgrac1);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navHostFragment.getNavController().navigate(R.id.igraFragment, args);
        }
    }

    private void sacuvajNotifikacijuUBazu(String uid, String senderName) {
        String datumVrijeme = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        Notifikacija n = new Notifikacija("poziv", "Poziv na partiju",
                senderName + " te je pozvao na prijateljsku partiju.",
                datumVrijeme, false);
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("notifikacije")
                .add(n);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopInviteListeners();
    }
}
