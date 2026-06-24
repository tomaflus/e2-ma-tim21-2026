package com.elfak.slagalica.fragments.misije;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elfak.slagalica.databinding.FragmentDnevneMisijeBinding;
import com.elfak.slagalica.model.DnevneMisije;
import com.elfak.slagalica.service.DnevneMisijeService;
import com.elfak.slagalica.util.CiklusUtil;
import com.google.firebase.auth.FirebaseAuth;

public class DnevneMisijeFragment extends Fragment {

    private FragmentDnevneMisijeBinding binding;
    private DnevneMisijeService service;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDnevneMisijeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        service = new DnevneMisijeService();
        binding.tvDatum.setText(CiklusUtil.trenutniDanId());
        ucitaj();
    }

    @Override
    public void onResume() {
        super.onResume();
        ucitaj();
    }

    private void ucitaj() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        service.ucitajMisije(uid, this::prikaziMisije);
    }

    private void prikaziMisije(DnevneMisije m) {
        if (binding == null) return;
        postaviStatus(binding.tvStatus1, m.isPobedaUradjeno());
        postaviStatus(binding.tvStatus2, m.isPorukaCetUradjeno());
        postaviStatus(binding.tvStatus3, m.isPrijateljskaUradjeno());
        postaviStatus(binding.tvStatus4, m.isTurnirPobedaUradjeno());
        postaviStatus(binding.tvStatusBonus, m.isBonusDodeljen());
    }

    private void postaviStatus(android.widget.TextView tv, boolean uradjeno) {
        if (uradjeno) {
            tv.setText("✓");
            tv.setTextColor(requireContext().getColor(com.elfak.slagalica.R.color.zelenaTacno));
        } else {
            tv.setText("○");
            tv.setTextColor(requireContext().getColor(com.elfak.slagalica.R.color.textSekundarna));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
