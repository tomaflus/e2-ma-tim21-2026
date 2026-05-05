package com.elfak.slagalica.fragments.games;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elfak.slagalica.databinding.FragmentAsocijacijeBinding;

public class AsocijacijeFragment extends Fragment {

    private static final long TRAJANJE = 120_000; // 2 minute

    private FragmentAsocijacijeBinding binding;
    private AsocijacijeViewModel viewModel;
    private CountDownTimer tajmer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAsocijacijeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AsocijacijeViewModel.class);
        viewModel.score.observe(getViewLifecycleOwner(),
                s -> binding.tvBodovi.setText("Bodovi: " + s));

        pokrniTajmer();
    }

    private void pokrniTajmer() {
        tajmer = new CountDownTimer(TRAJANJE, 1000) {
            @Override
            public void onTick(long ms) {
                long sec = ms / 1000;
                binding.tvTajmer.setText(
                        String.format("⏱ %d:%02d", sec / 60, sec % 60));
            }

            @Override
            public void onFinish() {
                binding.tvTajmer.setText("⏱ 0:00");
                binding.btnPogodi.setEnabled(false);
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tajmer != null) tajmer.cancel();
        binding = null;
    }
}
