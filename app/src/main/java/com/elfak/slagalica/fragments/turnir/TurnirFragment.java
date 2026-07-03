package com.elfak.slagalica.fragments.turnir;

import android.app.AlertDialog;
import java.util.Collections;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.R;
import com.elfak.slagalica.adapters.TurnirAdapter;
import com.elfak.slagalica.databinding.FragmentTurnirBinding;
import com.elfak.slagalica.model.Turnir;
import com.elfak.slagalica.repository.TurnirRepository;
import com.elfak.slagalica.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class TurnirFragment extends Fragment {

    private FragmentTurnirBinding binding;
    private TurnirRepository repo;
    private ListenerRegistration listener;

    private String myId;
    private String myIme = "";
    private int myLiga = 0;
    private int myTokeni = 0;
    private String myAvatarUrl = "";

    private TurnirAdapter mojiAdapter;
    private TurnirAdapter ostaliAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTurnirBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = new TurnirRepository();

        myId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        mojiAdapter   = new TurnirAdapter(t -> otvoriDetalje(t), true);
        ostaliAdapter = new TurnirAdapter(t -> otvoriDetalje(t), false);

        binding.rvMojiTurniri.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMojiTurniri.setAdapter(mojiAdapter);

        binding.rvOstaliTurniri.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOstaliTurniri.setAdapter(ostaliAdapter);

        binding.btnNapraviTurnir.setOnClickListener(v -> otvoriDijalogNapraviTurnir());

        ucitajKorisnika();
    }

    private void ucitajKorisnika() {
        new UserRepository().dohvatiKorisnika(
                user -> {
                    myIme      = user.getKorisnickoIme();
                    myLiga     = user.getLiga();
                    myTokeni   = user.getTokeni();
                    myAvatarUrl = user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
                    pocniSlušati();
                },
                err -> pocniSlušati()
        );
    }

    private void pocniSlušati() {
        listener = repo.slusajTurnire(svi -> {
            if (binding == null) return;
            List<Turnir> moji   = new ArrayList<>();
            List<Turnir> ostali = new ArrayList<>();
            for (Turnir t : svi) {
                if (jaUTurniru(t)) {
                    moji.add(t);
                } else if ("CEKA".equals(t.getStatus())) {
                    ostali.add(t);
                }
            }
            Collections.sort(moji, (a, b) -> {
                int redA = statusRed(a.getStatus());
                int redB = statusRed(b.getStatus());
                if (redA != redB) return redA - redB;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt()); // noviji prvi
            });
            mojiAdapter.setLista(moji);
            ostaliAdapter.setLista(ostali);
            binding.tvMojiPrazno.setVisibility(moji.isEmpty() ? View.VISIBLE : View.GONE);
            binding.tvOstaliPrazno.setVisibility(ostali.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private int statusRed(String status) {
        if ("CEKA".equals(status))    return 0;
        if ("U_TOKU".equals(status))  return 1;
        return 2; // GOTOV
    }

    private boolean jaUTurniru(Turnir t) {
        for (java.util.Map<String, Object> m : t.getIgraci()) {
            if (myId.equals(m.get("id"))) return true;
        }
        return false;
    }

    private void otvoriDijalogNapraviTurnir() {
        EditText et = new EditText(requireContext());
        et.setHint("Naziv turnira");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Napravi turnir")
                .setView(et)
                .setPositiveButton("Napravi", (d, w) -> {
                    String naziv = et.getText().toString().trim();
                    if (naziv.isEmpty()) {
                        Toast.makeText(getContext(), "Unesite naziv turnira.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (myTokeni < 3) {
                        Toast.makeText(getContext(),
                                "Potrebno je najmanje 3 tokena za kreiranje turnira.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.napraviTurnir(naziv, myId, myIme, myLiga, myAvatarUrl,
                            () -> Toast.makeText(getContext(), "Turnir napravljen!", Toast.LENGTH_SHORT).show(),
                            err -> Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Odustani", null)
                .show();
    }

    private void otvoriDetalje(Turnir t) {
        if (("U_TOKU".equals(t.getStatus()) || "GOTOV".equals(t.getStatus()))
                && !isDetached() && isAdded()) {
            Bundle args = new Bundle();
            args.putString("turnirId", t.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_turnirFragment_to_turnirBracketFragment, args);
        } else {
            TurnirDetaljiBottomSheet
                    .newInstance(t, myId, myIme, myLiga, myTokeni, myAvatarUrl, null)
                    .show(getChildFragmentManager(), "turnirDetalji");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
        binding = null;
    }
}
