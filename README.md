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
- Firebase Authentication
- Firebase Firestore
- Firebase Cloud Messaging
- Android Navigation Component

## Firebase

Naziv Firebase projekta: **slagalica-tim21**

Da biste dobili pristup Firebase projektu, kontaktirajte Student 1 da vas doda kao saradnika:
1. Student 1 ide na [console.firebase.google.com](https://console.firebase.google.com)
2. Odabere projekat `slagalica-tim21`
3. Klikne na zupčanik pored "Project Overview" → **Project settings**
4. Ide na tab **Users and permissions**
5. Klikne **Add member** i unese vaš Gmail

Nakon što dobijete pristup, preuzmite `google-services.json`:
1. Idite na [console.firebase.google.com](https://console.firebase.google.com)
2. Odaberite projekat `slagalica-tim21`
3. Kliknite na zupčanik → **Project settings**
4. U sekciji **Your apps** pronađite Android app
5. Preuzmite `google-services.json`
6. Kopirajte ga u `app/` folder projekta

> **Napomena:** `google-services.json` fajl nije uključen u repozitorijum iz sigurnosnih razloga i mora se dodati ručno!

## Pokretanje aplikacije

### Preduslovi

- Android Studio (najnovija verzija)
- JDK 17
- Android SDK API 26+
- Git

### Koraci

1. Klonirajte repozitorijum:
   ```
   git clone https://github.com/tomaflus/e2-ma-tim21-2026.git
   ```

2. Otvorite projekat u Android Studio:
   - File → Open → odaberite folder `e2-ma-tim21-2026`

3. Dodajte `google-services.json` fajl:
   - Preuzmite sa Firebase konzole (vidi sekciju Firebase iznad)
   - Kopirajte ga u `app/` folder projekta

4. Sačekajte da Gradle sync završi automatski

5. Kreirajte virtuelni uređaj:
   - Tools → Device Manager → Add a new device
   - Odaberite Pixel 6, API 35+
   - Kliknite Finish

6. Pokrenite aplikaciju:
   - Odaberite kreirani emulator iz dropdown liste
   - Kliknite zelenu strelicu Run

## Poznati problemi i rješenja

### Gradle / AGP kompatibilnost

Projekat koristi sljedeće verzije:

| Komponenta | Verzija |
|------------|---------|
| Gradle | 8.11.1 |
| AGP (Android Gradle Plugin) | 8.7.3 |

**Problem:** Greška `Unable to load class 'org.gradle.platform.base.Platform'` ili `Could not install Gradle distribution` nastaje zbog neusklađenosti Gradle i AGP verzija.

**Rješenje:**

1. U `gradle/libs.versions.toml` provjeri da piše:
   ```toml
   agp = "8.7.3"
   ```

2. U `gradle/wrapper/gradle-wrapper.properties` provjeri da piše:
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
   ```

3. U `gradle.properties` mora postojati:
   ```properties
   android.useAndroidX=true
   ```

4. Uradi **File → Sync Project with Gradle Files**

> **Napomena:** Ako Gradle ne može da se preuzme automatski (problem sa internetom), preuzmi `gradle-8.11.1-bin.zip` ručno sa [services.gradle.org](https://services.gradle.org/distributions/) i kopiraj u `C:\Users\<ime>\.gradle\wrapper\dists\`

## Git workflow

Svaki student radi na svojoj grani i spaja na `main` nakon svake kontrolne tačke.

### Kreiranje vlastite grane (primjer za Student 2 KT1):
```
git checkout main
git pull origin main
git checkout -b student2/kt1
git push origin student2/kt1
```

### Spajanje na main nakon KT:
```
git checkout main
git pull origin main
git merge student2/kt1
git push origin main
```

### Pregled grana:
- `main` — stabilna verzija nakon svake KT
- `student1/kt1` — Student 1 (KT1)
- `student2/kt1` — Student 2 (KT1)
- `student3/kt1` — Student 3 (KT1)

## Preporuka za rad (Demo)

> Ovo je preporučeni način rada koji se može mijenjati tokom razvoja prema potrebama tima.

### Arhitektura

Projekat koristi **troslojna arhitektura**:

```
Fragment (Prezentacija)
    ↓
Repository (Poslovna logika)
    ↓
Firebase (Baza podataka)
```

- **Fragment** — prikazuje podatke korisniku, reaguje na klikove
- **Repository** — sadrži poslovnu logiku, komunicira sa Firebase-om
- **Firebase** — čuva i dohvata podatke

### Preporučeni redosljed rada za svakog studenta

1. **Kreiraj XML layout** (GUI ekrana)
2. **Kreiraj Fragment klasu** i poveži je sa layoutom koristeći ViewBinding
3. **Dodaj fragment u nav_graph.xml**
4. **Kreiraj Model klasu** (npr. `User.java`, `Partija.java`)
5. **Kreiraj Repository klasu** koja komunicira sa Firebase-om
6. **Povezi Fragment sa Repository-em**

### Primjer dodavanja novog ekrana

**1. Kreiraj XML layout** u `res/layout/`:
```xml
<!-- fragment_novi_ekran.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    ...
</LinearLayout>
```

**2. Kreiraj Fragment klasu** u odgovarajućem paketu:
```java
public class NoviEkranFragment extends Fragment {
    private FragmentNoviEkranBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNoviEkranBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

**3. Dodaj u nav_graph.xml**:
```xml
<fragment
    android:id="@+id/noviEkranFragment"
    android:name="com.elfak.slagalica.fragments.NoviEkranFragment"
    android:label="Novi ekran"
    tools:layout="@layout/fragment_novi_ekran"/>
```

### Komunikacija sa Firebase-om (primjer Repository)

```java
public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Dohvati korisnika
    public void getUser(String userId, OnSuccessListener<User> onSuccess) {
        db.collection("users")
          .document(userId)
          .get()
          .addOnSuccessListener(doc -> {
              User user = doc.toObject(User.class);
              onSuccess.onSuccess(user);
          });
    }

    // Sačuvaj korisnika
    public void saveUser(User user) {
        db.collection("users")
          .document(user.getId())
          .set(user);
    }
}
```

### Firebase Firestore struktura baze

```
firestore/
├── users/                    # Korisnici
│   └── {userId}/
│       ├── korisnickoIme
│       ├── email
│       ├── region
│       ├── tokeni
│       ├── zvezde
│       └── liga
│
├── partije/                  # Partije
│   └── {partijaId}/
│       ├── igrac1Id
│       ├── igrac2Id
│       ├── status
│       └── rezultat
│
├── rangListe/                # Rang liste
│   ├── nedjeljna/
│   └── mjesecna/
│
└── chat/                     # Chat poruke
    └── {regionId}/
        └── poruke/
```

### Dogovor oko zajedničkih fajlova

Fajlovi koje svi studenti koriste i mijenjaju:
- `nav_graph.xml` — svako dodaje svoje fragmente
- `AndroidManifest.xml` — svako dodaje svoje servise/aktivnosti
- `strings.xml` — svako dodaje svoje stringove

**Preporuka:** Prije nego što mijenjate zajednički fajl, obavijestite ostale članove tima da izbjegnete konflikte na GitHubu!

## Struktura projekta

Projekat se razvija po troslojna arhitekturi. Svaki student dodaje svoje klase u odgovarajuće pakete.

```
app/src/main/java/com/elfak/slagalica/
│
├── activities/
│   └── MainActivity.java                  # Zajednički kontejner za fragmente
│
├── fragments/
│   ├── auth/                              # Student 1
│   │   ├── LoginFragment.java
│   │   └── RegisterFragment.java
│   │
│   ├── games/                             # Svi studenti dodaju svoje igre
│   │   ├── KorakPoKorakFragment.java      # Student 1
│   │   ├── MojBrojFragment.java           # Student 1
│   │   ├── KoZnaZnaFragment.java          # Student 2
│   │   ├── SpojniceFragment.java          # Student 2
│   │   ├── AsocijacijeFragment.java       # Student 3
│   │   └── ScockoFragment.java            # Student 3
│   │
│   ├── profile/                           # Student 2
│   │   └── ProfilFragment.java
│   │
│   ├── region/                            # Student 2
│   │   └── RegionFragment.java
│   │
│   ├── league/                            # Student 2
│   │   └── LigaFragment.java
│   │
│   ├── friends/                           # Student 2
│   │   └── PrijateljiFragment.java
│   │
│   ├── ranking/                           # Student 3
│   │   └── RangListaFragment.java
│   │
│   ├── tournament/                        # Student 3
│   │   └── TurnirFragment.java
│   │
│   ├── notifications/                     # Student 3
│   │   └── NotifikacijeFragment.java
│   │
│   ├── missions/                          # Student 3
│   │   └── DnevneMisijeFragment.java
│   │
│   ├── chat/                              # Student 1
│   │   └── CetFragment.java
│   │
│   ├── challenge/                         # Student 1
│   │   └── IzazovFragment.java
│   │
│   └── game/                              # Student 1
│       └── IgraFragment.java
│
├── model/                                 # Svi studenti dodaju svoje modele
│   ├── User.java
│   ├── Partija.java
│   ├── Igra.java
│   └── ...
│
├── repository/                            # Svi studenti dodaju svoje repozitorije
│   ├── UserRepository.java
│   ├── PartijaRepository.java
│   └── ...
│
└── services/                              # Svi studenti dodaju svoje servise
    ├── AuthService.java
    ├── NotifikacijeService.java
    └── ...
```

## Napomena o razvoju

- Svaki student radi **isključivo** na svojoj grani tokom KT
- Zajednički fajlovi se ažuriraju dogovorom između studenata
- Nakon svake KT, grane se spajaju na `main`
- Firebase Firestore se koristi za sve podatke
- Troslojna arhitektura mora biti jasno razdvojena
- Ova preporuka za rad se može mijenjati tokom razvoja prema potrebama tima
