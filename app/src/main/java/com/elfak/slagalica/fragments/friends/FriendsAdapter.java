package com.elfak.slagalica.fragments.friends;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.ItemFriendBinding;
import com.elfak.slagalica.model.User;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
    private List<User> users;
    private final OnInviteListener listener;

    public interface OnInviteListener { void onInvite(User user); }

    public FriendsAdapter(List<User> users, OnInviteListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateData(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendBinding binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.binding.tvUsername.setText(user.getKorisnickoIme());
        
        com.elfak.slagalica.model.League league = com.elfak.slagalica.model.League.getByStars(user.getZvezde());
        holder.binding.tvLeagueName.setText(league.getName());
        holder.binding.ivLeagueIcon.setImageResource(league.getIconRes());
        
        holder.binding.tvStars.setText(String.valueOf(user.getZvezde()));
        String rankText = (user.getMesecniBodovi() > 0) ? "#" + user.getMesecniBodovi() : "Nije rangiran";
        holder.binding.tvRank.setText(rankText + " u mesečnom rangu");
        
        // Status indicator & Button state
        if (user.isuPartiji()) {
            holder.binding.vStatusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
            holder.binding.btnInvite.setText("U partiji");
            holder.binding.btnInvite.setEnabled(false);
            holder.binding.btnInvite.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E2E4E")));
        } else if (user.isOnline()) {
            holder.binding.vStatusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
            holder.binding.btnInvite.setText("Igraj");
            holder.binding.btnInvite.setEnabled(true);
            holder.binding.btnInvite.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#673AB7")));
        } else {
            holder.binding.vStatusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#808080")));
            holder.binding.btnInvite.setText("Offline");
            holder.binding.btnInvite.setEnabled(false);
            holder.binding.btnInvite.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E2E4E")));
        }

        // TODO: Handle "In Match" status if we have that information
        // if (user.isInMatch()) { ... }

        holder.binding.btnInvite.setOnClickListener(v -> listener.onInvite(user));
        
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(user.getAvatarUrl())
                .circleCrop()
                .into(holder.binding.ivAvatar);
        } else {
            holder.binding.ivAvatar.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public int getItemCount() { return users.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemFriendBinding binding;
        public ViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}