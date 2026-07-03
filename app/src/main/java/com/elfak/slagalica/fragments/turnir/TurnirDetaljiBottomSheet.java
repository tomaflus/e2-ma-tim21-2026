package com.elfak.slagalica.fragments.turnir;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.elfak.slagalica.R;
import com.elfak.slagalica.model.Turnir;
import com.elfak.slagalica.repository.TurnirRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Map;

public class TurnirDetaljiBottomSheet extends BottomSheetDialogFragment {

    private Turnir turnir;
    private String myId;
    private String myIme;
    private int myLiga;
    private int myTokeni;
    private String myAvatarUrl;
    private Runnable onChanged;

    private TurnirRepository repo;

    public static TurnirDetaljiBottomSheet newInstance(Turnir t, String myId, String myIme,
                                                        int myLiga, int myTokeni,
                                                        String myAvatarUrl, Runnable onChanged) {
        TurnirDetaljiBottomSheet f = new TurnirDetaljiBottomSheet();
        f.turnir      = t;
        f.myId        = myId;
        f.myIme       = myIme;
        f.myLiga      = myLiga;
        f.myTokeni    = myTokeni;
        f.myAvatarUrl = myAvatarUrl != null ? myAvatarUrl : "";
        f.onChanged   = onChanged;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_turnir_detalji, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = new TurnirRepository();

        TextView tvNaziv       = view.findViewById(R.id.tvDetaljiNaziv);
        LinearLayout llIgraci  = view.findViewById(R.id.llDetaljiIgraci);
        MaterialButton btnUcestvuj = view.findViewById(R.id.btnUcestvuj);
        MaterialButton btnNapusti  = view.findViewById(R.id.btnNapusti);
        MaterialButton btnObrisi   = view.findViewById(R.id.btnObrisi);

        tvNaziv.setText(turnir.getNaziv());

        // Popuni listu igrača (avatar + ime + liga)
        List<Map<String, Object>> igraci = turnir.getIgraci();
        llIgraci.removeAllViews();
        for (int i = 0; i < igraci.size(); i++) {
            Map<String, Object> m = igraci.get(i);
            String igracId  = (String) m.get("id");
            String igracIme = (String) m.get("ime");
            Object ligaObj  = m.get("liga");
            int ligaBr = ligaObj instanceof Number ? ((Number) ligaObj).intValue() : 0;
            String avatarUrl = (String) m.get("avatarUrl");
            String kruna = turnir.getVlasnikId().equals(igracId) ? " 👑" : "";

            llIgraci.addView(napraviRedIgraca(igracIme + kruna, ligaBr, avatarUrl));
        }

        // Prazna mjesta
        for (int i = igraci.size(); i < 4; i++) {
            TextView tv = new TextView(requireContext());
            tv.setTextColor(0x88FFFFFF);
            tv.setTextSize(14f);
            tv.setPadding(0, 8, 0, 8);
            tv.setText((i + 1) + ". — čeka igrača");
            llIgraci.addView(tv);
        }

        boolean jaUTurniru = false;
        for (Map<String, Object> m : igraci) {
            if (myId.equals(m.get("id"))) { jaUTurniru = true; break; }
        }
        boolean jaVlasnik  = myId.equals(turnir.getVlasnikId());
        boolean turnirPun  = igraci.size() >= 4;
        boolean mozePrijava = "CEKA".equals(turnir.getStatus());

        if (jaVlasnik) {
            btnUcestvuj.setVisibility(View.GONE);
            btnNapusti.setVisibility(View.GONE);
            btnObrisi.setVisibility(mozePrijava ? View.VISIBLE : View.GONE);
        } else if (jaUTurniru) {
            btnUcestvuj.setVisibility(View.GONE);
            btnNapusti.setVisibility(mozePrijava ? View.VISIBLE : View.GONE);
            btnObrisi.setVisibility(View.GONE);
        } else {
            btnUcestvuj.setVisibility(mozePrijava && !turnirPun ? View.VISIBLE : View.GONE);
            btnNapusti.setVisibility(View.GONE);
            btnObrisi.setVisibility(View.GONE);
        }

        if (!mozePrijava) {
            TextView tvStatus = new TextView(requireContext());
            tvStatus.setText("Turnir je u toku");
            tvStatus.setTextColor(0xFFFFD700);
            tvStatus.setTextSize(14f);
            tvStatus.setPadding(0, 16, 0, 0);
            llIgraci.addView(tvStatus);
        }

        btnUcestvuj.setOnClickListener(v -> {
            if (myTokeni < 3) {
                Toast.makeText(getContext(),
                        "Potrebno je najmanje 3 tokena za ulazak u turnir.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            btnUcestvuj.setEnabled(false);
            repo.prijaviSe(
                    turnir.getId(), turnir.getNaziv(), turnir.getVlasnikId(),
                    myId, myIme, myLiga, myAvatarUrl,
                    () -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Pridružio si se turniru!", Toast.LENGTH_SHORT).show();
                            if (onChanged != null) onChanged.run();
                            dismiss();
                        }
                    },
                    err -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show();
                            btnUcestvuj.setEnabled(true);
                        }
                    }
            );
        });

        btnNapusti.setOnClickListener(v -> {
            btnNapusti.setEnabled(false);
            repo.odjavi(
                    turnir.getId(), turnir.getNaziv(), turnir.getVlasnikId(),
                    myId, myIme,
                    () -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Napustio si turnir.", Toast.LENGTH_SHORT).show();
                            if (onChanged != null) onChanged.run();
                            dismiss();
                        }
                    },
                    err -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show();
                            btnNapusti.setEnabled(true);
                        }
                    }
            );
        });

        btnObrisi.setOnClickListener(v -> {
            btnObrisi.setEnabled(false);
            repo.obrisi(
                    turnir.getId(), turnir.getNaziv(), turnir.getVlasnikId(),
                    () -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Turnir obrisan.", Toast.LENGTH_SHORT).show();
                            if (onChanged != null) onChanged.run();
                            dismiss();
                        }
                    },
                    err -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Greška: " + err, Toast.LENGTH_SHORT).show();
                            btnObrisi.setEnabled(true);
                        }
                    }
            );
        });
    }

    private View napraviRedIgraca(String ime, int liga, String avatarUrl) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(8);
        row.setPadding(0, pad, 0, pad);

        // Avatar
        ImageView iv = new ImageView(requireContext());
        int size = dp(40);
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(size, size);
        ivParams.setMarginEnd(dp(12));
        iv.setLayoutParams(ivParams);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this)
                .load(avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : null)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(iv);
        row.addView(iv);

        // Ime + liga
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvIme = new TextView(requireContext());
        tvIme.setText(ime);
        tvIme.setTextColor(0xFFFFFFFF);
        tvIme.setTextSize(15f);
        info.addView(tvIme);

        TextView tvLiga = new TextView(requireContext());
        tvLiga.setText("Liga " + (liga + 1));
        tvLiga.setTextColor(0xFF9E9E9E);
        tvLiga.setTextSize(12f);
        info.addView(tvLiga);

        row.addView(info);
        return row;
    }

    private int dp(int val) {
        return (int) (val * requireContext().getResources().getDisplayMetrics().density);
    }
}
