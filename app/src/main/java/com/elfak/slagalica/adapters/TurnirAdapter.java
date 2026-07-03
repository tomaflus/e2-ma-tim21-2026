package com.elfak.slagalica.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

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
    private final boolean statusBorder;

    public TurnirAdapter(OnKlikListener listener, boolean statusBorder) {
        this.listener = listener;
        this.statusBorder = statusBorder;
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

        String status = t.getStatus();
        int color;
        if ("GOTOV".equals(status)) {
            h.tvBrojIgraca.setText("✅ Završen");
            color = 0xFF4CAF50;
        } else if ("U_TOKU".equals(status)) {
            h.tvBrojIgraca.setText("⚔️ U toku");
            color = 0xFFFF9800;
        } else {
            h.tvBrojIgraca.setText("🕐 " + t.getIgraci().size() + "/4");
            color = 0xFFFFD700;
        }
        h.tvBrojIgraca.setTextColor(color);
        if (statusBorder) {
            h.card.setStrokeColor(ColorStateList.valueOf(color));
            float dp = h.card.getContext().getResources().getDisplayMetrics().density;
            h.card.setStrokeWidth((int) (2 * dp));
        }

        h.itemView.setOnClickListener(v -> listener.onClick(t));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvNaziv, tvBrojIgraca;
        VH(@NonNull View v) {
            super(v);
            card         = (MaterialCardView) v;
            tvNaziv      = v.findViewById(R.id.tvTurnirNaziv);
            tvBrojIgraca = v.findViewById(R.id.tvTurnirBrojIgraca);
        }
    }
}
