package com.elfak.slagalica.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.fragment.NavHostFragment;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.DialogFriendInviteBinding;
import com.elfak.slagalica.helpers.NotifikacijaHelper;
import com.elfak.slagalica.service.CiklusWorker;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
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
    private ListenerRegistration nagradeListener;
    private ListenerRegistration turnirListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog currentInviteDialog;
    private boolean isInForeground = false;
    private String lastShownInviteId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotifikacijaHelper.kreirajKanale(this);
        zakaziCiklusProverу();

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

        handleNavigationIntent(getIntent());

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNavigationIntent(intent);
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null) return;
        String navigateTo = intent.getStringExtra("navigateTo");
        if (navigateTo == null) return;
        intent.removeExtra("navigateTo");
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        NavHostFragment navHost = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHost == null) return;
        try {
            if ("dnevneMisije".equals(navigateTo)) {
                navHost.getNavController().navigate(R.id.dnevneMisijeFragment);
            } else if ("turnir".equals(navigateTo)) {
                String turnirId = intent.getStringExtra("turnirId");
                if (turnirId != null) {
                    Bundle args = new Bundle();
                    args.putString("turnirId", turnirId);
                    navHost.getNavController().navigate(R.id.turnirBracketFragment, args);
                } else {
                    navHost.getNavController().navigate(R.id.turnirFragment);
                }
            }
        } catch (Exception ignored) {}
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
        startNagradeListener(myId);
        startTurnirListener(myId);

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

    private void zakaziCiklusProverу() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CiklusWorker.class, 1, java.util.concurrent.TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ciklus_provjera",
                ExistingPeriodicWorkPolicy.KEEP,
                request);
    }

    private void startNagradeListener(String uid) {
        if (nagradeListener != null) return;
        boolean[] prvaVatara = {true};
        nagradeListener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("notifikacije")
                .whereEqualTo("tip", "nagrade")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    if (prvaVatara[0]) {
                        // Preskačemo inicijalni load — reagiramo samo na nove dokumente
                        prvaVatara[0] = false;
                        return;
                    }
                    for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            Integer tokeni = dc.getDocument().getLong("tokeniNedelja") != null
                                    ? dc.getDocument().getLong("tokeniNedelja").intValue() : 0;
                            Integer tokeniMes = dc.getDocument().getLong("tokeniMesec") != null
                                    ? dc.getDocument().getLong("tokeniMesec").intValue() : 0;
                            int ukupno = tokeni + tokeniMes;
                            NotifikacijaHelper.prikaziNotifikacijuNagradu(this, ukupno > 0 ? ukupno : 1);
                        }
                    }
                });
    }

    private void startTurnirListener(String uid) {
        if (turnirListener != null) return;
        boolean[] prvaVatara = {true};
        turnirListener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("notifikacije")
                .whereEqualTo("tip", "turnir")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    if (prvaVatara[0]) { prvaVatara[0] = false; return; }
                    for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            String naslov   = dc.getDocument().getString("naslov");
                            String sadrzaj  = dc.getDocument().getString("sadrzaj");
                            String turnirId = dc.getDocument().getString("turnirId");
                            NotifikacijaHelper.prikaziNotifikacijuTurnir(this,
                                    naslov  != null ? naslov  : "Turnir",
                                    sadrzaj != null ? sadrzaj : "",
                                    turnirId);
                        }
                    }
                });
    }

    private void stopInviteListeners() {
        if (inviteListener != null) { inviteListener.remove(); inviteListener = null; }
        if (acceptedListener != null) { acceptedListener.remove(); acceptedListener = null; }
        if (rejectedListener != null) { rejectedListener.remove(); rejectedListener = null; }
        if (nagradeListener != null) { nagradeListener.remove(); nagradeListener = null; }
        if (turnirListener != null) { turnirListener.remove(); turnirListener = null; }
    }

    private void showInviteDialog(FriendInvite invite, String docId) {
        if (currentInviteDialog != null && currentInviteDialog.isShowing()) return;

        DialogFriendInviteBinding dialogBinding = DialogFriendInviteBinding.inflate(getLayoutInflater());
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setView(dialogBinding.getRoot());
        builder.setCancelable(false);

        currentInviteDialog = builder.create();
        if (currentInviteDialog.getWindow() != null) {
            currentInviteDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // Set width to 90%
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = (int) (displayMetrics.widthPixels * 0.9);
            currentInviteDialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialogBinding.tvDialogTitle.setText("Poziv na partiju");
        dialogBinding.tvMessage.setText("Poziv od igrača:");
        dialogBinding.tvUsername.setText(invite.getSenderName());
        dialogBinding.tvSubMessage.setText("Želi da odigra partiju sa tobom.");
        
        dialogBinding.flCountdown.setVisibility(View.VISIBLE);
        dialogBinding.btnAction1.setVisibility(View.VISIBLE);
        dialogBinding.btnAction2.setVisibility(View.VISIBLE);
        dialogBinding.btnAction1.setText("Prihvati");
        dialogBinding.btnAction2.setText("Odbij");
        dialogBinding.btnCancel.setVisibility(View.GONE);

        // Fetch sender data for avatar
        FirebaseFirestore.getInstance().collection("users").document(invite.getSenderId())
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String avatarUrl = snapshot.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty() && !isFinishing()) {
                            com.bumptech.glide.Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(dialogBinding.ivAvatar);
                        } else {
                            dialogBinding.ivAvatar.setImageResource(R.drawable.ic_profile);
                        }
                    }
                });

        dialogBinding.btnAction1.setOnClickListener(v -> {
            currentInviteDialog.dismiss();
            acceptInvite(invite, docId);
        });
        
        dialogBinding.btnAction2.setOnClickListener(v -> {
            currentInviteDialog.dismiss();
            rejectInvite(invite, docId);
        });

        currentInviteDialog.show();

        new android.os.CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                dialogBinding.tvCountdown.setText(String.format(Locale.getDefault(), "%02d", seconds));
                dialogBinding.pbCountdown.setProgress(seconds);
            }
            public void onFinish() {
                if (currentInviteDialog != null && currentInviteDialog.isShowing()) {
                    currentInviteDialog.dismiss();
                    rejectInvite(invite, docId);
                }
            }
        }.start();
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
