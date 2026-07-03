package com.elfak.slagalica.fragments.turnir;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentTurnirBracketBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;

public class TurnirBracketFragment extends Fragment {

    private static final int ROLE_NONE         = 0;
    private static final int ROLE_SF1_IGRAC1   = 1;
    private static final int ROLE_SF1_IGRAC2   = 2;
    private static final int ROLE_SF2_IGRAC1   = 3;
    private static final int ROLE_SF2_IGRAC2   = 4;
    private static final int ROLE_FINAL_IGRAC1 = 5;
    private static final int ROLE_FINAL_IGRAC2 = 6;
    private static final int ROLE_TRECE_IGRAC1 = 7;
    private static final int ROLE_TRECE_IGRAC2 = 8;

    private FragmentTurnirBracketBinding binding;
    private ListenerRegistration listener;

    private String turnirId;
    private String myId;
    private int myRole = ROLE_NONE;
    private DocumentSnapshot lastSnap;
    private boolean rezimePrikazan = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTurnirBracketBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        myId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        turnirId = getArguments() != null ? getArguments().getString("turnirId") : null;
        if (turnirId == null) return;

        binding.btnIgraySF1.setOnClickListener(v -> klikniIgraj("sf1", "POLUFINALE 1"));
        binding.btnIgraySF2.setOnClickListener(v -> klikniIgraj("sf2", "POLUFINALE 2"));
        binding.btnIgrayFinal.setOnClickListener(v -> klikniIgraj("final", "FINALE"));
        binding.btnIgrayTrece.setOnClickListener(v -> klikniIgraj("trece", "3. MJESTO"));

