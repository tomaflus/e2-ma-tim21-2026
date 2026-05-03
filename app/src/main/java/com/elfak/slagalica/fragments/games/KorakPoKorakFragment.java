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
import com.elfak.slagalica.repository.PartijaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class KorakPoKorakFragment extends Fragment {

    private FragmentKorakPoKorakBinding binding;
    private KorakPoKorakRepository repository;
    private PartijaRepository partijaRepository;
    private KorakPoKorak trenutnoPitanje;

    // Parametri partije
    private String partijaId;
    private boolean jeIgrac1;

    // Status igre
    private int trenutniKorak = 0;
    private int bodovi = 0;
    private int boduiProtivnika = 0;
    private CountDownTimer tajmer;
    private boolean igraZavrsena = false;
    private boolean mojaRunda = false;
    private boolean runda1Zavrsena = false;

    // Bodovanje
    private static final int MAX_BODOVA = 20;
    private static final int ODBITAK_PO_KORAKU = 2;
    private static final int BODOVI_PROTIVNIK = 5;
    private static final int TRAJANJE_KORAKA = 10000;
    private static final int TRAJANJE_PROTIVNIK = 10000;

    // Status runde
    private static final String IGRAC1_IGRA = "IGRAC1_IGRA";
    private static final String IGRAC2_IGRA = "IGRAC2_IGRA";
    private static final String IGRAC1_PROMISIO = "IGRAC1_PROMISIO";
    private static final String IGRAC2_PROMISIO = "IGRAC2_PROMISIO";
    private static final String ZAVRSENA = "ZAVRSENA";

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
        partijaRepository = new PartijaRepository();

        // Dohvati argumente
        if (getArguments() != null) {
            partijaId = getArguments().getString("partijaId");
            jeIgrac1 = getArguments().getBoolean("jeIgrac1", true);
        }

        // Igrac 1 počinje prvu rundu
        mojaRunda = jeIgrac1;

        sakriSveKorake();
        ucitajPitanje();

        // Slušaj promjene statusa runde u Firestore
        slušajStatusRunde();

        // Klik na dugme Odgovori
        binding.btnOdgovori.setOnClickListener(v -> {
            if (igraZavrsena || !mojaRunda) return;

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

                    // Sačuvaj pitanje ID u Firestore za oba igrača
                    if (jeIgrac1) {
                        FirebaseFirestore.getInstance()
                                .collection("partije").document(partijaId)
                                .update("pitanjeKorakPoKorakId", pitanje.getId(),
                                        "statusKorakPoKorak", IGRAC1_IGRA);
                    }

                    if (mojaRunda) {
                        binding.btnOdgovori.setEnabled(true);
                        prikaziKorak(0);
                        pokrniTajmer();
                    } else {
                        binding.tvTajmer.setText("Cekaj...");
                        prikaziPoruku("Protivnik igra svoju rundu...");
                    }
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_LONG).show()
        );
    }

    private void slušajStatusRunde() {
        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("statusKorakPoKorak");
                    if (status == null) return;

                    // Dohvati bodove iz Firestore
                    Long b1 = snapshot.getLong("bodovi1KorakPoKorak");
                    Long b2 = snapshot.getLong("bodovi2KorakPoKorak");

                    switch (status) {
                        case IGRAC2_IGRA:
                            // Igrac 1 je završio — sada Igrac 2 igra
                            if (!jeIgrac1 && !runda1Zavrsena) {
                                runda1Zavrsena = true;
                                mojaRunda = true;
                                trenutniKorak = 0;
                                prikaziKorak(0);
                                binding.btnOdgovori.setEnabled(true);
                                pokrniTajmer();
                                prikaziPoruku("Tvoja runda!");
                            }
                            break;

                        case IGRAC1_PROMISIO:
                            // Igrac 1 nije pogodio — Igrac 2 ima šansu za 5 bodova
                            if (!jeIgrac1 && mojaRunda) {
                                zaustaviTajmer();
                                prikaziPoruku("Protivnik nije pogodio! Imas 10s za 5 bodova!");
                                pokrniTajmerProtivnika();
                            }
                            break;

                        case IGRAC2_PROMISIO:
                            // Igrac 2 nije pogodio — Igrac 1 ima šansu za 5 bodova
                            if (jeIgrac1) {
                                zaustaviTajmer();
                                prikaziPoruku("Protivnik nije pogodio! Imas 10s za 5 bodova!");
                                pokrniTajmerProtivnika();
                            }
                            break;

                        case ZAVRSENA:
                            // Igra završena
                            igraZavrsena = true;
                            zaustaviTajmer();
                            binding.btnOdgovori.setEnabled(false);

                            int mojiB = jeIgrac1 ?
                                    (b1 != null ? b1.intValue() : 0) :
                                    (b2 != null ? b2.intValue() : 0);

                            prikaziPoruku("Igra završena! Tvoji bodovi: " + mojiB);

                            // Obavijesti IgraFragment
                            if (getArguments() != null) {
                                Bundle result = new Bundle();
                                result.putInt("bodovi", mojiB);
                                getParentFragmentManager()
                                        .setFragmentResult("korakPoKorakZavrsen", result);
                            }
                            break;
                    }
                });
    }

    private void pokrniTajmer() {
        zaustaviTajmer();
        tajmer = new CountDownTimer(TRAJANJE_KORAKA * 7L, 1000) {
            @Override
            public void onTick(long ms) {
                int sekunde = (int)(ms / 1000);
                binding.tvTajmer.setText(sekunde + "s");

                // Otvori novi korak svakih 10s
                int korakIndex = 6 - (sekunde / 10);
                if (korakIndex > trenutniKorak && korakIndex <= 6) {
                    trenutniKorak = korakIndex;
                    prikaziKorak(trenutniKorak);
                }
            }

            @Override
            public void onFinish() {
                // Igrač nije pogodio
                igraZavrsena = true;
                binding.btnOdgovori.setEnabled(false);
                azurirajStatusPromisio();
            }
        }.start();
    }

    private void pokrniTajmerProtivnika() {
        zaustaviTajmer();
        tajmer = new CountDownTimer(TRAJANJE_PROTIVNIK, 1000) {
            @Override
            public void onTick(long ms) {
                binding.tvTajmer.setText("Bonus: " + ms / 1000 + "s");
            }

            @Override
            public void onFinish() {
                // Nije iskoristio bonus šansu
                azurirajZavrsenu();
            }
        }.start();
        binding.btnOdgovori.setEnabled(true);
        mojaRunda = true;
    }

    private void provjeriOdgovor(String odgovor) {
        if (trenutnoPitanje == null) return;

        if (odgovor.equalsIgnoreCase(trenutnoPitanje.getRjesenje())) {
            tajmer.cancel();
            binding.btnOdgovori.setEnabled(false);

            int osvојeniBodovi;
            if (!runda1Zavrsena || (jeIgrac1 && !runda1Zavrsena)) {
                // Normalna runda
                osvојeniBodovi = MAX_BODOVA - (trenutniKorak * ODBITAK_PO_KORAKU);
            } else {
                // Bonus šansa
                osvојeniBodovi = BODOVI_PROTIVNIK;
            }

            bodovi += osvојeniBodovi;

            // Ažuriraj bodove u Firestore
            String poljeB = jeIgrac1 ? "bodovi1KorakPoKorak" : "bodovi2KorakPoKorak";
            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update(poljeB, bodovi);

            Toast.makeText(getContext(),
                    "Tacno! +" + osvојeniBodovi + " bodova!",
                    Toast.LENGTH_SHORT).show();

            azurirajSledeci();
        } else {
            Toast.makeText(getContext(),
                    "Netacno!", Toast.LENGTH_SHORT).show();
            binding.etOdgovor.setText("");
        }
    }

    private void azurirajSledeci() {
        if (jeIgrac1 && !runda1Zavrsena) {
            // Igrac 1 pogodio — Igrac 2 na red
            runda1Zavrsena = true;
            mojaRunda = false;
            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update("statusKorakPoKorak", IGRAC2_IGRA);
        } else {
            // Igrac 2 pogodio — igra završena
            azurirajZavrsenu();
        }
    }

    private void azurirajStatusPromisio() {
        String status = jeIgrac1 ? IGRAC1_PROMISIO : IGRAC2_PROMISIO;
        mojaRunda = false;

        if (jeIgrac1 && !runda1Zavrsena) {
            runda1Zavrsena = true;
            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update("statusKorakPoKorak", status);
        } else {
            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update("statusKorakPoKorak", status);
        }
    }

    private void azurirajZavrsenu() {
        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .update("statusKorakPoKorak", ZAVRSENA);
    }

    private void prikaziPoruku(String poruka) {
        Toast.makeText(getContext(), poruka, Toast.LENGTH_LONG).show();
    }

    private void zaustaviTajmer() {
        if (tajmer != null) tajmer.cancel();
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
        zaustaviTajmer();
        binding = null;
    }
}