package com.elfak.slagalica.util;

import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RegionUtil {

    public static List<GeoPoint> getVojvodinaPolygon() {
        return Arrays.asList(
            new GeoPoint(45.92, 18.85), new GeoPoint(46.12, 19.13), new GeoPoint(46.18, 19.65), 
            new GeoPoint(46.15, 20.01), new GeoPoint(46.11, 20.48), new GeoPoint(46.18, 20.76), 
            new GeoPoint(46.06, 21.14), new GeoPoint(46.12, 21.48), new GeoPoint(45.75, 21.42), 
            new GeoPoint(45.45, 21.28), new GeoPoint(45.22, 21.51), new GeoPoint(44.82, 21.41), 
            new GeoPoint(44.80, 20.94), new GeoPoint(44.88, 20.64), new GeoPoint(44.98, 20.64), 
            new GeoPoint(45.02, 20.40), new GeoPoint(44.95, 20.20), new GeoPoint(44.82, 19.98), 
            new GeoPoint(44.86, 19.22), new GeoPoint(44.92, 18.82), new GeoPoint(45.28, 18.81), 
            new GeoPoint(45.54, 18.91)
        );
    }

    public static List<GeoPoint> getBeogradPolygon() {
        return Arrays.asList(
            new GeoPoint(44.98, 20.64), new GeoPoint(44.88, 20.64), new GeoPoint(44.75, 20.75), 
            new GeoPoint(44.52, 20.68), new GeoPoint(44.42, 20.45), new GeoPoint(44.45, 20.25), 
            new GeoPoint(44.72, 20.15), new GeoPoint(44.82, 19.98), new GeoPoint(44.95, 20.20), 
            new GeoPoint(45.02, 20.40)
        );
    }

    public static List<GeoPoint> getSumadijaPolygon() {
        return Arrays.asList(
            new GeoPoint(44.82, 19.98), new GeoPoint(44.72, 20.15), new GeoPoint(44.45, 20.25), 
            new GeoPoint(44.42, 20.45), new GeoPoint(44.52, 20.68), new GeoPoint(44.41, 20.95), 
            new GeoPoint(44.15, 21.10), new GeoPoint(43.85, 21.15), new GeoPoint(43.52, 21.25), 
            new GeoPoint(43.32, 21.18), new GeoPoint(43.27, 20.82), new GeoPoint(43.13, 20.61), 
            new GeoPoint(42.98, 20.51), new GeoPoint(42.84, 20.28), new GeoPoint(42.66, 20.18), 
            new GeoPoint(42.85, 19.75), new GeoPoint(43.15, 19.25), new GeoPoint(43.55, 19.15), 
            new GeoPoint(43.78, 19.52), new GeoPoint(44.18, 19.05), new GeoPoint(44.58, 19.12), 
            new GeoPoint(44.86, 19.22)
        );
    }

    public static List<GeoPoint> getIstocnaPolygon() {
        return Arrays.asList(
            new GeoPoint(44.82, 21.41), new GeoPoint(44.65, 22.15), new GeoPoint(44.82, 22.45), 
            new GeoPoint(44.52, 22.78), new GeoPoint(44.25, 22.52), new GeoPoint(43.95, 22.95), 
            new GeoPoint(43.42, 22.62), new GeoPoint(42.85, 23.02), new GeoPoint(42.32, 22.58), 
            new GeoPoint(42.28, 22.35), new GeoPoint(42.22, 21.47), new GeoPoint(42.36, 21.60), 
            new GeoPoint(42.58, 21.68), new GeoPoint(42.75, 21.73), new GeoPoint(42.94, 21.46), 
            new GeoPoint(43.05, 21.28), new GeoPoint(43.16, 21.05), new GeoPoint(43.27, 20.82), 
            new GeoPoint(43.32, 21.18), new GeoPoint(43.52, 21.25), new GeoPoint(43.85, 21.15), 
            new GeoPoint(44.15, 21.10), new GeoPoint(44.41, 20.95), new GeoPoint(44.52, 20.68), 
            new GeoPoint(44.75, 20.75), new GeoPoint(44.88, 20.64), new GeoPoint(44.80, 20.94)
        );
    }

    public static List<GeoPoint> getKosovoPolygon() {
        return Arrays.asList(
            new GeoPoint(43.27, 20.82), new GeoPoint(43.16, 21.05), new GeoPoint(43.05, 21.28), 
            new GeoPoint(42.94, 21.46), new GeoPoint(42.75, 21.73), new GeoPoint(42.58, 21.68), 
            new GeoPoint(42.36, 21.60), new GeoPoint(42.22, 21.47), new GeoPoint(42.20, 21.32), 
            new GeoPoint(42.10, 21.37), new GeoPoint(42.01, 21.25), new GeoPoint(41.86, 20.96), 
            new GeoPoint(41.85, 20.59), new GeoPoint(41.95, 20.60), new GeoPoint(42.06, 20.50), 
            new GeoPoint(42.15, 20.35), new GeoPoint(42.32, 20.08), new GeoPoint(42.52, 20.06), 
            new GeoPoint(42.66, 20.18), new GeoPoint(42.84, 20.28), new GeoPoint(42.98, 20.51), 
            new GeoPoint(43.13, 20.61)
        );
    }

    public static List<GeoPoint> getPolygonForRegion(String regionName) {
        if (regionName == null) return getBeogradPolygon();
        if (regionName.contains("Vojvodina")) return getVojvodinaPolygon();
        if (regionName.contains("Beograd")) return getBeogradPolygon();
        if (regionName.contains("Šumadija") || regionName.contains("zapadna")) return getSumadijaPolygon();
        if (regionName.contains("Južna") || regionName.contains("istočna")) return getIstocnaPolygon();
        if (regionName.contains("Kosovo")) return getKosovoPolygon();
        return getBeogradPolygon();
    }

    public static boolean isPointInPolygon(GeoPoint point, List<GeoPoint> polygon) {
        int intersectCount = 0;
        for (int j = 0; j < polygon.size(); j++) {
            GeoPoint a = polygon.get(j);
            GeoPoint b = polygon.get((j + 1) % polygon.size());
            if (rayCastIntersect(point, a, b)) {
                intersectCount++;
            }
        }
        return (intersectCount % 2) == 1;
    }

    private static boolean rayCastIntersect(GeoPoint tap, GeoPoint vertA, GeoPoint vertB) {
        double aY = vertA.getLatitude();
        double bY = vertB.getLatitude();
        double aX = vertA.getLongitude();
        double bX = vertB.getLongitude();
        double pY = tap.getLatitude();
        double pX = tap.getLongitude();

        if (vertA.getLongitude() == vertB.getLongitude()) {
             if (pX < vertA.getLongitude() && pY >= Math.min(aY, bY) && pY < Math.max(aY, bY)) return true;
             return false;
        }

        if ((aY > pY && bY > pY) || (aY < pY && bY < pY) || (aX < pX && bX < pX)) {
            return false;
        }

        double m = (aY - bY) / (aX - bX);
        double bee = aY - (m * aX);
        double xIntersection = (pY - bee) / m;

        return xIntersection > pX;
    }

    public static GeoPoint getRandomPointInRegion(String regionName) {
        List<GeoPoint> polygon = getPolygonForRegion(regionName);
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (GeoPoint p : polygon) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }

        Random random = new Random();
        for (int i = 0; i < 400; i++) {
            double lat = minLat + (maxLat - minLat) * random.nextDouble();
            double lon = minLon + (maxLon - minLon) * random.nextDouble();
            GeoPoint point = new GeoPoint(lat, lon);
            if (isPointInPolygon(point, polygon)) {
                return point;
            }
        }
        return new GeoPoint((minLat + maxLat) / 2, (minLon + maxLon) / 2);
    }
}