        listener = FirebaseFirestore.getInstance()
                .collection("turniri").document(turnirId)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !snap.exists() || binding == null) return;
                    lastSnap = snap;
                    azurirajBracket(snap);
                });
    }

    @SuppressWarnings("unchecked")
    private void klikniIgraj(String sfPrefix, String sfNaziv) {
        if (lastSnap == null || !isAdded()) return;
        boolean jeIgrac1 = myRole == ROLE_SF1_IGRAC1 || myRole == ROLE_SF2_IGRAC1
                || myRole == ROLE_FINAL_IGRAC1 || myRole == ROLE_TRECE_IGRAC1;

        // Odredi ime protivnika
        String protivnikKey = sfPrefix + (jeIgrac1 ? "Igrac2" : "Igrac1");
        Map<String, Object> protivnikMap = (Map<String, Object>) lastSnap.get(protivnikKey);
        String protivnikIme = protivnikMap != null ? (String) protivnikMap.get("ime") : "Protivnik";

        Bundle args = new Bundle();
        args.putString("turnirId",     turnirId);
        args.putString("sfPrefix",     sfPrefix);
        args.putBoolean("jeIgrac1",    jeIgrac1);
        args.putString("protivnikIme", protivnikIme != null ? protivnikIme : "Protivnik");
        args.putString("turnirNaziv",  lastSnap.getString("naziv") != null ? lastSnap.getString("naziv") : "Turnir");
        args.putString("sfNaziv",      sfNaziv);

        Navigation.findNavController(requireView())
                .navigate(R.id.action_turnirBracketFragment_to_turnirCekanjeFragment, args);
    }

    @SuppressWarnings("unchecked")
    private void azurirajBracket(DocumentSnapshot snap) {
        String naziv = snap.getString("naziv");
        binding.tvBracketNaziv.setText(naziv != null ? naziv : "Turnir");

        myRole = odrediRolu(snap);

        // Polufinale 1
        Map<String, Object> sf1i1 = (Map<String, Object>) snap.get("sf1Igrac1");
        Map<String, Object> sf1i2 = (Map<String, Object>) snap.get("sf1Igrac2");
        prikaziIgraca(sf1i1, binding.ivAvatarSF1Igrac1, binding.tvImeSF1Igrac1, binding.tvLigaSF1Igrac1);
        prikaziIgraca(sf1i2, binding.ivAvatarSF1Igrac2, binding.tvImeSF1Igrac2, binding.tvLigaSF1Igrac2);

        Long sf1b1 = snap.getLong("sf1Bodovi1");
        Long sf1b2 = snap.getLong("sf1Bodovi2");
        binding.tvRezultatSF1.setText(sf1b1 != null ? sf1b1 + " — " + sf1b2 : "—");
        markirajGubitnika(sf1b1, sf1b2, binding.tvXSF1Igrac1, binding.tvXSF1Igrac2);

        // Polufinale 2
        Map<String, Object> sf2i1 = (Map<String, Object>) snap.get("sf2Igrac1");
        Map<String, Object> sf2i2 = (Map<String, Object>) snap.get("sf2Igrac2");
        prikaziIgraca(sf2i1, binding.ivAvatarSF2Igrac1, binding.tvImeSF2Igrac1, binding.tvLigaSF2Igrac1);
        prikaziIgraca(sf2i2, binding.ivAvatarSF2Igrac2, binding.tvImeSF2Igrac2, binding.tvLigaSF2Igrac2);

        Long sf2b1 = snap.getLong("sf2Bodovi1");
        Long sf2b2 = snap.getLong("sf2Bodovi2");
        binding.tvRezultatSF2.setText(sf2b1 != null ? sf2b1 + " — " + sf2b2 : "—");
        markirajGubitnika(sf2b1, sf2b2, binding.tvXSF2Igrac1, binding.tvXSF2Igrac2);

        // Igraj dugmad
        azurirajDugmeSF(snap, "sf1", binding.btnIgraySF1,
                myRole == ROLE_SF1_IGRAC1 || myRole == ROLE_SF1_IGRAC2,
                myRole == ROLE_SF1_IGRAC1);
        azurirajDugmeSF(snap, "sf2", binding.btnIgraySF2,
                myRole == ROLE_SF2_IGRAC1 || myRole == ROLE_SF2_IGRAC2,
                myRole == ROLE_SF2_IGRAC1);
        azurirajDugmeSF(snap, "final", binding.btnIgrayFinal,
                myRole == ROLE_FINAL_IGRAC1 || myRole == ROLE_FINAL_IGRAC2,
                myRole == ROLE_FINAL_IGRAC1);
        azurirajDugmeSF(snap, "trece", binding.btnIgrayTrece,
                myRole == ROLE_TRECE_IGRAC1 || myRole == ROLE_TRECE_IGRAC2,
                myRole == ROLE_TRECE_IGRAC1);

        // Finale
        Map<String, Object> fi1 = (Map<String, Object>) snap.get("finalIgrac1");
        Map<String, Object> fi2 = (Map<String, Object>) snap.get("finalIgrac2");
        prikaziFinalistu(fi1, binding.ivAvatarFinalIgrac1, binding.tvImeFinalIgrac1,
                binding.tvLigaFinalIgrac1, "Čeka pobjednika polufinala 1");
        prikaziFinalistu(fi2, binding.ivAvatarFinalIgrac2, binding.tvImeFinalIgrac2,
                binding.tvLigaFinalIgrac2, "Čeka pobjednika polufinala 2");

        Long fb1 = snap.getLong("finalBodovi1");
        Long fb2 = snap.getLong("finalBodovi2");
        binding.tvRezultatFinal.setText((fi1 != null && fi2 != null && fb1 != null)
                ? fb1 + " — " + fb2 : "—");
        markirajGubitnika(fb1, fb2, binding.tvXFinalIgrac1, binding.tvXFinalIgrac2);

        String pobjednikIme = snap.getString("pobjednikIme");
        if (pobjednikIme != null && !pobjednikIme.isEmpty()) {
            binding.tvPobjednik.setVisibility(View.VISIBLE);
            binding.tvPobjednik.setText("🏆 " + pobjednikIme);
        } else {
            binding.tvPobjednik.setVisibility(View.GONE);
        }

        // 3. mjesto
        Map<String, Object> t1 = (Map<String, Object>) snap.get("treceIgrac1");
        Map<String, Object> t2 = (Map<String, Object>) snap.get("treceIgrac2");
        prikaziFinalistu(t1, binding.ivAvatarTreceIgrac1, binding.tvImeTreceIgrac1,
                binding.tvLigaTreceIgrac1, "Čeka gubitnika polufinala 1");
        prikaziFinalistu(t2, binding.ivAvatarTreceIgrac2, binding.tvImeTreceIgrac2,
                binding.tvLigaTreceIgrac2, "Čeka gubitnika polufinala 2");

        Long tb1 = snap.getLong("treceBodovi1");
        Long tb2 = snap.getLong("treceBodovi2");
        binding.tvRezultatTrece.setText((t1 != null && t2 != null && tb1 != null)
                ? tb1 + " — " + tb2 : "—");
        markirajGubitnika(tb1, tb2, binding.tvXTreceIgrac1, binding.tvXTreceIgrac2);

        // Rezime po završetku turnira
        if ("GOTOV".equals(snap.getString("status")) && !rezimePrikazan && isAdded()) {
            rezimePrikazan = true;
            prikaziRezimuDijalog(snap);
        }

        String treciIme = snap.getString("treciIme");
        if (treciIme != null && !treciIme.isEmpty()) {
            binding.tvTreciPobjednik.setVisibility(View.VISIBLE);
            binding.tvTreciPobjednik.setText("🥉 " + treciIme);
        } else {
            binding.tvTreciPobjednik.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("unchecked")
    private int odrediRolu(DocumentSnapshot snap) {
        // Finale i 3. mjesto imaju prioritet — isti igrač je i u sf1Igrac1 i u finalIgrac1
        Map<String, Object> fi1 = (Map<String, Object>) snap.get("finalIgrac1");
        Map<String, Object> fi2 = (Map<String, Object>) snap.get("finalIgrac2");
        if (fi1 != null && myId.equals(fi1.get("id"))) return ROLE_FINAL_IGRAC1;
        if (fi2 != null && myId.equals(fi2.get("id"))) return ROLE_FINAL_IGRAC2;

        Map<String, Object> ti1 = (Map<String, Object>) snap.get("treceIgrac1");
        Map<String, Object> ti2 = (Map<String, Object>) snap.get("treceIgrac2");
        if (ti1 != null && myId.equals(ti1.get("id"))) return ROLE_TRECE_IGRAC1;
        if (ti2 != null && myId.equals(ti2.get("id"))) return ROLE_TRECE_IGRAC2;

        // Polufinala — tek ako nije napredovao u sljedeću fazu
        String[] sfKeys = {"sf1", "sf2"};
        int[] roles1 = {ROLE_SF1_IGRAC1, ROLE_SF2_IGRAC1};
        int[] roles2 = {ROLE_SF1_IGRAC2, ROLE_SF2_IGRAC2};
        for (int i = 0; i < 2; i++) {
            Map<String, Object> m1 = (Map<String, Object>) snap.get(sfKeys[i] + "Igrac1");
            Map<String, Object> m2 = (Map<String, Object>) snap.get(sfKeys[i] + "Igrac2");
            if (m1 != null && myId.equals(m1.get("id"))) return roles1[i];
            if (m2 != null && myId.equals(m2.get("id"))) return roles2[i];
        }
        return ROLE_NONE;
    }

    private void azurirajDugmeSF(DocumentSnapshot snap, String sfPrefix,
                                  MaterialButton btn, boolean mozeVidjeti, boolean jeIgrac1) {
        String partijaId = snap.getString(sfPrefix + "PartijaId");
        Log.d("TurnirBracket", "azurirajDugmeSF " + sfPrefix
                + " mozeVidjeti=" + mozeVidjeti + " partijaId=" + partijaId);
        if (!mozeVidjeti || partijaId != null) {
            btn.setVisibility(View.GONE);
            return;
        }
        String myReadyKey = sfPrefix + (jeIgrac1 ? "ReadyIgrac1" : "ReadyIgrac2");
        boolean vecKliknut = Boolean.TRUE.equals(snap.getBoolean(myReadyKey));
        Log.d("TurnirBracket", "azurirajDugmeSF " + sfPrefix + " vecKliknut=" + vecKliknut);
        btn.setVisibility(vecKliknut ? View.GONE : View.VISIBLE);
    }

    private void markirajGubitnika(Long b1, Long b2, TextView xI1, TextView xI2) {
        if (b1 == null || b2 == null) {
            xI1.setVisibility(View.GONE);
            xI2.setVisibility(View.GONE);
            return;
        }
        xI1.setVisibility(b1 < b2 ? View.VISIBLE : View.GONE);
        xI2.setVisibility(b2 < b1 ? View.VISIBLE : View.GONE);
    }

    private void prikaziIgraca(Map<String, Object> m, ImageView iv, TextView tvIme, TextView tvLiga) {
        if (m == null) return;
        String ime     = (String) m.get("ime");
        Object ligaObj = m.get("liga");
        int liga = ligaObj instanceof Number ? ((Number) ligaObj).intValue() : 0;
        String avatarUrl = (String) m.get("avatarUrl");

        tvIme.setText(ime != null ? ime : "—");
        tvLiga.setText("Liga " + (liga + 1));

        Glide.with(this)
                .load(avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : null)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(iv);
    }

    private void prikaziFinalistu(Map<String, Object> m, ImageView iv, TextView tvIme,
                                   TextView tvLiga, String placeholder) {
        if (m != null) {
            iv.setVisibility(View.VISIBLE);
            tvLiga.setVisibility(View.VISIBLE);
            tvIme.setTextColor(0xFFFFFFFF);
            tvIme.setTextSize(13f);
            tvIme.setTypeface(null, android.graphics.Typeface.BOLD);
            prikaziIgraca(m, iv, tvIme, tvLiga);
        } else {
            iv.setVisibility(View.INVISIBLE);
            tvLiga.setVisibility(View.GONE);
            tvIme.setTextColor(0xAAFFFFFF);
            tvIme.setTextSize(12f);
            tvIme.setTypeface(null, android.graphics.Typeface.ITALIC);
            tvIme.setText(placeholder);
        }
    }

    private void prikaziRezimuDijalog(DocumentSnapshot snap) {
        String naziv = snap.getString("naziv") != null ? snap.getString("naziv") : "Turnir";
        String p1 = snap.getString("pobjednikIme");
        String p2 = snap.getString("drugiIme");
        String p3 = snap.getString("treciIme");
        String p4 = snap.getString("cetvrtiIme");

        String tekst = "🥇  " + (p1 != null ? p1 : "—") + "\n\n"
                     + "🥈  " + (p2 != null ? p2 : "—") + "\n\n"
                     + "🥉  " + (p3 != null ? p3 : "—") + "\n\n"
                     + "4.    " + (p4 != null ? p4 : "—");

        new AlertDialog.Builder(requireContext())
                .setTitle("🏆 " + naziv + " — završen!")
                .setMessage(tekst)
                .setPositiveButton("Pogledaj bracket", null)
                .setCancelable(true)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) { listener.remove(); listener = null; }
        binding = null;
    }
}
