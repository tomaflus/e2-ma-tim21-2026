package com.elfak.slagalica.fragments.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentNotifikacijeBinding;
import com.elfak.slagalica.databinding.ItemNotifikacijaBinding;

import java.util.ArrayList;
import java.util.List;

public class NotifikacijeFragment extends Fragment {

    private FragmentNotifikacijeBinding binding;
    private NotifikacijeAdapter adapter;
    private NotifikacijeViewModel viewModel;

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

        adapter = new NotifikacijeAdapter(new ArrayList<>());
        binding.rvNotifikacije.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifikacije.setAdapter(adapter);

        viewModel.getFiltrirane().observe(getViewLifecycleOwner(),
                stavke -> adapter.postaviStavke(stavke));

        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFilterNeprocitane)
                viewModel.postaviFilter(NotifikacijeViewModel.Filter.NEPROCITANE);
            else if (id == R.id.chipFilterProcitane)
                viewModel.postaviFilter(NotifikacijeViewModel.Filter.PROCITANE);
            else
                viewModel.postaviFilter(NotifikacijeViewModel.Filter.SVE);
        });

        viewModel.getAktivniFilter().observe(getViewLifecycleOwner(), filter -> {
            int id = filter == NotifikacijeViewModel.Filter.NEPROCITANE ? R.id.chipFilterNeprocitane
                    : filter == NotifikacijeViewModel.Filter.PROCITANE ? R.id.chipFilterProcitane
                    : R.id.chipFilterSve;
            if (binding.chipGroupFilter.getCheckedChipId() != id)
                binding.chipGroupFilter.check(id);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static class NotifikacijeAdapter extends RecyclerView.Adapter<NotifikacijeAdapter.ViewHolder> {

        private final List<Notifikacija> stavke;

        NotifikacijeAdapter(List<Notifikacija> stavke) {
            this.stavke = stavke;
        }

        void postaviStavke(List<Notifikacija> novoStavke) {
            stavke.clear();
            stavke.addAll(novoStavke);
            notifyDataSetChanged();
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
            holder.bind(stavke.get(position));
        }

        @Override
        public int getItemCount() {
            return stavke.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemNotifikacijaBinding binding;

            ViewHolder(ItemNotifikacijaBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Notifikacija notifikacija) {
                binding.tvIkona.setText(notifikacija.ikona);
                binding.tvNaslov.setText(notifikacija.naslov);
                binding.tvSadrzaj.setText(notifikacija.sadrzaj);
                binding.tvDatumVrijeme.setText(notifikacija.datumVrijeme);
                binding.viewNeprocitano.setVisibility(
                        notifikacija.procitana ? View.INVISIBLE : View.VISIBLE);
            }
        }
    }
}
