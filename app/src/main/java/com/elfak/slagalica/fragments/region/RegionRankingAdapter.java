package com.elfak.slagalica.fragments.region;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<Region, Integer> entry = data.get(position);
        holder.tvName.setText((position + 1) + ". " + entry.getKey().getFullName());
        holder.tvStars.setText(entry.getValue() + " zvezda");

        if (entry.getKey().getFullName().equalsIgnoreCase(userRegion)) {
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.region_highlight));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStars;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
            tvStars = itemView.findViewById(android.R.id.text2);
            tvName.setTextColor(Color.WHITE);
            tvStars.setTextColor(Color.LTGRAY);
        }
    }
}