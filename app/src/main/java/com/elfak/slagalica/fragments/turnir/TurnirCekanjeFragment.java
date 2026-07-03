package com.elfak.slagalica.fragments.turnir;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentTurnirCekanjeBinding;
import com.elfak.slagalica.repository.TurnirRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class TurnirCekanjeFragment extends Fragment {

    private FragmentTurnirCekanjeBinding binding;
    private ListenerRegistration listener;
    private TurnirRepository repo;
    private boolean navigirano = false;

    private String turnirId;
    private String sfPrefix;
    private boolean jeIgrac1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTurnirCekanjeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = new TurnirRepository();

        Bundle args = getArguments();
        if (args == null) return;

        turnirId            = args.getString("turnirId");
        sfPrefix            = args.getString("sfPrefix");
        jeIgrac1            = args.getBoolean("jeIgrac1", true);
        String protivnikIme = args.getString("protivnikIme", "Protivnik");
        String turnirNaziv  = args.getString("turnirNaziv", "Turnir");
        String sfNaziv      = args.getString("sfNaziv", "POLUFINALE");

        binding.tvCekanjeTurnirNaziv.setText(turnirNaziv);
        binding.tvCekanjeFaza.setText(sfNaziv);
        binding.tvCekanjeStatus.setText("Čeka se " + protivnikIme + "...");

        binding.btnOdustaniCekanje.setOnClickListener(v -> odustani());

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() { odustani(); }
                });

        repo.readySe(turnirId, sfPrefix, jeIgrac1,
                () -> { /* listener detektuje partijaId i navigira */ },
                err -> {
                    if (isAdded())
                        Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show();
                });

        String partijaIdKey = sfPrefix + "PartijaId";
        listener = FirebaseFirestore.getInstance()
                .collection("turniri").document(turnirId)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !snap.exists() || binding == null || navigirano) return;
                    String partijaId = snap.getString(partijaIdKey);
                    if (partijaId != null) {
                        navigirajNaPartiju(partijaId, jeIgrac1);
                    }
                });
    }

    private void odustani() {
        if (!isAdded() || navigirano) return;
        if (turnirId == null || sfPrefix == null) {
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }
        repo.odustaniOdCekanja(turnirId, sfPrefix, jeIgrac1,
                () -> {
                    if (isAdded()) Navigation.findNavController(requireView()).popBackStack();
                },
                err -> {
                    if (isAdded()) Navigation.findNavController(requireView()).popBackStack();
                });
    }

    private void navigirajNaPartiju(String partijaId, boolean jeIgrac1) {
        if (!isAdded() || isDetached()) return;
        navigirano = true;
        Bundle args = new Bundle();
        args.putString("partijaId", partijaId);
        args.putBoolean("jeIgrac1", jeIgrac1);
        args.putString("turnirId", turnirId);
        args.putString("turnirSfPrefix", sfPrefix);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_turnirCekanjeFragment_to_igraFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) { listener.remove(); listener = null; }
        binding = null;
    }
}
