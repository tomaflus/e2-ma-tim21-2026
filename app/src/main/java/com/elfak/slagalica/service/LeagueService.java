package com.elfak.slagalica.service;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.elfak.slagalica.model.League;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.repository.UserRepository;

public class LeagueService {
    private final UserRepository repo;
    private final Context context;

    public LeagueService(Context context) {
        this.context = context;
        this.repo = new UserRepository();
    }

    public void checkLeagueAdvancement(User user) {
        League current = League.getByStars(user.getZvezde());
        if (current.ordinal() != user.getLiga()) {
            boolean up = current.ordinal() > user.getLiga();
            repo.azurirajZvezdeILigu(user.getZvezde(), () -> {
                showLeagueDialog(current, up);
            });
        }
    }

    private void showLeagueDialog(League league, boolean up) {
        String title = up ? "Promocija u ligu!" : "Ispadanje iz lige";
        String msg = up ? "Čestitamo! Napredovali ste u ligu: " : "Nazadovali ste u ligu: ";
        
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg + league.getName())
                .setPositiveButton("U redu", null)
                .setIcon(league.getIconRes())
                .show();
    }

    public void applyMonthlyPenalty(User user) {
        int noveZvezde = (int) (user.getZvezde() * 0.7);
        repo.azurirajZvezdeILigu(noveZvezde, () -> {
            Toast.makeText(context, "Mesečni ciklus završen: -30% zvezda zbog neaktivnosti", Toast.LENGTH_LONG).show();
        });
    }
}