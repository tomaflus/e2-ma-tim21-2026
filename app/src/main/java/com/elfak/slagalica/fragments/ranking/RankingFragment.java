package com.elfak.slagalica.fragments.ranking;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.databinding.FragmentRankingBinding;
import com.elfak.slagalica.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RankingFragment extends Fragment {
    private FragmentRankingBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvRanking.setLayoutManager(new LinearLayoutManager(getContext()));
        
        setupCycleDates();
        loadRanking();
        
        refreshRunnable = new Runnable() {
            @Override public void run() {
                loadRanking();
                handler.postDelayed(this, 120000); // 2 min
            }
        };
        handler.postDelayed(refreshRunnable, 120000);
    }

    private void setupCycleDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        String startW = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String endW = sdf.format(cal.getTime());
        
        Calendar calM = Calendar.getInstance();
        calM.set(Calendar.DAY_OF_MONTH, 1);
        String startM = sdf.format(calM.getTime());
        calM.set(Calendar.DAY_OF_MONTH, calM.getActualMaximum(Calendar.DAY_OF_MONTH));
        String endM = sdf.format(calM.getTime());

        binding.tvCycleInfo.setText("Nedeljni: " + startW + " - " + endW + "\nMesečni: " + startM + " - " + endM);
    }

    private void loadRanking() {
        db.collection("users")
                .orderBy("mesecniBodovi", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<User> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User u = doc.toObject(User.class);
                        u.setId(doc.getId());
                        list.add(u);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}