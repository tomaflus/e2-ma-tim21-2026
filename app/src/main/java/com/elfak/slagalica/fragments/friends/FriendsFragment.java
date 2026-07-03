package com.elfak.slagalica.fragments.friends;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.DialogFriendInviteBinding;
import com.elfak.slagalica.databinding.FragmentFriendsBinding;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.FriendRepository;
import com.elfak.slagalica.repository.PartijaRepository;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FriendsFragment extends Fragment {
    private FragmentFriendsBinding binding;
    private FriendRepository friendRepository;
    private AuthRepository authRepository;
    private PartijaRepository partijaRepository;
    private FriendsAdapter adapter;
    private String mojeIme = "";
    private ListenerRegistration inviteStatusListener;
    private android.app.AlertDialog activeInviteDialog;
    private CountDownTimer countDownTimer;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    dodajPrijatelja(result.getContents());
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        friendRepository = new FriendRepository();
        authRepository = new AuthRepository();
        partijaRepository = new PartijaRepository();

        adapter = new FriendsAdapter(new ArrayList<>(), this::posaljiPoziv);

        binding.rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvFriends.setAdapter(adapter);

        ucitajPrijatelje();

        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                izvrsiPretragu();
                return true;
            }
            return false;
        });

        binding.btnSearch.setOnClickListener(v -> izvrsiPretragu());

        binding.btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Usmerite QR kod u okvir");
            options.setBeepEnabled(false);
            options.setOrientationLocked(false); // Allow rotation to vertical
            barcodeLauncher.launch(options);
        });

        binding.btnDebugSeed.setOnClickListener(v -> seedDebugData());
    }

    private void izvrsiPretragu() {
        String q = binding.etSearch.getText().toString().trim();
        sakrijTastaturu();
        if (!q.isEmpty()) {
            friendRepository.searchUsers(q, users -> {
                if (users.isEmpty()) {
                    Toast.makeText(getContext(), "Nije pronađen nijedan igrač.", Toast.LENGTH_SHORT).show();
                }
                adapter.updateData(users);
            });
        } else {
            ucitajPrijatelje();
        }
    }

    private void sakrijTastaturu() {
        if (getView() != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private void posaljiPoziv(User receiver) {
        if (authRepository.trenutniKorisnik() == null || getContext() == null) return;
        String ime = mojeIme.isEmpty() ? "Igrač" : mojeIme;
        
        showInviteDialog(receiver, "SENDING", null);

        partijaRepository.kreirajPrijateljskuPartiju(
            ime,
            gameId -> friendRepository.createInvite(
                receiver.getId(), ime, gameId,
                inviteId -> listenForInviteStatus(inviteId, receiver, gameId)
            ),
            err -> {
                if (activeInviteDialog != null) activeInviteDialog.dismiss();
                Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void listenForInviteStatus(String inviteId, User friend, String gameId) {
        inviteStatusListener = FirebaseFirestore.getInstance().collection("invites")
            .document(inviteId)
            .addSnapshotListener((snapshot, e) -> {
                if (snapshot == null || !snapshot.exists() || binding == null) return;
                String status = snapshot.getString("status");
                if ("ACCEPTED".equals(status)) {
                    stopInviteStatusListener();
                    if (activeInviteDialog != null) activeInviteDialog.dismiss();
                    navigateToGame(gameId, true);
                } else if ("REJECTED".equals(status)) {
                    stopInviteStatusListener();
                    showInviteDialog(friend, "DECLINED", null);
                }
            });
    }

    private void stopInviteStatusListener() {
        if (inviteStatusListener != null) {
            inviteStatusListener.remove();
            inviteStatusListener = null;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void showInviteDialog(User friend, String type, String inviteId) {
        if (getContext() == null) return;
        
        DialogFriendInviteBinding dialogBinding = DialogFriendInviteBinding.inflate(getLayoutInflater());
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext(), R.style.CustomDialogTheme);
        builder.setView(dialogBinding.getRoot());
        builder.setCancelable(false);

        if (activeInviteDialog != null && activeInviteDialog.isShowing()) {
            activeInviteDialog.dismiss();
        }
        activeInviteDialog = builder.create();
        if (activeInviteDialog.getWindow() != null) {
            activeInviteDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Set width to 90%
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = (int) (displayMetrics.widthPixels * 0.9);
            activeInviteDialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if (friend != null) {
            dialogBinding.tvUsername.setText(friend.getKorisnickoIme());
            if (friend.getAvatarUrl() != null && !friend.getAvatarUrl().isEmpty()) {
                 com.bumptech.glide.Glide.with(this)
                     .load(friend.getAvatarUrl())
                     .placeholder(R.drawable.ic_profile)
                     .circleCrop()
                     .into(dialogBinding.ivAvatar);
            } else {
                 dialogBinding.ivAvatar.setImageResource(R.drawable.ic_profile);
            }
        }

        switch (type) {
            case "SENDING":
                dialogBinding.tvMessage.setText("Šalješ poziv igraču");
                dialogBinding.tvSubMessage.setText("Čekamo odgovor...");
                dialogBinding.flCountdown.setVisibility(View.VISIBLE);
                dialogBinding.btnCancel.setVisibility(View.VISIBLE);
                dialogBinding.btnCancel.setOnClickListener(v -> {
                    if (inviteId != null) friendRepository.cancelInvite(inviteId, null);
                    stopInviteStatusListener();
                    activeInviteDialog.dismiss();
                });
                startCountdown(dialogBinding);
                break;
            case "DECLINED":
                dialogBinding.tvMessage.setText("Poziv odbijen");
                dialogBinding.tvSubMessage.setText("Igrač je odbio vaš poziv.");
                dialogBinding.flCountdown.setVisibility(View.GONE);
                dialogBinding.btnCancel.setVisibility(View.GONE);
                dialogBinding.btnOk.setVisibility(View.VISIBLE);
                dialogBinding.ivResultIcon.setVisibility(View.VISIBLE);
                dialogBinding.ivResultIcon.setImageResource(android.R.drawable.ic_delete);
                dialogBinding.btnOk.setOnClickListener(v -> {
                    activeInviteDialog.dismiss();
                });
                break;
            case "EXPIRED":
                dialogBinding.tvMessage.setText("Poziv istekao");
                dialogBinding.tvSubMessage.setText("Igrač nije odgovorio na vreme.");
                dialogBinding.flCountdown.setVisibility(View.GONE);
                dialogBinding.btnCancel.setVisibility(View.GONE);
                dialogBinding.btnOk.setVisibility(View.VISIBLE);
                dialogBinding.ivResultIcon.setVisibility(View.VISIBLE);
                dialogBinding.ivResultIcon.setImageResource(android.R.drawable.ic_lock_idle_alarm);
                dialogBinding.btnOk.setOnClickListener(v -> {
                    activeInviteDialog.dismiss();
                });
                break;
        }

        activeInviteDialog.show();
    }

    private void startCountdown(DialogFriendInviteBinding dialogBinding) {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                dialogBinding.tvCountdown.setText(String.format(java.util.Locale.getDefault(), "%02d", seconds));
                dialogBinding.pbCountdown.setProgress(seconds);
            }
            public void onFinish() {
                dialogBinding.tvCountdown.setText("00");
                dialogBinding.pbCountdown.setProgress(0);
                showInviteDialog(null, "EXPIRED", null);
            }
        }.start();
    }

    private void navigateToGame(String gameId, boolean jeIgrac1) {
        Bundle args = new Bundle();
        args.putString("partijaId", gameId);
        args.putBoolean("jeIgrac1", jeIgrac1);
        NavHostFragment.findNavController(this).navigate(R.id.igraFragment, args);
    }

    private void seedDebugData() {
        if (authRepository.trenutniKorisnik() == null) return;
        String myUid = authRepository.trenutniKorisnik().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Random r = new Random();
        
        // Specific user "Boris"
        User boris = new User("debug_boris", "borkolahos1@gmail.com", "Boris", "Beograd");
        boris.setZvezde(1748);
        boris.setMesecniBodovi(97);
        boris.setOnline(true);
        boris.setuPartiji(false);
        boris.setLiga(com.elfak.slagalica.model.League.ZLATNE_LEGENDE.ordinal());
        boris.setAvatarUrl(null); // Ensure no screenshot
        db.collection("users").document(boris.getId()).set(boris);

        String[] imena = {"Stefan", "Marko", "Jovana", "Nikola", "Ana", "Milica", "Dusan", "Milos", "Jelena", "Igor", "Sara", "Luka", "Tara", "Pavle"};
        
        List<String> debugIds = new ArrayList<>();
        debugIds.add(boris.getId());
        
        for (String ime : imena) {
            String id = "debug_" + ime.toLowerCase() + "_" + r.nextInt(1000);
            User u = new User(id, id + "@example.com", ime, "Beograd");
            int zvezde = r.nextInt(2000);
            u.setZvezde(zvezde);
            u.setMesecniBodovi(r.nextInt(500));
            u.setOnline(r.nextBoolean());
            u.setuPartiji(u.isOnline() && r.nextInt(10) > 7);
            u.setLiga(com.elfak.slagalica.model.League.getByStars(zvezde).ordinal());
            u.setAvatarUrl(null); // Ensure no screenshot
            
            db.collection("users").document(id).set(u);
            debugIds.add(id);
        }
        
        db.collection("users").document(myUid).update("prijateljiIds", FieldValue.arrayUnion(debugIds.toArray()))
            .addOnSuccessListener(v -> {
                Toast.makeText(getContext(), "Seedovano " + debugIds.size() + " prijatelja!", Toast.LENGTH_SHORT).show();
                ucitajPrijatelje();
            });
    }

    private void dodajPrijatelja(String friendId) {
        friendRepository.addFriendMutual(friendId, () -> {
            Toast.makeText(getContext(), "Prijatelj uspešno dodat!", Toast.LENGTH_SHORT).show();
            ucitajPrijatelje(); 
        });
    }

    private void ucitajPrijatelje() {
        if (authRepository.trenutniKorisnik() == null) return;
        authRepository.getKorisnikPodaci(authRepository.trenutniKorisnik().getUid(), user -> {
            if (user != null && binding != null) {
                if (user.getKorisnickoIme() != null) mojeIme = user.getKorisnickoIme();
                friendRepository.loadFriends(user.getPrijateljiIds(), friends -> {
                    if (adapter != null) adapter.updateData(friends);
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopInviteStatusListener();
        binding = null;
    }
}