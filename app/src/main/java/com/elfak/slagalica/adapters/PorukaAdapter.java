package com.elfak.slagalica.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.Poruka;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PorukaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIP_MOJA = 1;
    private static final int TIP_TUDJA = 2;

    private List<Poruka> poruke;
    private String mojId;

    public PorukaAdapter(List<Poruka> poruke, String mojId) {
        this.poruke = poruke;
        this.mojId = mojId;
    }

    @Override
    public int getItemViewType(int position) {
        if (poruke.get(position).getPosiljacId().equals(mojId)) {
            return TIP_MOJA;
        }
        return TIP_TUDJA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TIP_MOJA) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_poruka_moja, parent, false);
            return new MojaPorukaViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_poruka_tudja, parent, false);
            return new TudjaPorukaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Poruka poruka = poruke.get(position);
        String vrijemeFormatted = new SimpleDateFormat("dd.MM. HH:mm",
                Locale.getDefault()).format(new Date(poruka.getVrijemeSlanja()));

        if (holder instanceof MojaPorukaViewHolder) {
            MojaPorukaViewHolder h = (MojaPorukaViewHolder) holder;
            h.tvTekst.setText(poruka.getTekst());
            h.tvVrijeme.setText(vrijemeFormatted);
        } else {
            TudjaPorukaViewHolder h = (TudjaPorukaViewHolder) holder;
            h.tvIme.setText(poruka.getPosiljacIme());
            h.tvTekst.setText(poruka.getTekst());
            h.tvVrijeme.setText(vrijemeFormatted);
        }
    }

    @Override
    public int getItemCount() {
        return poruke.size();
    }

    public void azurirajPoruke(List<Poruka> novePoruke) {
        this.poruke = novePoruke;
        notifyDataSetChanged();
    }

    // ViewHolder za moje poruke
    static class MojaPorukaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTekst, tvVrijeme;

        MojaPorukaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTekst = itemView.findViewById(R.id.tvTekst);
            tvVrijeme = itemView.findViewById(R.id.tvVrijeme);
        }
    }

    // ViewHolder za tuđe poruke
    static class TudjaPorukaViewHolder extends RecyclerView.ViewHolder {
        TextView tvIme, tvTekst, tvVrijeme;

        TudjaPorukaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIme = itemView.findViewById(R.id.tvIme);
            tvTekst = itemView.findViewById(R.id.tvTekst);
            tvVrijeme = itemView.findViewById(R.id.tvVrijeme);
        }
    }
}