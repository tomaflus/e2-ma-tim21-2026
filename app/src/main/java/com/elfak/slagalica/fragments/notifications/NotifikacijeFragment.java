package com.elfak.slagalica.fragments.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentNotifikacijeBinding;
import com.elfak.slagalica.databinding.ItemNotifikacijaBinding;
import com.elfak.slagalica.model.Notifikacija;
import com.elfak.slagalica.viewModels.notifications.NotifikacijeViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotifikacijeFragment extends Fragment {

    private FragmentNotifikacijeBinding binding;
    private NotifikacijeAdapter adapter;
    private NotifikacijeViewModel viewModel;
    private String uid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotifikacijeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NotifikacijeViewModel.class);

        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new NotifikacijeAdapter(new ArrayList<>());
        binding.rvNotifikacije.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifikacije.setAdapter(adapter);

        viewModel.getFiltrirane().observe(getViewLifecycleOwner(),
                stavke -> adapter.postaviStavke(stavke));

        // Klik na stavku: selekcijski rezim → toggle; nepročitana → markira; pročitana → navigira
        adapter.setOnItemKlikListener(notifikacija -> {
            if (adapter.jeUSelekcijiRezimu()) {
                adapter.toggleSelekcija(notifikacija);
            } else if (!notifikacija.procitana) {
                viewModel.markirajKaoProcitanu(uid, notifikacija.dokumentId);
            } else {
                navigirajZaTip(notifikacija);
            }
        });

        // Dugi pritisak ulazi u selekcijski rezim
        adapter.setOnItemDugiPritisakListener(notifikacija -> {
            if (!adapter.jeUSelekcijiRezimu()) {
                adapter.ulaziUSelekcijiRezim();
                adapter.toggleSelekcija(notifikacija);
                prikaziSelekcijaToolbar(true);
            }
        });

        // Klik van RecyclerView izlazi iz selekcijskog rezima
        binding.getRoot().setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && adapter.jeUSelekcijiRezimu()) {
                // Provjeri da li je klik van RecyclerView
                int[] rvLokacija = new int[2];
                binding.rvNotifikacije.getLocationOnScreen(rvLokacija);
                float x = event.getRawX();
                float y = event.getRawY();
                boolean vanRv = x < rvLokacija[0] || x > rvLokacija[0] + binding.rvNotifikacije.getWidth()
                        || y < rvLokacija[1] || y > rvLokacija[1] + binding.rvNotifikacije.getHeight();
                if (vanRv) {
                    izlazIzSelekcijeRezima();
                }
            }
            return false;
        });

        binding.btnObrisiSelektovane.setOnClickListener(v -> {
            List<String> ids = adapter.dohvatiSelektovaneId();
            if (!ids.isEmpty()) {
                viewModel.obrisiNotifikacije(uid, ids);
                izlazIzSelekcijeRezima();
            }
        });

        binding.btnIzaberiSve.setOnClickListener(v -> {
            adapter.izaberiSve();
        });

        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFilterNeprocitane)
                viewModel.postaviFilter(NotifikacijeViewModel.Filter.NEPROCITANE);
            else if (id == R.id.chipFilterProcitane)
                viewModel.postaviFilter(NotifikacijeViewModel.Filter.PROCITANE);
            else
                viewModel.postaviFilter(NotifikacijeViewModel.Filter.SVE);

            // Promijena filtera izlazi iz selekcijskog rezima
            izlazIzSelekcijeRezima();
        });

        viewModel.getAktivniFilter().observe(getViewLifecycleOwner(), filter -> {
            int id = filter == NotifikacijeViewModel.Filter.NEPROCITANE ? R.id.chipFilterNeprocitane
                    : filter == NotifikacijeViewModel.Filter.PROCITANE ? R.id.chipFilterProcitane
                    : R.id.chipFilterSve;
            if (binding.chipGroupFilter.getCheckedChipId() != id)
                binding.chipGroupFilter.check(id);
        });
    }

    private void navigirajZaTip(com.elfak.slagalica.model.Notifikacija n) {
        if (n.tip == null || !isAdded()) return;
        switch (n.tip) {
            case "turnir":
                if (n.turnirId != null) {
                    Bundle args = new Bundle();
                    args.putString("turnirId", n.turnirId);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_notifikacijeFragment_to_turnirBracketFragment, args);
                } else {
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_notifikacijeFragment_to_turnirFragment);
                }
                break;
            case "nagrade":
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_notifikacijeFragment_to_homeFragment);
                break;
            case "misije":
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_notifikacijeFragment_to_dnevneMisijeFragment);
                break;
        }
    }

    private void prikaziSelekcijaToolbar(boolean prikazi) {
        binding.llSelekcijaToolbar.setVisibility(prikazi ? View.VISIBLE : View.GONE);
    }

    private void izlazIzSelekcijeRezima() {
        adapter.izlaziIzSelekcijeRezima();
        prikaziSelekcijaToolbar(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    static class NotifikacijeAdapter extends RecyclerView.Adapter<NotifikacijeAdapter.ViewHolder> {

        interface OnItemKlikListener { void onKlik(Notifikacija n); }
        interface OnItemDugiPritisakListener { void onDugiPritisak(Notifikacija n); }

        private final List<Notifikacija> stavke;
        private OnItemKlikListener onItemKlikListener;
        private OnItemDugiPritisakListener onItemDugiPritisakListener;
        private boolean selekcijaRezim = false;
        private final Set<String> selektovani = new HashSet<>(); // set documentId-eva

        NotifikacijeAdapter(List<Notifikacija> stavke) {
            this.stavke = stavke;
        }

        void setOnItemKlikListener(OnItemKlikListener l) { this.onItemKlikListener = l; }
        void setOnItemDugiPritisakListener(OnItemDugiPritisakListener l) { this.onItemDugiPritisakListener = l; }

        void postaviStavke(List<Notifikacija> nove) {
            stavke.clear();
            stavke.addAll(nove);
            selektovani.retainAll(getDokumentIds());
            notifyDataSetChanged();
        }

        boolean jeUSelekcijiRezimu() { return selekcijaRezim; }

        void ulaziUSelekcijiRezim() {
            selekcijaRezim = true;
            notifyDataSetChanged();
        }

        void izlaziIzSelekcijeRezima() {
            selekcijaRezim = false;
            selektovani.clear();
            notifyDataSetChanged();
        }

        void toggleSelekcija(Notifikacija n) {
            if (n.dokumentId == null) return;
            if (selektovani.contains(n.dokumentId)) selektovani.remove(n.dokumentId);
            else selektovani.add(n.dokumentId);
            notifyDataSetChanged();
        }

        void izaberiSve() {
            for (Notifikacija n : stavke) {
                if (n.dokumentId != null) selektovani.add(n.dokumentId);
            }
            notifyDataSetChanged();
        }

        List<String> dohvatiSelektovaneId() {
            return new ArrayList<>(selektovani);
        }

        private Set<String> getDokumentIds() {
            Set<String> ids = new HashSet<>();
            for (Notifikacija n : stavke) if (n.dokumentId != null) ids.add(n.dokumentId);
            return ids;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemNotifikacijaBinding itemBinding = ItemNotifikacijaBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notifikacija n = stavke.get(position);
            holder.bind(n, selekcijaRezim, selektovani.contains(n.dokumentId));
            holder.itemView.setOnClickListener(v -> {
                if (onItemKlikListener != null) onItemKlikListener.onKlik(n);
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (onItemDugiPritisakListener != null) onItemDugiPritisakListener.onDugiPritisak(n);
                return true;
            });
        }

        @Override
        public int getItemCount() { return stavke.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemNotifikacijaBinding binding;

            ViewHolder(ItemNotifikacijaBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Notifikacija n, boolean selekcijaRezim, boolean selektovana) {
                binding.tvIkona.setText(n.ikona);
                binding.tvNaslov.setText(n.naslov);
                binding.tvSadrzaj.setText(n.sadrzaj);
                binding.tvDatumVrijeme.setText(n.datumVrijeme);
                binding.viewNeprocitano.setVisibility(n.procitana ? View.INVISIBLE : View.VISIBLE);

                // Selekcijski rezim
                binding.cbSelektovana.setVisibility(selekcijaRezim ? View.VISIBLE : View.GONE);
                binding.cbSelektovana.setChecked(selektovana);

                // Highlight selektovane stavke
                binding.getRoot().setBackgroundColor(selektovana
                        ? 0x33FFFFFF  // bijeli overlay
                        : android.graphics.Color.TRANSPARENT);
            }
        }
    }
}
