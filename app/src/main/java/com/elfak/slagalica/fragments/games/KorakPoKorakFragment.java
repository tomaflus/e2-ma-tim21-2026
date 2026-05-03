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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class KorakPoKorakFragment extends Fragment {

    private FragmentKorakPoKorakBinding binding;
    private KorakPoKorakRepository repository;
    private KorakPoKorak trenutnoPitanje;
    private ListenerRegistration listenerRegistration;

    private String partijaId;
    private boolean jeIgrac1;

    private int trenutniKorak = 0;
    private int bodovi = 0;
    private CountDownTimer tajmer;
    private boolean mojaRunda = false;
    private boolean bonusRunda = false;

    private static final int MAX_BODOVA = 20;
    private static final int ODBITAK_PO_KORAKU = 2;
    private static final int BODOVI_BONUS = 5;
    private static final long TRAJANJE_RUNDE = 70000;
    private static final long TRAJANJE_BONUSA = 10000;

    // Statusi
    private static final String IGRAC1_IGRA = "IGRAC1_IGRA";
    private static final String IGRAC1_BONUS = "IGRAC1_BONUS";
    private static final String IGRAC2_IGRA = "IGRAC2_IGRA";
    private static final String IGRAC2_BONUS = "IGRAC2_BONUS";
    private static final String IGRAC2_IGRA_RUNDA2 = "IGRAC2_IGRA_RUNDA2";
    private static final String IGRAC2_BONUS_RUNDA2 = "IGRAC2_BONUS_RUNDA2";
    private static final String IGRAC1_BONUS_RUNDA2 = "IGRAC1_BONUS_RUNDA2";
    private static final String ZAVRSENA = "ZAVRSENA";

    private String trenutniStatus = "";

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

        if (getArguments() != null) {
            partijaId = getArguments().getString("partijaId");
            jeIgrac1 = getArguments().getBoolean("jeIgrac1", true);
        }

        sakriSveKorake();
        binding.btnOdgovori.setEnabled(false);

        ucitajPitanje();
        slušajStatusRunde();

        binding.btnOdgovori.setOnClickListener(v -> {
            if (!mojaRunda) {
                Toast.makeText(getContext(), "Nije tvoja runda!", Toast.LENGTH_SHORT).show();
                return;
            }
            String odgovor = binding.etOdgovor.getText().toString().trim();
            if (odgovor.isEmpty()) {
                Toast.makeText(getContext(), "Unesite odgovor!", Toast.LENGTH_SHORT).show();
                return;
            }
            provjeriOdgovor(odgovor);
        });
    }

    private void ucitajPitanje() {
        repository.dohvatiNasumicnoPitanje(
                pitanje -> {
                    trenutnoPitanje = pitanje;

                    if (jeIgrac1) {
                        FirebaseFirestore.getInstance()
                                .collection("partije").document(partijaId)
                                .update(
                                        "pitanjeKorakPoKorakId", pitanje.getId(),
                                        "statusKorakPoKorak", IGRAC1_IGRA,
                                        "bodovi1KorakPoKorak", 0,
                                        "bodovi2KorakPoKorak", 0
                                );
                        pocniMojuRundu();
                    } else {
                        prikaziCekanje("Protivnik igra svoju rundu...");
                    }
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_LONG).show()
        );
    }

    private void slušajStatusRunde() {
        listenerRegistration = FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("statusKorakPoKorak");
                    if (status == null || status.equals(trenutniStatus)) return;
                    trenutniStatus = status;

                    Long b1 = snapshot.getLong("bodovi1KorakPoKorak");
                    Long b2 = snapshot.getLong("bodovi2KorakPoKorak");

                    switch (status) {
                        case IGRAC1_BONUS:
                            if (!jeIgrac1) {
                                zaustaviTajmer();
                                mojaRunda = true;
                                bonusRunda = true;
                                binding.btnOdgovori.setEnabled(true);
                                prikaziSveKorake();
                                pokrniTajmerBonusa();
                                prikaziStatus("Protivnik nije pogodio! Imas 10s za 5 bodova!");
                            } else {
                                prikaziCekanje("Protivnik pokusava pogoditi...");
                            }
                            break;

                        case IGRAC2_IGRA:
                        case IGRAC2_IGRA_RUNDA2:
                            if (!jeIgrac1) {
                                zaustaviTajmer();
                                trenutniKorak = 0;
                                mojaRunda = true;
                                bonusRunda = false;
                                pocniMojuRundu();
                            } else {
                                prikaziCekanje("Protivnik igra svoju rundu...");
                            }
                            break;

                        case IGRAC2_BONUS:
                        case IGRAC2_BONUS_RUNDA2:
                            if (jeIgrac1) {
                                zaustaviTajmer();
                                mojaRunda = true;
                                bonusRunda = true;
                                binding.btnOdgovori.setEnabled(true);
                                prikaziSveKorake();
                                pokrniTajmerBonusa();
                                prikaziStatus("Protivnik nije pogodio! Imas 10s za 5 bodova!");
                            } else {
                                prikaziCekanje("Protivnik pokusava pogoditi...");
                            }
                            break;

                        case IGRAC1_BONUS_RUNDA2:
                            if (jeIgrac1) {
                                zaustaviTajmer();
                                mojaRunda = true;
                                bonusRunda = true;
                                binding.btnOdgovori.setEnabled(true);
                                prikaziSveKorake();
                                pokrniTajmerBonusa();
                                prikaziStatus("Protivnik nije pogodio! Imas 10s za 5 bodova!");
                            } else {
                                prikaziCekanje("Protivnik pokusava pogoditi...");
                            }
                            break;

                        case ZAVRSENA:
                            zaustaviTajmer();
                            mojaRunda = false;
                            binding.btnOdgovori.setEnabled(false);

                            int mojiB = jeIgrac1 ?
                                    (b1 != null ? b1.intValue() : 0) :
                                    (b2 != null ? b2.intValue() : 0);

                            prikaziStatus("Igra zavrsena! Bodovi: " + mojiB);

                            Bundle result = new Bundle();
                            result.putInt("bodovi", mojiB);
                            if (getParentFragmentManager() != null) {
                                getParentFragmentManager()
                                        .setFragmentResult("korakPoKorakZavrsen", result);
                            }
                            break;
                    }
                });
    }

    private void pocniMojuRundu() {
        prikaziStatus("Tvoja runda!");
        trenutniKorak = 0;
        prikaziKorak(0);
        binding.btnOdgovori.setEnabled(true);
        mojaRunda = true;
        pokrniTajmerRunde();
    }

    private void prikaziCekanje(String poruka) {
        mojaRunda = false;
        binding.btnOdgovori.setEnabled(false);
        binding.tvTajmer.setText("Cekaj...");
        prikaziStatus(poruka);
        sakriSveKorake();
    }

    private void pokrniTajmerRunde() {
        zaustaviTajmer();
        tajmer = new CountDownTimer(TRAJANJE_RUNDE, 1000) {
            @Override
            public void onTick(long ms) {
                int sekunde = (int)(ms / 1000);
                binding.tvTajmer.setText(sekunde + "s");
                int noviKorak = (int)((TRAJANJE_RUNDE - ms) / 10000);
                if (noviKorak > trenutniKorak && noviKorak <= 6) {
                    trenutniKorak = noviKorak;
                    prikaziKorak(trenutniKorak);
                }
            }

            @Override
            public void onFinish() {
                mojaRunda = false;
                binding.btnOdgovori.setEnabled(false);
                binding.tvTajmer.setText("0s");
                tajmerIstekao();
            }
        }.start();
    }

    private void tajmerIstekao() {
        String noviStatus;
        switch (trenutniStatus) {
            case IGRAC1_IGRA:
                noviStatus = IGRAC1_BONUS;
                break;
            case IGRAC2_IGRA:
                noviStatus = IGRAC2_BONUS;
                break;
            case IGRAC2_IGRA_RUNDA2:
                noviStatus = IGRAC2_BONUS_RUNDA2;
                break;
            default:
                noviStatus = ZAVRSENA;
                break;
        }

        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .update("statusKorakPoKorak", noviStatus);
    }

    private void pokrniTajmerBonusa() {
        zaustaviTajmer();
        tajmer = new CountDownTimer(TRAJANJE_BONUSA, 1000) {
            @Override
            public void onTick(long ms) {
                binding.tvTajmer.setText("Bonus: " + ms / 1000 + "s");
            }

            @Override
            public void onFinish() {
                mojaRunda = false;
                binding.btnOdgovori.setEnabled(false);
                bonusTajmerIstekao();
            }
        }.start();
    }

    private void bonusTajmerIstekao() {
        String noviStatus;
        switch (trenutniStatus) {
            case IGRAC1_BONUS:
                noviStatus = IGRAC2_IGRA_RUNDA2;
                break;
            case IGRAC2_BONUS:
            case IGRAC2_BONUS_RUNDA2:
            case IGRAC1_BONUS_RUNDA2:
                noviStatus = ZAVRSENA;
                break;
            default:
                noviStatus = ZAVRSENA;
                break;
        }

        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .update("statusKorakPoKorak", noviStatus);
    }

    private void provjeriOdgovor(String odgovor) {
        if (trenutnoPitanje == null) return;

        if (odgovor.equalsIgnoreCase(trenutnoPitanje.getRjesenje())) {
            zaustaviTajmer();
            mojaRunda = false;
            binding.btnOdgovori.setEnabled(false);

            int osvојeniBodovi = bonusRunda ?
                    BODOVI_BONUS :
                    MAX_BODOVA - (trenutniKorak * ODBITAK_PO_KORAKU);

            bodovi += osvојeniBodovi;
            binding.tvBodovi.setText("Bodovi: " + bodovi);

            String poljeB = jeIgrac1 ? "bodovi1KorakPoKorak" : "bodovi2KorakPoKorak";
            FirebaseFirestore.getInstance()
                    .collection("partije").document(partijaId)
                    .update(poljeB, bodovi)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(),
                                "Tacno! +" + osvојeniBodovi + " bodova!",
                                Toast.LENGTH_SHORT).show();
                        napraviSledeci();
                    });
        } else {
            Toast.makeText(getContext(), "Netacno!", Toast.LENGTH_SHORT).show();
            binding.etOdgovor.setText("");
        }
    }

    private void napraviSledeci() {
        String noviStatus;
        switch (trenutniStatus) {
            case IGRAC1_IGRA:
                noviStatus = IGRAC2_IGRA;
                prikaziCekanje("Cekaj Igraca 2...");
                break;
            case IGRAC1_BONUS:
                noviStatus = IGRAC2_IGRA_RUNDA2;
                if (!jeIgrac1) prikaziCekanje("Cekaj...");
                break;
            default:
                noviStatus = ZAVRSENA;
                break;
        }

        FirebaseFirestore.getInstance()
                .collection("partije").document(partijaId)
                .update("statusKorakPoKorak", noviStatus);
    }

    private void prikaziSveKorake() {
        if (trenutnoPitanje == null) return;
        String[] koraci = trenutnoPitanje.getKoraci();
        binding.tvKorak1.setText("Korak 1: " + koraci[0]);
        binding.tvKorak1.setVisibility(View.VISIBLE);
        binding.tvKorak2.setText("Korak 2: " + koraci[1]);
        binding.tvKorak2.setVisibility(View.VISIBLE);
        binding.tvKorak3.setText("Korak 3: " + koraci[2]);
        binding.tvKorak3.setVisibility(View.VISIBLE);
        binding.tvKorak4.setText("Korak 4: " + koraci[3]);
        binding.tvKorak4.setVisibility(View.VISIBLE);
        binding.tvKorak5.setText("Korak 5: " + koraci[4]);
        binding.tvKorak5.setVisibility(View.VISIBLE);
        binding.tvKorak6.setText("Korak 6: " + koraci[5]);
        binding.tvKorak6.setVisibility(View.VISIBLE);
        binding.tvKorak7.setText("Korak 7: " + koraci[6]);
        binding.tvKorak7.setVisibility(View.VISIBLE);
    }

    private void prikaziStatus(String poruka) {
        if (getContext() != null)
            Toast.makeText(getContext(), poruka, Toast.LENGTH_LONG).show();
    }

    private void zaustaviTajmer() {
        if (tajmer != null) {
            tajmer.cancel();
            tajmer = null;
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
        if (binding == null) return;
        binding.tvKorak1.setVisibility(View.GONE);
        binding.tvKorak2.setVisibility(View.GONE);
        binding.tvKorak3.setVisibility(View.GONE);
        binding.tvKorak4.setVisibility(View.GONE);
        binding.tvKorak5.setVisibility(View.GONE);
        binding.tvKorak6.setVisibility(View.GONE);
        binding.tvKorak7.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        zaustaviTajmer();
        if (listenerRegistration != null) listenerRegistration.remove();
        binding = null;
    }
}