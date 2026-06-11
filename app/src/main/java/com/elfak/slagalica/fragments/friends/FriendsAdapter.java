package com.elfak.slagalica.fragments.friends;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.binding.tvLeague.setText("Liga: " + user.getLiga());
        holder.binding.btnInvite.setOnClickListener(v -> listener.onInvite(user));
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