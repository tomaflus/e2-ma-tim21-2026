# e2-ma-tim21-2026

# Slagalica - Mobilna aplikacija

Mobilna aplikacija po ugledu na kviz Slagalica, razvijena u okviru predmeta Mobilne aplikacije (2025/26).

## Tim

| Student | KT1 i KT2 | KO |
|---------|-----------|-----|
| Student 1 | Registracija i logovanje + Igre: Korak po korak i Moj broj | Igranje partija, Čet, Izazov |
| Student 2 | Prikaz profila + Igre: Ko zna zna i Spojnice | Prikaz regiona, Napredovanje kroz lige, Prijatelji |
| Student 3 | Notifikacije + Igre: Asocijacije i Skočko | Rang lista, Turnir, Dnevne misije |

## Tehnologije

- Java
- Android Studio
- Firebase (Authentication, Firestore, Storage)
- Google Maps API / OpenStreetMap
- Android Navigation Component
- ZXing QR Scanner

## Firebase

Naziv Firebase projekta: **slagalica-test** (Student 2 testna baza)

Da biste pokrenuli aplikaciju sa Student 2 funkcionalnostima, obavezno koristite najnoviji `google-services.json` koji je konfigurisan za rad sa Base64 slikama i slobodnim pravilima pristupa.

## KT1 & KT2 Student 2 - Završene funkcionalnosti

Student 2 je uspešno implementirao sve predviđene module za drugu kontrolnu tačku (KT2) i dodatne kriterijume ocene (KO).

### 1. Profil Korisnika (KT2)
- **Prikaz podataka**: Korisničko ime, email, tokeni, zvezde, liga i region.
- **Avatar Sistem**: Implementiran upload na Firebase. Slika se čuva kao Base64 string u Firestore bazi (Plan B) radi zaobilaženja restrikcija plaćanja Storage-a.
- **Dynamic Frame**: Boja okvira avatara (Zlatna, Srebrna, Bronzana) se automatski menja na osnovu regionalnog ranga korisnika.
- **QR Kod**: Generisanje jedinstvenog QR koda za svakog korisnika.

### 2. Igre i Multiplayer Logika (KT2)
- **Ko zna zna**: 
  - 5 pitanja, 5 sekundi po pitanju.
  - Bodovanje: +10 tačno, -5 netačno.
  - Implementirana logika "Brži igrač" (poređenje milisekundi klika).
- **Spojnice**:
  - 2 runde sa mešanjem pojmova.
  - Logika "Druga šansa": Ako prvi igrač pogreši ili mu istekne vreme, drugi igrač preuzima preostale pojmove.
- **Asocijacije**:
  - Potpuno funkcionalne 2 runde.
  - Sistem bodovanja po specifikaciji (max 30 bodova po rundi).
- **Multiplayer Ready**: Svi fragmenti podržavaju `partijaId` za real-time sinhronizaciju.

### 3. Regioni i Mapa (KO)
- **Google Maps Integracija**: Prikaz stvarne mape Srbije sa markerima igrača.
- **Real-time Statistika**: Broj aktivnih igrača po regionu se računa agregacijom `online` statusa iz Firebase-a.
- **Regionalni Rang**: Automatsko sabiranje zvezda svih igrača regiona za mesečnu rang listu.

### 4. Lige i Napredovanje (KO)
- **Sistem 0-5 liga**: Od Početnika do Dijamantske lige.
- **Automatizacija**: Automatska promocija i ispadanje na osnovu broja zvezda (100, 200, 400, 800, 1600).
- **Bonusi**: Svaka liga donosi dodatne dnevne tokene (Liga 5 = +5 tokena dnevno).
- **Mesečni Penal**: Implementirana logika za smanjenje 30% zvezda neaktivnim igračima.

### 5. Prijatelji (KO)
- **Obostrano Prijateljstvo**: Korišćenje `WriteBatch` operacija osigurava da dodavanje prijatelja bude momentalno vidljivo kod oba korisnika.
- **QR Skener**: Integrisan ZXing skener za brzo dodavanje prijatelja skeniranjem telefona.
- **Real-time Pozivi**: Sistem poziva na partiju koji iskače kao dijalog u bilo kom delu aplikacije (MainActivity listener).
- **Timeout**: Automatsko odbijanje poziva nakon 10 sekundi.

## Arhitektura Projekta

Projekat prati striktnu **troslojnu arhitekturu**:
1. **UI Layer**: Fragmenti (npr. `KoZnaZnaFragment`) koriste ViewBinding i komuniciraju isključivo sa servisima.
2. **Business Layer**: Servisi (npr. `QuizService`, `LeagueService`) sadrže svu logiku bodovanja i pravila igara.
3. **Data Layer**: Repozitorijumi (`UserRepository`, `FriendRepository`) vrše asinhronu komunikaciju sa Firebase-om.

## Pokretanje i Testiranje

1. Uradite **Clean Project** i **Rebuild Project**.
2. Koristite stvarni uređaj ili emulator sa Play Prodavnicom za rad Google Mapa.
3. Za testiranje multiplayer poziva, pokrenite aplikaciju na dva uređaja istovremeno.

---
*Ažurirano: Jun 2026. | Student 2: [Tvoje Ime]*
