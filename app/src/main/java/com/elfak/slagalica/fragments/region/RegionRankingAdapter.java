package com.elfak.slagalica.fragments.region;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.Region;

import java.util.List;
import java.util.Map;

public class RegionRankingAdapter extends RecyclerView.Adapter<RegionRankingAdapter.ViewHolder> {
    private final List<Map.Entry<Region, Integer>> data;
    private final String userRegion;

    public RegionRankingAdapter(List<Map.Entry<Region, Integer>> data, String userRegion) {
        this.data = data;
        this.userRegion = userRegion;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_region_ranking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<Region, Integer> entry = data.get(position);
        Region region = entry.getKey();
        
        holder.tvRank.setText((position + 1) + ".");
        holder.ivIcon.setImageResource(region.getIconRes());
        
        int regionColor = region.getColor();
        int opaqueColor = android.graphics.Color.rgb(
                android.graphics.Color.red(regionColor),
                android.graphics.Color.green(regionColor),
                android.graphics.Color.blue(regionColor)
        );
        holder.vIconBg.getBackground().setTint(opaqueColor);

        holder.tvName.setText(region.getFullName());
        holder.tvStars.setText(entry.getValue() + " zvezda");

        String uRegion = userRegion != null ? userRegion.trim() : "";
        if (region.getFullName().equalsIgnoreCase(uRegion) || 
            region.getShortName().equalsIgnoreCase(uRegion)) {
            holder.tvYourRegion.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundResource(R.drawable.bg_ranking_item_highlight);
        } else {
            holder.tvYourRegion.setVisibility(View.GONE);
            holder.itemView.setBackgroundResource(R.drawable.bg_ranking_item);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvStars, tvYourRegion;
        ImageView ivIcon;
        View vIconBg;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            vIconBg = itemView.findViewById(R.id.vIconBg);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvStars = itemView.findViewById(R.id.tvStars);
            tvYourRegion = itemView.findViewById(R.id.tvYourRegion);
        }
    }
}
