package com.elfak.slagalica.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.Turnir;

import java.util.ArrayList;
import java.util.List;

public class TurnirAdapter extends RecyclerView.Adapter<TurnirAdapter.VH> {

    public interface OnKlikListener { void onClick(Turnir t); }

    private List<Turnir> lista = new ArrayList<>();
    private final OnKlikListener listener;

    public TurnirAdapter(OnKlikListener listener) {
        this.listener = listener;
    }

    public void setLista(List<Turnir> nova) {
        lista = nova != null ? nova : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_turnir, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Turnir t = lista.get(pos);
        h.tvNaziv.setText(t.getNaziv());
        int brIgraca = t.getIgraci().size();
        h.tvBrojIgraca.setText(brIgraca + "/4 igrača");
        h.itemView.setOnClickListener(v -> listener.onClick(t));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNaziv, tvBrojIgraca;
        VH(@NonNull View v) {
            super(v);
            tvNaziv     = v.findViewById(R.id.tvTurnirNaziv);
            tvBrojIgraca = v.findViewById(R.id.tvTurnirBrojIgraca);
        }
    }
}
