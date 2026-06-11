package com.elfak.slagalica.fragments.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.elfak.slagalica.databinding.FragmentIgraBinding;

public class IgraFragment extends Fragment {
    private FragmentIgraBinding binding;
    private String partijaId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentIgraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            partijaId = getArguments().getString("partijaId");
        }
        // Logic for game cycle starting here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}