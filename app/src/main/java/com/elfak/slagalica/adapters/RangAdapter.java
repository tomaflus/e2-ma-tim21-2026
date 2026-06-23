package com.elfak.slagalica.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.League;
import com.elfak.slagalica.model.User;

import java.util.ArrayList;
import java.util.List;

public class RangAdapter extends RecyclerView.Adapter<RangAdapter.VH> {

    public enum Tip { NEDELJNA, MESECNA }

    private List<User> lista = new ArrayList<>();
    private Tip tip = Tip.NEDELJNA;

    public void updateData(List<User> nova, Tip noviTip) {
        lista = nova;
        tip = noviTip;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rang, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        User u = lista.get(pos);
        h.tvPozicija.setText((pos + 1) + ".");
        h.tvIme.setText(u.getKorisnickoIme());
        int zvezdeCiklus = tip == Tip.NEDELJNA ? u.getNedeljneZvezde() : u.getMesecneZvezde();
        h.tvZvezde.setText(zvezdeCiklus + " ★");
        League liga = League.getByStars(u.getZvezde());
        h.ivLiga.setImageResource(liga.getIconRes());
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPozicija, tvIme, tvZvezde;
        ImageView ivLiga;

        VH(@NonNull View v) {
            super(v);
            tvPozicija = v.findViewById(R.id.tvPozicija);
            ivLiga     = v.findViewById(R.id.ivLiga);
            tvIme      = v.findViewById(R.id.tvIme);
            tvZvezde   = v.findViewById(R.id.tvZvezde);
        }
    }
}
