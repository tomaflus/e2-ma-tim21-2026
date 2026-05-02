package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elfak.slagalica.databinding.FragmentKorakPoKorakBinding;
import com.elfak.slagalica.model.KorakPoKorak;
import com.elfak.slagalica.repository.KorakPoKorakRepository;

public class KorakPoKorakFragment extends Fragment {

    private FragmentKorakPoKorakBinding binding;
    private KorakPoKorakRepository repository;
    private KorakPoKorak trenutnoPitanje;

    private int trenutniKorak = 0;
    private int bodovi = 0;
    private CountDownTimer tajmer;
    private boolean igraZavrsena = false;

    // Bodovanje
    private static final int MAX_BODOVA = 20;
    private static final int ODBITAK_PO_KORAKU = 2;
    private static final int BODOVI_PROTIVNIK = 5;
    private static final int TRAJANJE_KORAKA = 10000; // 10 sekundi

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKorakPoKorakBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new KorakPoKorakRepository();

        // Sakrij sve korake na početku
        sakriSveKorake();

        // Dohvati pitanje iz Firebase
        ucitajPitanje();

        // Klik na dugme Odgovori
        binding.btnOdgovori.setOnClickListener(v -> {
            if (igraZavrsena) return;

            String odgovor = binding.etOdgovor.getText().toString().trim();

            if (odgovor.isEmpty()) {
                Toast.makeText(getContext(),
                        "Unesite odgovor!", Toast.LENGTH_SHORT).show();
                return;
            }

            provjeriOdgovor(odgovor);
        });
    }

    private void ucitajPitanje() {
        binding.btnOdgovori.setEnabled(false);
        repository.dohvatiNasumicnoPitanje(
                pitanje -> {
                    trenutnoPitanje = pitanje;
                    binding.btnOdgovori.setEnabled(true);
                    prikaziKorak(0);
                    pokrniTajmer();
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_LONG).show()
        );
    }

    private void pokrniTajmer() {
        if (tajmer != null) tajmer.cancel();

        tajmer = new CountDownTimer(TRAJANJE_KORAKA, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvTajmer.setText(millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                // Isteklo vrijeme za ovaj korak — otvori sljedeći
                if (trenutniKorak < 6) {
                    trenutniKorak++;
                    prikaziKorak(trenutniKorak);
                    pokrniTajmer();
                } else {
                    // Svi koraci otvoreni — igra završena bez odgovora
                    igraZavrsena = true;
                    binding.btnOdgovori.setEnabled(false);
                    Toast.makeText(getContext(),
                            "Isteklo vrijeme! Rjesenje: " + trenutnoPitanje.getRjesenje(),
                            Toast.LENGTH_LONG).show();
                    azurirajBodove();
                }
            }
        }.start();
    }

    private void provjeriOdgovor(String odgovor) {
        if (trenutnoPitanje == null) return;

        if (odgovor.equalsIgnoreCase(trenutnoPitanje.getRjesenje())) {
            // Tačan odgovor!
            tajmer.cancel();
            igraZavrsena = true;
            binding.btnOdgovori.setEnabled(false);

            // Bodovi = 20 - (trenutniKorak * 2)
            int osvојeniBodovi = MAX_BODOVA - (trenutniKorak * ODBITAK_PO_KORAKU);
            bodovi += osvојeniBodovi;
            azurirajBodove();

            Toast.makeText(getContext(),
                    "Tacno! Osvojili ste " + osvојeniBodovi + " bodova!",
                    Toast.LENGTH_LONG).show();
        } else {
            // Netačan odgovor
            Toast.makeText(getContext(),
                    "Netacno, pokusajte ponovo!", Toast.LENGTH_SHORT).show();
            binding.etOdgovor.setText("");
        }
    }

    private void prikaziKorak(int index) {
        if (trenutnoPitanje == null) return;

        String[] koraci = trenutnoPitanje.getKoraci();
        sakriSveKorake();

        if (index >= 0 && koraci[0] != null) {
            binding.tvKorak1.setText("Korak 1: " + koraci[0]);
            binding.tvKorak1.setVisibility(View.VISIBLE);
        }
        if (index >= 1 && koraci[1] != null) {
            binding.tvKorak2.setText("Korak 2: " + koraci[1]);
            binding.tvKorak2.setVisibility(View.VISIBLE);
        }
        if (index >= 2 && koraci[2] != null) {
            binding.tvKorak3.setText("Korak 3: " + koraci[2]);
            binding.tvKorak3.setVisibility(View.VISIBLE);
        }
        if (index >= 3 && koraci[3] != null) {
            binding.tvKorak4.setText("Korak 4: " + koraci[3]);
            binding.tvKorak4.setVisibility(View.VISIBLE);
        }
        if (index >= 4 && koraci[4] != null) {
            binding.tvKorak5.setText("Korak 5: " + koraci[4]);
            binding.tvKorak5.setVisibility(View.VISIBLE);
        }
        if (index >= 5 && koraci[5] != null) {
            binding.tvKorak6.setText("Korak 6: " + koraci[5]);
            binding.tvKorak6.setVisibility(View.VISIBLE);
        }
        if (index >= 6 && koraci[6] != null) {
            binding.tvKorak7.setText("Korak 7: " + koraci[6]);
            binding.tvKorak7.setVisibility(View.VISIBLE);
        }
    }

    private void sakriSveKorake() {
        binding.tvKorak1.setVisibility(View.GONE);
        binding.tvKorak2.setVisibility(View.GONE);
        binding.tvKorak3.setVisibility(View.GONE);
        binding.tvKorak4.setVisibility(View.GONE);
        binding.tvKorak5.setVisibility(View.GONE);
        binding.tvKorak6.setVisibility(View.GONE);
        binding.tvKorak7.setVisibility(View.GONE);
    }

    private void azurirajBodove() {
        binding.tvBodovi.setText("Bodovi: " + bodovi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        binding = null;
    }
}