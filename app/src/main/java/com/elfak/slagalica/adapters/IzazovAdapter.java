package com.elfak.slagalica.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elfak.slagalica.R;
import com.elfak.slagalica.model.Izazov;

import java.util.List;

public class IzazovAdapter extends RecyclerView.Adapter<IzazovAdapter.IzazovViewHolder> {

    private List<Izazov> izazovi;
    private String mojId;
    private OnPrihvatiListener listener;

    public interface OnPrihvatiListener {
        void onPrihvati(Izazov izazov);
    }

    public IzazovAdapter(List<Izazov> izazovi, String mojId,
                         OnPrihvatiListener listener) {
        this.izazovi = izazovi;
        this.mojId = mojId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IzazovViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_izazov, parent, false);
        return new IzazovViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IzazovViewHolder holder, int position) {
        Izazov izazov = izazovi.get(position);

        holder.tvKreator.setText("Kreator: " + izazov.getKreatorIme());
        holder.tvUlog.setText("Ulog: " + izazov.getZvezdeUlog() +
                " zvezda, " + izazov.getTokeniUlog() + " tokena");
        holder.tvIgraci.setText("Igraci: " + izazov.getIgraciIds().size() + "/4");

        // Sakrij dugme ako je kreator ili vec u izazovu
        if (izazov.getKreatorId().equals(mojId) ||
                izazov.getIgraciIds().contains(mojId)) {
            holder.btnPrihvati.setText("Vec ste u izazovu");
            holder.btnPrihvati.setEnabled(false);
        } else {
            holder.btnPrihvati.setText("Prihvati izazov");
            holder.btnPrihvati.setEnabled(true);
            holder.btnPrihvati.setOnClickListener(v -> listener.onPrihvati(izazov));
        }
    }

    @Override
    public int getItemCount() {
        return izazovi.size();
    }

    public void azurirajIzazove(List<Izazov> noviIzazovi) {
        this.izazovi = noviIzazovi;
        notifyDataSetChanged();
    }

    static class IzazovViewHolder extends RecyclerView.ViewHolder {
        TextView tvKreator, tvUlog, tvIgraci;
        Button btnPrihvati;

        IzazovViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKreator = itemView.findViewById(R.id.tvKreatorIzazov);
            tvUlog = itemView.findViewById(R.id.tvUlogIzazov);
            tvIgraci = itemView.findViewById(R.id.tvIgraciIzazov);
            btnPrihvati = itemView.findViewById(R.id.btnPrihvatiIzazov);
        }
    }
}