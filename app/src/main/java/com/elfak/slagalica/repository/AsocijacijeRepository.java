package com.elfak.slagalica.repository;

import java.util.ArrayList;
import java.util.List;

public class AsocijacijeRepository {
    public static class Asocijacija {
        public String a1, a2, a3, a4, resA;
        public String b1, b2, b3, b4, resB;
        public String c1, c2, c3, c4, resC;
        public String d1, d2, d3, d4, resD;
        public String konacno;

        public Asocijacija(String a1, String a2, String a3, String a4, String resA,
                          String b1, String b2, String b3, String b4, String resB,
                          String c1, String c2, String c3, String c4, String resC,
                          String d1, String d2, String d3, String d4, String resD,
                          String konacno) {
            this.a1 = a1; this.a2 = a2; this.a3 = a3; this.a4 = a4; this.resA = resA;
            this.b1 = b1; this.b2 = b2; this.b3 = b3; this.b4 = b4; this.resB = resB;
            this.c1 = c1; this.c2 = c2; this.c3 = c3; this.c4 = c4; this.resC = resC;
            this.d1 = d1; this.d2 = d2; this.d3 = d3; this.d4 = d4; this.resD = resD;
            this.konacno = konacno;
        }
    }

    public List<Asocijacija> getMockAsocijacije() {
        List<Asocijacija> list = new ArrayList<>();
        list.add(new Asocijacija(
            "VUK", "OVCA", "MESO", "PASTIR", "ČOPOR",
            "CRVEN", "PLAV", "ZELEN", "BOJA", "SPEKTAR",
            "REKA", "JEZERO", "MORE", "OKEAN", "VODA",
            "KAPA", "ŠAL", "RUKAVICE", "SNEG", "ZIMA",
            "PRIRODA"
        ));
        list.add(new Asocijacija(
            "BROJ", "CIFRA", "ZBIR", "RAZLIKA", "MATEMATIKA",
            "DRŽAVA", "NAROD", "GRANICA", "ZASTAVA", "ZEMLJA",
            "ŠKOLA", "UČENIK", "KNJIGA", "ZADATAK", "OBRAZOVANJE",
            "REČ", "SLOVO", "JEZIK", "PISMO", "KOMUNIKACIJA",
            "ZNANJE"
        ));
        return list;
    }
}