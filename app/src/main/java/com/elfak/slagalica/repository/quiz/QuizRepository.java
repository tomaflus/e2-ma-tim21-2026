package com.elfak.slagalica.repository.quiz;

import com.elfak.slagalica.model.quiz.Difficulty;
import com.elfak.slagalica.model.quiz.Question;
import java.util.ArrayList;
import java.util.List;

public class QuizRepository {
    public List<Question> getMockQuestions() {
        List<Question> q = new ArrayList<>();
        
        // GEOGRAFIJA
        q.add(new Question("Koji je glavni grad Brazila?", new String[]{"Rio de Žaneiro", "Sao Paulo", "Brazilija", "Salvador"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Najviši vrh na svetu, Mont Everest, nalazi se na planinskom vencu:", new String[]{"Andi", "Alpi", "Himalaji", "Kordiljeri"}, 2, Difficulty.EASY));
        q.add(new Question("Koja reka protiče kroz Pariz?", new String[]{"Dunav", "Temza", "Sena", "Rajna"}, 2, Difficulty.EASY));
        q.add(new Question("Najmanja država na svetu po površini je:", new String[]{"Monako", "Nauru", "Vatikan", "San Marino"}, 2, Difficulty.EASY));
        q.add(new Question("Koji okean razdvaja Evropu i Ameriku?", new String[]{"Tihi okean", "Indijski okean", "Severni ledeni okean", "Atlantski okean"}, 3, Difficulty.EASY));
        q.add(new Question("Država koja se prostire na dva kontinenta je:", new String[]{"Egipat", "Turska", "Panama", "Sve navedene"}, 3, Difficulty.MEDIUM));
        q.add(new Question("Koje je najdublje jezero na svetu?", new String[]{"Kaspijsko", "Bajkalsko", "Mrtvo more", "Viktorijino"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Glavni grad Kanade je:", new String[]{"Toronto", "Vankuver", "Montreal", "Otava"}, 3, Difficulty.MEDIUM));
        
        // ISTORIJA
        q.add(new Question("Kada je pao Berlinski zid?", new String[]{"1985", "1989", "1991", "1995"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Ko je bio prvi predsednik SAD?", new String[]{"Tomas Džeferson", "Abraham Linkoln", "Džordž Vašington", "Džon Adams"}, 2, Difficulty.EASY));
        q.add(new Question("U kom gradu je ubijen Franc Ferdinand 1914. godine?", new String[]{"Beč", "Beograd", "Sarajevo", "Zagreb"}, 2, Difficulty.EASY));
        q.add(new Question("Dinastija koja je vladala Francuskom do revolucije 1789. bila je:", new String[]{"Habsburg", "Burbon", "Romanov", "Tjudor"}, 1, Difficulty.HARD));
        q.add(new Question("Koja civilizacija je izgradila Maču Pikču?", new String[]{"Asteci", "Maje", "Inke", "Feničani"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Boj na Kosovu odigrao se koje godine?", new String[]{"1354", "1389", "1453", "1219"}, 1, Difficulty.EASY));
        q.add(new Question("Ko je otkrio Ameriku 1492. godine?", new String[]{"Vasko da Gama", "Magelan", "Kristifor Kolumbo", "Amerigo Vespuči"}, 2, Difficulty.EASY));
        
        // SPORT
        q.add(new Question("Koliko igrača broji jedan tim u vaterpolu na terenu?", new String[]{"5", "6", "7", "11"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koji teniser ima najviše osvojenih Grend slem titula (do 2024)?", new String[]{"Rafael Nadal", "Rodžer Federer", "Novak Đoković", "Pit Sampras"}, 2, Difficulty.EASY));
        q.add(new Question("Gde su održane prve moderne Olimpijske igre 1896?", new String[]{"Rim", "Pariz", "Atina", "London"}, 2, Difficulty.EASY));
        q.add(new Question("Koji klub ima najviše titula Lige šampiona u fudbalu?", new String[]{"Milan", "Bajern Minhen", "Real Madrid", "Liverpul"}, 2, Difficulty.EASY));
        q.add(new Question("U kom sportu se dodeljuje 'Stanley Cup'?", new String[]{"Bejzbol", "Američki fudbal", "Hokej na ledu", "Košarka"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koliko minuta traje jedna četvrtina u NBA utakmici?", new String[]{"10", "12", "15", "8"}, 1, Difficulty.MEDIUM));
        
        // NAUKA / BIOLOGIJA / HEMIJA
        q.add(new Question("Koji gas čini najveći deo Zemljine atmosfere?", new String[]{"Kiseonik", "Azot", "Ugljen-dioksid", "Vodonik"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koju oznaku u periodnom sistemu ima gvožđe?", new String[]{"Fe", "F", "Ir", "Gv"}, 0, Difficulty.EASY));
        q.add(new Question("Planeta poznata kao 'Crvena planeta' je:", new String[]{"Venera", "Mars", "Jupiter", "Saturn"}, 1, Difficulty.EASY));
        q.add(new Question("Najveća žlezda u ljudskom telu je:", new String[]{"Slezina", "Pankreas", "Jetra", "Štitna žlezda"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koja je formula za kuhinjsku so?", new String[]{"H2O", "CO2", "NaCl", "NaOH"}, 2, Difficulty.EASY));
        q.add(new Question("Koliko hromozoma ima normalna ljudska ćelija?", new String[]{"23", "44", "46", "48"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Brzina svetlosti iznosi približno:", new String[]{"300.000 km/s", "150.000 km/s", "1.000.000 km/s", "30.000 km/s"}, 0, Difficulty.MEDIUM));
        q.add(new Question("Ko je postavio teoriju relativiteta?", new String[]{"Isak Njutn", "Nikola Tesla", "Albert Ajnštajn", "Marija Kiri"}, 2, Difficulty.EASY));
        
        // KNJIŽEVNOST
        q.add(new Question("Ko je autor romana 'Zločin i kazna'?", new String[]{"Lav Tolstoj", "Fjodor Dostojevski", "Anton Čehov", "Ivan Turgenjev"}, 1, Difficulty.EASY));
        q.add(new Question("Kako se zove glavni junak Servantesovog remek-dela?", new String[]{"Sančo Pansa", "Don Kihot", "D'Artanjan", "Guliver"}, 1, Difficulty.EASY));
        q.add(new Question("Koji pisac je dobio Nobelovu nagradu za književnost 1961. godine?", new String[]{"Miloš Crnjanski", "Danilo Kiš", "Ivo Andrić", "Meša Selimović"}, 2, Difficulty.EASY));
        q.add(new Question("Koji je prvi srpski roman?", new String[]{"Seobe", "Aristid i Natalija", "Koreni", "Derviš i smrt"}, 1, Difficulty.HARD));
        q.add(new Question("Ko je napisao 'Gorski vijenac'?", new String[]{"Vuk Karadžić", "Petar II Petrović Njegoš", "Branko Radičević", "Jovan Jovanović Zmaj"}, 1, Difficulty.EASY));
        
        // FILM / MUZIKA
        q.add(new Question("Koji film je osvojio prvog Oskara za najbolji film ikada?", new String[]{"Prohujalo sa vihorom", "Krila (Wings)", "Kazablanka", "Građanin Kejn"}, 1, Difficulty.HARD));
        q.add(new Question("Iz koje države potiče grupa ABBA?", new String[]{"Norveška", "Danska", "Švedska", "Finska"}, 2, Difficulty.EASY));
        q.add(new Question("Ko je režirao film 'Paklena pomorandža'?", new String[]{"Stiven Spilberg", "Alfred Hičkok", "Stenli Kjubrik", "Kventin Tarantino"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koji instrument je svirao Majls Dejvis?", new String[]{"Saksofon", "Klavir", "Truba", "Gitara"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Ko je 'Kralj popa'?", new String[]{"Elvis Prisli", "Majkl Džekson", "Princ", "Dejvid Bouvi"}, 1, Difficulty.EASY));
        
        // TEHNOLOGIJA
        q.add(new Question("Šta znači skraćenica WWW?", new String[]{"World Wide Web", "World West Web", "Web World Wide", "Wide World Web"}, 0, Difficulty.EASY));
        q.add(new Question("Koji programski jezik je kreirao Džejms Gosling?", new String[]{"Python", "C++", "Java", "JavaScript"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koje godine je lansiran prvi iPhone?", new String[]{"2005", "2007", "2008", "2010"}, 1, Difficulty.MEDIUM));
        q.add(new Question("OS koji koristi većinu pametnih telefona na svetu je:", new String[]{"iOS", "Windows", "Android", "Symbian"}, 2, Difficulty.EASY));
        q.add(new Question("Ko je osnivač kompanije Microsoft?", new String[]{"Stiv Džobs", "Bil Gejts", "Mark Cukerberg", "Elon Mask"}, 1, Difficulty.EASY));
        
        // OPŠTA KULTURA
        q.add(new Question("Koji je hemijski simbol za srebro?", new String[]{"Si", "Au", "Ag", "Sn"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koliko polja ima šahovska tabla?", new String[]{"32", "64", "81", "100"}, 1, Difficulty.EASY));
        q.add(new Question("U kom gradu se nalazi Tadž Mahal?", new String[]{"Delhi", "Mumbaj", "Agra", "Daka"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koji horoskopski znak je osoba rođena 15. avgusta?", new String[]{"Rak", "Lav", "Devica", "Blizanci"}, 1, Difficulty.EASY));
        q.add(new Question("Statua slobode je poklon koje države Americi?", new String[]{"Engleske", "Nemačke", "Francuske", "Italije"}, 2, Difficulty.EASY));
        q.add(new Question("Koliko nogu ima pauk?", new String[]{"6", "8", "10", "12"}, 1, Difficulty.EASY));
        q.add(new Question("Koji je najtvrđi prirodni mineral?", new String[]{"Kvarc", "Topaz", "Dijamant", "Rubin"}, 2, Difficulty.EASY));
        q.add(new Question("Koja zemlja je kolevka karatea?", new String[]{"Kina", "Japan", "Koreja", "Tajland"}, 1, Difficulty.EASY));
        q.add(new Question("Šta proučava entomologija?", new String[]{"Ptice", "Insekte", "Ribe", "Gmizavce"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koji je najveći organ ljudskog tela?", new String[]{"Srce", "Pluća", "Koža", "Mozak"}, 2, Difficulty.EASY));
        q.add(new Question("Koliko je 12 na kvadrat?", new String[]{"122", "144", "169", "244"}, 1, Difficulty.EASY));
        q.add(new Question("Koja je valuta u Japanu?", new String[]{"Juan", "Von", "Jen", "Dolar"}, 2, Difficulty.EASY));
        q.add(new Question("Koja planina se nalazi pored Napulja?", new String[]{"Etna", "Vezuv", "Mon Blan", "Olimp"}, 1, Difficulty.EASY));
        q.add(new Question("Ko je naslikao 'Mona Lizu'?", new String[]{"Mikaelanđelo", "Rafael", "Leonardo da Vinči", "Donatelo"}, 2, Difficulty.EASY));
        q.add(new Question("Koliko žica ima standardna violina?", new String[]{"3", "4", "5", "6"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koji je glavni grad Portugala?", new String[]{"Madrid", "Porto", "Lisabon", "Sevilja"}, 2, Difficulty.EASY));
        q.add(new Question("Šta je po zanimanju bio Nikola Tesla?", new String[]{"Hemičar", "Biolog", "Inženjer i fizičar", "Matematičar"}, 2, Difficulty.EASY));
        
        // DODATNA PITANJA (NASTAVAK)
        q.add(new Question("Koji hemijski element ima simbol O?", new String[]{"Zlato", "Kiseonik", "Olovo", "Srebro"}, 1, Difficulty.EASY));
        q.add(new Question("Gde se nalazi dvorac Versaj?", new String[]{"Nemačka", "Austrija", "Francuska", "Engleska"}, 2, Difficulty.EASY));
        q.add(new Question("Koliko je 100 podeljeno sa 4?", new String[]{"20", "25", "30", "40"}, 1, Difficulty.EASY));
        q.add(new Question("Koja je najveća država na svetu po površini?", new String[]{"Kina", "SAD", "Rusija", "Kanada"}, 2, Difficulty.EASY));
        q.add(new Question("Koji je glavni grad Italije?", new String[]{"Milano", "Rim", "Napulj", "Venecija"}, 1, Difficulty.EASY));
        q.add(new Question("Kako se zove proces kojim biljke stvaraju hranu?", new String[]{"Oksidacija", "Fotosinteza", "Respiracija", "Fermentacija"}, 1, Difficulty.EASY));
        q.add(new Question("Koja je najmanja ptica na svetu?", new String[]{"Kolibri", "Vrabac", "Lastavica", "Papagaj"}, 0, Difficulty.MEDIUM));
        q.add(new Question("U kom veku je živeo Leonardo da Vinči?", new String[]{"13. i 14.", "15. i 16.", "17. i 18.", "19. i 20."}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koji je najnaseljeniji kontinent?", new String[]{"Afrika", "Evropa", "Azija", "Severna Amerika"}, 2, Difficulty.EASY));
        q.add(new Question("Koji se gas nalazi u balonima koji lete?", new String[]{"Kiseonik", "Helijum", "Azot", "Argon"}, 1, Difficulty.EASY));
        q.add(new Question("Koja je najduža reka na svetu?", new String[]{"Amazon", "Nil", "Dunav", "Jangcekjang"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koji je glavni grad Grčke?", new String[]{"Solun", "Atina", "Sparta", "Heraklion"}, 1, Difficulty.EASY));
        q.add(new Question("Koja planeta je najbliža Suncu?", new String[]{"Venera", "Zemlja", "Merkur", "Mars"}, 2, Difficulty.EASY));
        q.add(new Question("Koliko minuta ima jedan sat?", new String[]{"50", "60", "80", "100"}, 1, Difficulty.EASY));
        q.add(new Question("U kojoj državi se nalazi piramida Čičen Ica?", new String[]{"Egipat", "Meksiko", "Peru", "Grčka"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koji je glavni grad Mađarske?", new String[]{"Beč", "Prag", "Budimpešta", "Bratislava"}, 2, Difficulty.EASY));
        q.add(new Question("Koja je najveća pustinja na svetu?", new String[]{"Gobi", "Sahara", "Antarktička pustinja", "Kalahari"}, 2, Difficulty.HARD));
        q.add(new Question("Koji je najviši vodopad na svetu?", new String[]{"Nijagarini vodopadi", "Viktorijini vodopadi", "Anđeoski vodopad", "Igvazu"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Ko je otkrio penicilin?", new String[]{"Luj Paster", "Aleksandar Fleming", "Robert Koh", "Nikola Tesla"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koja država ima najviše stanovnika?", new String[]{"Indija", "Kina", "SAD", "Indonezija"}, 0, Difficulty.MEDIUM));
        q.add(new Question("Koji je glavni grad Crne Gore?", new String[]{"Cetinje", "Podgorica", "Budva", "Nikšić"}, 1, Difficulty.EASY));
        q.add(new Question("Koji je simbol za zlato u hemiji?", new String[]{"Ag", "Gd", "Au", "Fe"}, 2, Difficulty.EASY));
        q.add(new Question("Koji je najveći sisar na svetu?", new String[]{"Slon", "Plavi kit", "Žirafa", "Nosorog"}, 1, Difficulty.EASY));
        q.add(new Question("Gde se nalaze Alpi?", new String[]{"Azija", "Evropa", "Afrika", "Južna Amerika"}, 1, Difficulty.EASY));
        q.add(new Question("Ko je napisao 'Hari Potera'?", new String[]{"Dž.R.R. Tolkin", "Dž.K. Rouling", "Stiven King", "Agata Kristi"}, 1, Difficulty.EASY));
        q.add(new Question("Koje je boje smaragd?", new String[]{"Crvene", "Plave", "Zelene", "Žute"}, 2, Difficulty.EASY));
        q.add(new Question("Koji je glavni grad Hrvatske?", new String[]{"Split", "Rijeka", "Zagreb", "Dubrovnik"}, 2, Difficulty.EASY));
        q.add(new Question("Šta je atom?", new String[]{"Molekul", "Najmanja čestica elementa", "Ćelija", "Energija"}, 1, Difficulty.EASY));
        q.add(new Question("Koji je glavni grad Rusije?", new String[]{"Sankt Peterburg", "Moskva", "Kijev", "Minsk"}, 1, Difficulty.EASY));
        q.add(new Question("Koliko dana ima prestupna godina?", new String[]{"364", "365", "366", "367"}, 2, Difficulty.EASY));
        q.add(new Question("Koji je najveći unutrašnji organ čoveka?", new String[]{"Srce", "Pluća", "Jetra", "Bubrezi"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koja je valuta u Velikoj Britaniji?", new String[]{"Evro", "Dolar", "Funta", "Franak"}, 2, Difficulty.EASY));
        q.add(new Question("Ko je bio prvi čovek u svemiru?", new String[]{"Nil Armstrong", "Jurij Gagarin", "Baz Oldrin", "Džon Glen"}, 1, Difficulty.EASY));
        q.add(new Question("U kojoj državi se nalaze piramide u Gizi?", new String[]{"Meksiko", "Egipat", "Sudan", "Irak"}, 1, Difficulty.EASY));
        q.add(new Question("Koji je glavni grad Austrije?", new String[]{"Minhen", "Cirih", "Beč", "Berlin"}, 2, Difficulty.EASY));
        q.add(new Question("Koliko okeana postoji na Zemlji?", new String[]{"3", "4", "5", "6"}, 2, Difficulty.MEDIUM));
        q.add(new Question("Koji je glavni grad Turske?", new String[]{"Istanbul", "Ankara", "Izmir", "Antalija"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Ko je naslikao 'Svod Sikstinske kapele'?", new String[]{"Leonardo da Vinči", "Mikaelanđelo", "Rafael", "Donatelo"}, 1, Difficulty.MEDIUM));
        q.add(new Question("Koji je hemijski simbol za gvožđe?", new String[]{"Fe", "Ir", "Fv", "Go"}, 0, Difficulty.EASY));
        q.add(new Question("Gde su održane Olimpijske igre 2024?", new String[]{"Tokio", "London", "Pariz", "Los Anđeles"}, 2, Difficulty.EASY));
        q.add(new Question("Koji je glavni grad BiH?", new String[]{"Banja Luka", "Sarajevo", "Mostar", "Tuzla"}, 1, Difficulty.EASY));

        return q;
    }
}