package com.elfak.slagalica.service;

import com.elfak.slagalica.repository.AsocijacijeRepository;
import com.elfak.slagalica.service.stats.StatsService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AsocijacijeService {
    private final List<AsocijacijeRepository.Asocijacija> asocijacije;
    private final StatsService stats;
    private int round = 1;
    private int score = 0;
    private boolean isIgrac1NaPotezu = true;
    private AsocijacijeRepository.Asocijacija current;

    private Set<String> otvorenaPolja = new HashSet<>();
    private boolean resenoA = false, resenoB = false, resenoC = false, resenoD = false;
    private boolean resenoKonacno = false;

    public AsocijacijeService(AsocijacijeRepository repo, StatsService stats) {
        this.stats = stats;
        List<AsocijacijeRepository.Asocijacija> all = repo.getMockAsocijacije();
        Collections.shuffle(all);
        this.asocijacije = all;
        loadRound(1);
    }

    public void loadRound(int r) {
        this.round = r;
        this.current = asocijacije.get(r - 1);
        this.otvorenaPolja.clear();
        this.resenoA = resenoB = resenoC = resenoD = resenoKonacno = false;
        this.isIgrac1NaPotezu = (r == 1);
    }

    public void otvoriPolje(String id) {
        otvorenaPolja.add(id);
    }

    public boolean pogodiKolonu(char kolona, String pokusaj) {
        String resenje = "";
        switch (kolona) {
            case 'A': resenje = current.resA; break;
            case 'B': resenje = current.resB; break;
            case 'C': resenje = current.resC; break;
            case 'D': resenje = current.resD; break;
        }
        if (resenje.equalsIgnoreCase(pokusaj)) {
            int neotvorena = 0;
            for(int i=1; i<=4; i++) if(!otvorenaPolja.contains(kolona + String.valueOf(i))) neotvorena++;
            int points = 2 + neotvorena;
            if(isIgrac1NaPotezu) score += points;
            
            switch (kolona) {
                case 'A': resenoA = true; break;
                case 'B': resenoB = true; break;
                case 'C': resenoC = true; break;
                case 'D': resenoD = true; break;
            }
            // Automatski otvori sva polja te kolone
            for(int i=1; i<=4; i++) otvorenaPolja.add(kolona + String.valueOf(i));
            return true;
        }
        isIgrac1NaPotezu = !isIgrac1NaPotezu;
        return false;
    }

    public boolean pogodiKonacno(String pokusaj) {
        if (current.konacno.equalsIgnoreCase(pokusaj)) {
            int neotvoreneKolone = 0;
            if(!resenoA) neotvoreneKolone++;
            if(!resenoB) neotvoreneKolone++;
            if(!resenoC) neotvoreneKolone++;
            if(!resenoD) neotvoreneKolone++;
            
            int points = 7 + (neotvoreneKolone * 6);
            // Dodajemo i poene za neotvorena polja u neotvorenim kolonama
            // Specifikacija g: "7 bodova + 6 bodova za svaku neotvorenu kolonu + dobijeni bodovi na prethodno opisan način"
            // To znaci svaka neotvorena kolona nosi 6 + 2 (resenje) + 4 (polja) = 12? 
            // Re-read: "7 bodova + 6 bodova za svaku neotvorenu kolonu + dobijeni bodovi za otvorene/delimično otvorene"
            // Primer h: 7 (konacno) + 5 (kolona A sa 1 poljem) + 3*6 (ostale) = 30.
            // Znaci: Konacno (7) + (neotvorene * 6) + (svaka neotvorena/delimična kolona po njenom sistemu)
            
            if(!resenoA) score += (2 + getNeotvorenaUKoloni('A'));
            if(!resenoB) score += (2 + getNeotvorenaUKoloni('B'));
            if(!resenoC) score += (2 + getNeotvorenaUKoloni('C'));
            if(!resenoD) score += (2 + getNeotvorenaUKoloni('D'));
            
            if(isIgrac1NaPotezu) score += points;
            resenoKonacno = resenoA = resenoB = resenoC = resenoD = true;
            return true;
        }
        isIgrac1NaPotezu = !isIgrac1NaPotezu;
        return false;
    }
    
    private int getNeotvorenaUKoloni(char k) {
        int n = 0;
        for(int i=1; i<=4; i++) if(!otvorenaPolja.contains(k + String.valueOf(i))) n++;
        return n;
    }

    public void zameniIgraca() { isIgrac1NaPotezu = !isIgrac1NaPotezu; }
    public boolean isResenoKonacno() { return resenoKonacno; }
    public AsocijacijeRepository.Asocijacija getCurrent() { return current; }
    public Set<String> getOtvorenaPolja() { return otvorenaPolja; }
    public int getScore() { return score; }
    public int getRound() { return round; }
    public boolean isIgrac1NaPotezu() { return isIgrac1NaPotezu; }
    public boolean isReseno(char k) {
        switch(k) { case 'A': return resenoA; case 'B': return resenoB; case 'C': return resenoC; case 'D': return resenoD; default: return false; }
    }
}