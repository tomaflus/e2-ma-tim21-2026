package com.elfak.slagalica.fragments.izazov;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.R;
import com.elfak.slagalica.adapters.IzazovAdapter;
import com.elfak.slagalica.databinding.FragmentIzazovBinding;
import com.elfak.slagalica.model.Izazov;
import com.elfak.slagalica.repository.AuthRepository;
import com.elfak.slagalica.repository.IzazovRepository;
import com.elfak.slagalica.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IzazovFragment extends Fragment {

    private FragmentIzazovBinding binding;
    private IzazovRepository izazovRepository;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private IzazovAdapter adapter;

    private String mojId;
    private String mojIme;
    private String mojRegion;
    private String aktivniIzazovId = null;
    private boolean izazovDialogPrikazan = false;
    private boolean rezultatiPrikazani = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentIzazovBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        izazovRepository = new IzazovRepository();
        authRepository = new AuthRepository();
        userRepository = new UserRepository();

        mojId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new IzazovAdapter(new ArrayList<>(), mojId, this::prihvatiIzazov);
        binding.rvIzazovi.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvIzazovi.setAdapter(adapter);

        userRepository.dohvatiKorisnika(
                user -> {
                    mojIme = user.getKorisnickoIme() != null ?
                            user.getKorisnickoIme() : user.getEmail();
                    mojRegion = user.getRegion() != null ?
                            user.getRegion() : "Opsti";
                    ucitajIzazove();
                    slušajMojeIzazove();
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );

        binding.btnKreirajIzazov.setOnClickListener(v -> {
            String zvezdeStr = binding.etZvezdeUlog.getText().toString().trim();
            String tokeniStr = binding.etTokeniUlog.getText().toString().trim();

            if (zvezdeStr.isEmpty() || tokeniStr.isEmpty()) {
                Toast.makeText(getContext(), "Unesite ulog!", Toast.LENGTH_SHORT).show();
                return;
            }

            int zvezde = Integer.parseInt(zvezdeStr);
            int tokeni = Integer.parseInt(tokeniStr);

            if (zvezde < 0 || zvezde > 10) {
                Toast.makeText(getContext(),
                        "Zvezde moraju biti izmedju 0 i 10!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (tokeni < 0 || tokeni > 2) {
                Toast.makeText(getContext(),
                        "Tokeni moraju biti izmedju 0 i 2!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mojRegion == null) {
                Toast.makeText(getContext(), "Region nije ucitan!", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnKreirajIzazov.setEnabled(false);

            izazovRepository.kreirajIzazov(mojIme, mojRegion, zvezde, tokeni,
                    izazovId -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                aktivniIzazovId = izazovId;
                                binding.btnKreirajIzazov.setEnabled(true);
                                binding.etZvezdeUlog.setText("");
                                binding.etTokeniUlog.setText("");
                                Toast.makeText(getContext(),
                                        "Izazov postavljen! Cekajte igrace da prihvate.",
                                        Toast.LENGTH_LONG).show();
                                pratiIzazov(izazovId);
                            });
                        }
                    },
                    poruka -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnKreirajIzazov.setEnabled(true);
                                Toast.makeText(getContext(),
                                        "Greska: " + poruka, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
    }

    // Sluša sve izazove u regiji u realnom vremenu
    private void slušajMojeIzazove() {
        FirebaseFirestore.getInstance()
                .collection("izazovi")
                .whereEqualTo("region", mojRegion)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;

                    for (var doc : snapshot.getDocuments()) {
                        Izazov izazov = doc.toObject(Izazov.class);
                        if (izazov == null) continue;
                        izazov.setId(doc.getId());

                        // Provjeri da li je korisnik u ovom izazovu i status je U_TOKU
                        if (izazov.getStatus().equals("U_TOKU") &&
                                izazov.getIgraciIds() != null &&
                                izazov.getIgraciIds().contains(mojId) &&
                                !izazovDialogPrikazan) {

                            izazovDialogPrikazan = true;

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        prikaziIgraIzazova(izazov));
                            }
                            break;
                        }

                        // Provjeri da li je izazov zavrsen
                        if (izazov.getStatus().equals("ZAVRSEN") &&
                                izazov.getIgraciIds() != null &&
                                izazov.getIgraciIds().contains(mojId) &&
                                izazov.getRezultati() != null &&
                                izazov.getRezultati().containsKey(mojId) &&
                                !rezultatiPrikazani) {

                            rezultatiPrikazani = true; // ← spriječava ponavljanje

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        prikaziRezultate(izazov));
                            }
                            break;
                        }
                    }
                });
    }

    private void pratiIzazov(String izazovId) {
        izazovRepository.slušajIzazov(izazovId,
                izazov -> {
                    if (izazov.getStatus().equals("ZAVRSEN") && !rezultatiPrikazani) {
                        rezultatiPrikazani = true;
                        prikaziRezultate(izazov);
                    } else if (izazov.getStatus().equals("U_TOKU") &&
                            !izazovDialogPrikazan) {
                        izazovDialogPrikazan = true;
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    prikaziIgraIzazova(izazov));
                        }
                    } else if (izazov.getIgraciIds().size() > 1 &&
                            izazov.getKreatorId().equals(mojId) &&
                            izazov.getStatus().equals("AKTIVAN")) {
                        prikaziDugmePokreni(izazovId, izazov);
                    }
                },
                poruka -> {}
        );
    }

    private void prikaziDugmePokreni(String izazovId, Izazov izazov) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Pokrenite izazov")
                        .setMessage("Igraci koji su se pridruzili: " +
                                izazov.getIgraciIds().size() + "/4\n" +
                                "Zelite li pokrenuti izazov?")
                        .setPositiveButton("Pokreni", (dialog, which) ->
                                izazovRepository.pokreniIzazov(izazovId,
                                        id -> {
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(() ->
                                                        Toast.makeText(getContext(),
                                                                "Izazov pokrenut!",
                                                                Toast.LENGTH_SHORT).show());
                                            }
                                        },
                                        poruka -> Toast.makeText(getContext(),
                                                "Greska: " + poruka,
                                                Toast.LENGTH_SHORT).show()))
                        .setNegativeButton("Cekaj jos", null)
                        .show()
        );
    }

    private void ucitajIzazove() {
        izazovRepository.slušajIzazove(mojRegion,
                izazovi -> adapter.azurirajIzazove(izazovi),
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );
    }

    private void prikaziIgraIzazova(Izazov izazov) {
        if (getActivity() == null || !isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Izazov pokrenut!")
                .setMessage("Izazov od " + izazov.getKreatorIme() +
                        " je pokrenut!\nOsvajate " +
                        (int)(izazov.getZvezdeUlog() *
                                izazov.getIgraciIds().size() * 0.75) +
                        " zvezda ako pobijedite!\n\nKliknite Igraj da pocnete!")
                .setPositiveButton("Igraj!", (dialog, which) -> {
                    Bundle args = new Bundle();
                    args.putString("izazovId", izazov.getId());
                    args.putBoolean("jeIzazov", true);
                    androidx.navigation.Navigation
                            .findNavController(requireView())
                            .navigate(R.id.action_izazovFragment_to_igraFragment, args);
                })
                .setCancelable(false)
                .show();
    }

    private void prihvatiIzazov(Izazov izazov) {
        userRepository.dohvatiKorisnika(
                user -> {
                    if (user.getZvezde() < izazov.getZvezdeUlog()) {
                        Toast.makeText(getContext(),
                                "Nemate dovoljno zvezda!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (user.getTokeni() < izazov.getTokeniUlog()) {
                        Toast.makeText(getContext(),
                                "Nemate dovoljno tokena!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Prihvatiti izazov?")
                                        .setMessage("Ulozit cete " + izazov.getZvezdeUlog() +
                                                " zvezda i " + izazov.getTokeniUlog() +
                                                " tokena.\nAko pobijedite, osvajate " +
                                                (int)(izazov.getZvezdeUlog() *
                                                        izazov.getIgraciIds().size() * 0.75) +
                                                " zvezda!")
                                        .setPositiveButton("Prihvati", (dialog, which) ->
                                                izazovRepository.prihvatiIzazov(
                                                        izazov.getId(), mojIme,
                                                        id -> {
                                                            if (getActivity() != null) {
                                                                getActivity().runOnUiThread(() ->
                                                                        Toast.makeText(getContext(),
                                                                                "Izazov prihvacen! Cekajte pokretanje.",
                                                                                Toast.LENGTH_LONG).show());
                                                            }
                                                        },
                                                        poruka -> {
                                                            if (getActivity() != null) {
                                                                getActivity().runOnUiThread(() ->
                                                                        Toast.makeText(getContext(),
                                                                                "Greska: " + poruka,
                                                                                Toast.LENGTH_SHORT).show());
                                                            }
                                                        }))
                                        .setNegativeButton("Odustani", null)
                                        .show()
                        );
                    }
                },
                poruka -> Toast.makeText(getContext(),
                        "Greska: " + poruka, Toast.LENGTH_SHORT).show()
        );
    }

    private void prikaziRezultate(Izazov izazov) {
        if (getActivity() == null || izazov.getRezultati() == null) return;

        List<Map.Entry<String, Integer>> sortirani =
                new ArrayList<>(izazov.getRezultati().entrySet());
        sortirani.sort((a, b) -> b.getValue() - a.getValue());

        StringBuilder sb = new StringBuilder();
        int ukupnoZvezde = izazov.getZvezdeUlog() * izazov.getIgraciIds().size();

        for (int i = 0; i < sortirani.size(); i++) {
            String igracId = sortirani.get(i).getKey();
            int bodovi = sortirani.get(i).getValue();

            int indeks = izazov.getIgraciIds().indexOf(igracId);
            String ime = indeks >= 0 && indeks < izazov.getIgraciImena().size() ?
                    izazov.getIgraciImena().get(indeks) : "Nepoznat";

            sb.append(i + 1).append(". ").append(ime)
                    .append(": ").append(bodovi).append(" bodova");

            if (i == 0) {
                sb.append(" → Pobjednik! +")
                        .append((int)(ukupnoZvezde * 0.75))
                        .append(" zvezda");
            } else if (i == 1) {
                sb.append(" → Nazad ulozeno: +")
                        .append(izazov.getZvezdeUlog())
                        .append(" zvezda");
            }
            sb.append("\n");
        }

        String finalTekst = sb.toString();

        getActivity().runOnUiThread(() -> {
            // KLJUCNA PROVJERA — binding mora postojati
            if (binding == null || !isAdded()) return;

            binding.tvRezultati.setText(finalTekst);
            binding.layoutRezultati.setVisibility(View.VISIBLE);
            binding.rvIzazovi.setVisibility(View.GONE);

            binding.btnZatvoriRezultate.setOnClickListener(v -> {
                if (binding == null) return;
                binding.layoutRezultati.setVisibility(View.GONE);
                binding.rvIzazovi.setVisibility(View.VISIBLE);
                rezultatiPrikazani = false;

                FirebaseFirestore.getInstance()
                        .collection("izazovi")
                        .document(izazov.getId())
                        .delete();
            });
        });

        // Rasporedi nagrade (samo kreator)
        if (izazov.getKreatorId().equals(mojId)) {
            izazovRepository.rasporediNagrade(izazov.getId(), id -> {}, poruka -> {});
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        izazovRepository.ukloniListener();
        binding = null;
    }
}