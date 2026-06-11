package com.elfak.slagalica.repository.matching;

import com.elfak.slagalica.model.matching.MatchingPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MatchingRepository {
    private final List<List<MatchingPair>> allSets = new ArrayList<>();

    public MatchingRepository() {
        initSets();
    }

    private void initSets() {
        // SET 1: Država - Glavni grad
        List<MatchingPair> set1 = new ArrayList<>();
        set1.add(new MatchingPair("Srbija", "Beograd"));
        set1.add(new MatchingPair("Francuska", "Pariz"));
        set1.add(new MatchingPair("Nemačka", "Berlin"));
        set1.add(new MatchingPair("Portugal", "Lisabon"));
        set1.add(new MatchingPair("Japan", "Tokio"));
        allSets.add(set1);

        // SET 2: Pisac - Delo
        List<MatchingPair> set2 = new ArrayList<>();
        set2.add(new MatchingPair("Ivo Andrić", "Na Drini ćuprija"));
        set2.add(new MatchingPair("Meša Selimović", "Derviš i smrt"));
        set2.add(new MatchingPair("Dostojevski", "Zločin i kazna"));
        set2.add(new MatchingPair("Dante Aligijeri", "Božanstvena komedija"));
        set2.add(new MatchingPair("Viktor Igo", "Jadnici"));
        allSets.add(set2);

        // SET 3: Hemijski simbol - Element
        List<MatchingPair> set3 = new ArrayList<>();
        set3.add(new MatchingPair("Au", "Zlato"));
        set3.add(new MatchingPair("Ag", "Srebro"));
        set3.add(new MatchingPair("Fe", "Gvožđe"));
        set3.add(new MatchingPair("Cu", "Bakar"));
        set3.add(new MatchingPair("Pb", "Olovo"));
        allSets.add(set3);

        // SET 4: Planeta - Osobina
        List<MatchingPair> set4 = new ArrayList<>();
        set4.add(new MatchingPair("Mars", "Crvena planeta"));
        set4.add(new MatchingPair("Jupiter", "Najveća planeta"));
        set4.add(new MatchingPair("Saturn", "Planeta sa prstenovima"));
        set4.add(new MatchingPair("Venera", "Najtoplija planeta"));
        set4.add(new MatchingPair("Merkur", "Najbliža Suncu"));
        allSets.add(set4);

        // SET 5: Naučnik - Otkriće/Izum
        List<MatchingPair> set5 = new ArrayList<>();
        set5.add(new MatchingPair("Nikola Tesla", "Naizmenična struja"));
        set5.add(new MatchingPair("Albert Ajnštajn", "Teorija relativiteta"));
        set5.add(new MatchingPair("Isak Njutn", "Zakon gravitacije"));
        set5.add(new MatchingPair("Marija Kiri", "Radijum i polonijum"));
        set5.add(new MatchingPair("Aleksandar Fleming", "Penicilin"));
        allSets.add(set5);

        // SET 6: Reka - Država (u kojoj se nalazi ušće ili veći deo)
        List<MatchingPair> set6 = new ArrayList<>();
        set6.add(new MatchingPair("Nil", "Egipat"));
        set6.add(new MatchingPair("Amazon", "Brazil"));
        set6.add(new MatchingPair("Misisipi", "SAD"));
        set6.add(new MatchingPair("Dunav", "Rumunija"));
        set6.add(new MatchingPair("Volga", "Rusija"));
        allSets.add(set6);

        // SET 7: Sport - Broj igrača u timu na terenu
        List<MatchingPair> set7 = new ArrayList<>();
        set7.add(new MatchingPair("Fudbal", "11 igrača"));
        set7.add(new MatchingPair("Košarka", "5 igrača"));
        set7.add(new MatchingPair("Odbojka", "6 igrača"));
        set7.add(new MatchingPair("Vaterpolo", "7 igrača"));
        set7.add(new MatchingPair("Rukomet", "7 igrača"));
        allSets.add(set7);

        // SET 8: Životinja - Grupa
        List<MatchingPair> set8 = new ArrayList<>();
        set8.add(new MatchingPair("Soko", "Ptica"));
        set8.add(new MatchingPair("Šaran", "Riba"));
        set8.add(new MatchingPair("Kobra", "Gmizavac"));
        set8.add(new MatchingPair("Lav", "Sisari"));
        set8.add(new MatchingPair("Žaba", "Vodozemci"));
        allSets.add(set8);

        // SET 9: Istorijska ličnost - Uloga
        List<MatchingPair> set9 = new ArrayList<>();
        set9.add(new MatchingPair("Napoleon", "Francuski car"));
        set9.add(new MatchingPair("Cezar", "Rimski diktator"));
        set9.add(new MatchingPair("Linkoln", "Američki predsednik"));
        set9.add(new MatchingPair("Karađorđe", "Vođa Prvog ustanka"));
        set9.add(new MatchingPair("Atila", "Vođa Huna"));
        allSets.add(set9);

        // SET 10: Glumac - Poznati film
        List<MatchingPair> set10 = new ArrayList<>();
        set10.add(new MatchingPair("Marlon Brando", "Kum"));
        set10.add(new MatchingPair("Tom Henks", "Forest Gamp"));
        set10.add(new MatchingPair("Leonardo Dikaprio", "Titanik"));
        set10.add(new MatchingPair("Robert De Niro", "Taksista"));
        set10.add(new MatchingPair("Bred Pit", "Borilački klub"));
        allSets.add(set10);
        
        // SET 11: Jedinice mere - Fizička veličina
        List<MatchingPair> set11 = new ArrayList<>();
        set11.add(new MatchingPair("Vat", "Snaga"));
        set11.add(new MatchingPair("Džul", "Energija"));
        set11.add(new MatchingPair("Paskal", "Pritisak"));
        set11.add(new MatchingPair("Ohm", "Električni otpor"));
        set11.add(new MatchingPair("Tesla", "Magnetna indukcija"));
        allSets.add(set11);
    }

    public List<MatchingPair> getRandomSet() {
        if (allSets.isEmpty()) return new ArrayList<>();
        return allSets.get(new Random().nextInt(allSets.size()));
    }

    public List<List<MatchingPair>> getRandomGameSets(int count) {
        List<List<MatchingPair>> copy = new ArrayList<>(allSets);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }
}