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
import com.elfak.slagalica.model.User;
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

        // TEST: simulacija kraja ciklusa — ukloniti prije produkcije
        binding.btnTestSimulacija.setOnClickListener(v -> simulirajKrajCiklusa());

        handler.post(autoRefresh);
    }

    // TEST: postavlja stari ciklusId da proveriINagradi smatra da je ciklus završen
    private void simulirajKrajCiklusa() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("nedeljaCiklusId", "2020-W01");
        updates.put("mesecCiklusId", "2020-01");
        updates.put("nedeljneZvezde", 42);
        updates.put("mesecneZvezde", 42);
        updates.put("rangiranNedelja", true);
        updates.put("rangiranMesec", true);
        updates.put("lastNagradaNedeljaId", "");
        updates.put("lastNagradaMesecId", "");
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(v ->
                        Toast.makeText(getContext(),
                                "Ciklus simuliran! Idi na Home da preuzmeš nagradu.",
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void prikaziNedeljnu() {
        trenutniTip = RangAdapter.Tip.NEDELJNA;
        binding.tvOpseg.setText(CiklusUtil.opsegNedelje());
        binding.btnNedeljna.setAlpha(1f);
        binding.btnMesecna.setAlpha(0.5f);
        ucitajListu();
    }

    private void prikaziMesecnu() {
        trenutniTip = RangAdapter.Tip.MESECNA;
        binding.tvOpseg.setText(CiklusUtil.opsegMeseca());
        binding.btnNedeljna.setAlpha(0.5f);
        binding.btnMesecna.setAlpha(1f);
        ucitajListu();
    }

    private void ucitajListu() {
        String ciklusPolje = trenutniTip == RangAdapter.Tip.NEDELJNA
                ? "nedeljaCiklusId" : "mesecCiklusId";
        String trenutniId = trenutniTip == RangAdapter.Tip.NEDELJNA
                ? CiklusUtil.trenutniNedeljaId() : CiklusUtil.trenutniMesecId();
        String zvezdePolje = trenutniTip == RangAdapter.Tip.NEDELJNA
                ? "nedeljneZvezde" : "mesecneZvezde";

        final RangAdapter.Tip tipSnapshot = trenutniTip;

        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo(ciklusPolje, trenutniId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (binding == null) return;
                    List<User> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User u = doc.toObject(User.class);
                        u.setId(doc.getId());
                        lista.add(u);
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(autoRefresh);
        binding = null;
    }
}
