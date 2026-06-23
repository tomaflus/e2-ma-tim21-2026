package com.elfak.slagalica.fragments.rang;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentNagradaBinding;

public class NagradaFragment extends Fragment {

    private FragmentNagradaBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNagradaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        int tokeniNedelja = args.getInt("tokeniNedelja", 0);
        int pozicijaNedelja = args.getInt("pozicijaNedelja", -1);
        int tokeniMesec = args.getInt("tokeniMesec", 0);
        int pozicijaMesec = args.getInt("pozicijaMesec", -1);
        int ukupno = tokeniNedelja + tokeniMesec;

        if (pozicijaNedelja > 0) {
            binding.tvNagradaNedelja.setVisibility(View.VISIBLE);
            binding.tvNagradaNedelja.setText(
                    pozicijaNedelja + ". mesto na nedeljnoj rang listi — +" + tokeniNedelja + " tokena");
        }
        if (pozicijaMesec > 0) {
            binding.tvNagradaMesec.setVisibility(View.VISIBLE);
            binding.tvNagradaMesec.setText(
                    pozicijaMesec + ". mesto na mesečnoj rang listi — +" + tokeniMesec + " tokena");
        }
        binding.tvUkupno.setText(getString(R.string.nagrada_ukupno_tokena, ukupno));

        // Pokretanje animacije tek kad je view izmjeren i prikvačen na prozor
        binding.ivNagrada.post(this::animiraNagradu);
        pustitiZvukIVibraciju();

        binding.btnZatvori.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
    }

    private void animiraNagradu() {
        if (binding == null) return;
        // Polazna stanja su postavljena u XML-u: scaleX=0, scaleY=0, alpha=0
        binding.ivNagrada.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(2.5f))
                .start();
    }

    private void pustitiZvukIVibraciju() {
        // Ringtone — radi kad zvuk nije utišan
        try {
            Uri zvuk = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), zvuk);
            if (ringtone != null) ringtone.play();
        } catch (Exception ignored) {}

        // Vibracija 200 ms — fallback za nečujni/tihov režim
        try {
            Vibrator vibrator = (Vibrator) requireContext()
                    .getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(
                            200, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(200);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
