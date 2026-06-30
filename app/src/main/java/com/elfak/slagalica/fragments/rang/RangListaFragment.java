package com.elfak.slagalica.fragments.rang;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.widget.Toast;

import com.elfak.slagalica.adapters.RangAdapter;
import com.elfak.slagalica.databinding.FragmentRangListaBinding;
import com.elfak.slagalica.model.RangLista;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.service.RewardService;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RangListaFragment extends Fragment {

    private FragmentRangListaBinding binding;
    private RangAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RangAdapter.Tip trenutniTip = RangAdapter.Tip.NEDELJNA;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final Runnable autoRefresh = new Runnable() {
        @Override
        public void run() {
            ucitajListu();
            handler.postDelayed(this, 120_000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRangListaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new RangAdapter();
        binding.rvRangLista.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRangLista.setAdapter(adapter);

        prikaziNedeljnu();

        binding.btnNedeljna.setOnClickListener(v -> prikaziNedeljnu());
        binding.btnMesecna.setOnClickListener(v -> prikaziMesecnu());

        // TEST: obrađuje nagrade za sve rangirane korisnike i resetuje listu
        binding.btnTestSimulacija.setOnClickListener(v -> simulirajKrajCiklusa());

        handler.post(autoRefresh);
    }

    private void prikaziNedeljnu() {
        trenutniTip = RangAdapter.Tip.NEDELJNA;
        binding.btnNedeljna.setAlpha(1f);
        binding.btnMesecna.setAlpha(0.5f);
        ucitajListu();
    }

    private void prikaziMesecnu() {
        trenutniTip = RangAdapter.Tip.MESECNA;
        binding.btnNedeljna.setAlpha(0.5f);
        binding.btnMesecna.setAlpha(1f);
        ucitajListu();
    }

    private void ucitajListu() {
        String tipStr = trenutniTip == RangAdapter.Tip.NEDELJNA ? "nedeljna" : "mesecna";
        final RangAdapter.Tip tipSnapshot = trenutniTip;

        db.collection("rangListe")
                .whereEqualTo("tip", tipStr)
                .whereEqualTo("aktivna", true)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (binding == null) return;

                    if (snaps.isEmpty()) {
                        // Nema aktivne rang liste — kreiraj je
                        kreirajRangListuAkoNema(tipStr, () -> ucitajListu());
                        return;
                    }

                    RangLista rl = snaps.getDocuments().get(0).toObject(RangLista.class);
                    if (rl == null) return;

                    binding.tvOpseg.setText(rl.getOpseg());

                    String ciklusPolje = tipSnapshot == RangAdapter.Tip.NEDELJNA
                            ? "nedeljaCiklusId" : "mesecCiklusId";

                    db.collection("users")
                            .whereEqualTo(ciklusPolje, rl.getCiklusId())
                            .get()
                            .addOnSuccessListener(userSnaps -> {
                                if (binding == null) return;
                                List<User> lista = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : userSnaps) {
                                    User u = doc.toObject(User.class);
                                    u.setId(doc.getId());
                                    // prikaži samo korisnike koji su stvarno igrali u ovom ciklusu
                                    boolean rangiran = tipSnapshot == RangAdapter.Tip.NEDELJNA
                                            ? u.isRangiranNedelja() : u.isRangiranMesec();
                                    if (rangiran) lista.add(u);
                                }
                                Collections.sort(lista, (a, b) -> {
                                    int va = tipSnapshot == RangAdapter.Tip.NEDELJNA
                                            ? a.getNedeljneZvezde() : a.getMesecneZvezde();
                                    int vb = tipSnapshot == RangAdapter.Tip.NEDELJNA
                                            ? b.getNedeljneZvezde() : b.getMesecneZvezde();
                                    return vb - va;
                                });
                                if (lista.size() > 10) lista = lista.subList(0, 10);
                                adapter.updateData(lista, tipSnapshot);
                            });
                });
    }

    private void kreirajRangListuAkoNema(String tip, Runnable onKreirano) {
        String ciklusId = tip.equals("nedeljna")
                ? CiklusUtil.trenutniNedeljaId() + "-1"
                : CiklusUtil.trenutniMesecId() + "-1";
        String opseg = tip.equals("nedeljna")
                ? CiklusUtil.opsegNedelje()
                : CiklusUtil.opsegMeseca();

        RangLista rl = new RangLista(tip, ciklusId, opseg, true);
        db.collection("rangListe").add(rl)
                .addOnSuccessListener(ref -> { if (onKreirano != null) onKreirano.run(); });
    }

    // TEST: obrađuje nagrade za sve rangirane korisnike tekuće sedmice i mjeseca
    private void simulirajKrajCiklusa() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        binding.btnTestSimulacija.setEnabled(false);
        new RewardService().obradiKrajCiklusa(requireContext(), () -> {
            if (!isAdded()) return;
            binding.btnTestSimulacija.setEnabled(true);
            ucitajListu();
            Toast.makeText(getContext(),
                    "Kraj ciklusa obrađen. Nagrade dodijeljene. Idi na Home.",
                    Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(autoRefresh);
        binding = null;
    }
}
