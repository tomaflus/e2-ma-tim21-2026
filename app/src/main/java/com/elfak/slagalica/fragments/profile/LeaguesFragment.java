package com.elfak.slagalica.fragments.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.League;

public class LeaguesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leagues, container, false);
        LinearLayout list = view.findViewById(R.id.llLeaguesList);

        for (League l : League.values()) {
            View item = inflater.inflate(R.layout.item_friend, list, false); // Reuse item layout style
            ((TextView)item.findViewById(R.id.tvUsername)).setText(l.getName());
            ((TextView)item.findViewById(R.id.tvLeague)).setText("Potrebno zvezda: " + l.getMinStars() + "\nBonus tokeni: +" + l.getBonusTokens());
            ((ImageView)item.findViewById(R.id.ivAvatar)).setImageResource(l.getIconRes());
            item.findViewById(R.id.btnInvite).setVisibility(View.GONE);
            list.addView(item);
        }

        return view;
    }
}