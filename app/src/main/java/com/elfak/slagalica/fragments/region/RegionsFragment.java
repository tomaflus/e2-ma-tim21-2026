package com.elfak.slagalica.fragments.region;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentRegionsBinding;
import com.elfak.slagalica.model.Region;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.service.RegionService;
import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RegionsFragment extends Fragment {
    private FragmentRegionsBinding binding;
    private RegionService regionService;
    private String currentUserRegion = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        binding = FragmentRegionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        regionService = new RegionService();

        setupMap();
        loadLeaderboard();
        loadUserPoints();
        loadRegionMarkers();
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        binding.mapView.setMultiTouchControls(true);

        GeoPoint serbiaCenter = new GeoPoint(44.0165, 21.0059);
        binding.mapView.getController().setZoom(7.5);
        binding.mapView.getController().setCenter(serbiaCenter);
    }

    private void loadLeaderboard() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(auth.getCurrentUser().getUid())
                .get().addOnSuccessListener(doc -> {
                    currentUserRegion = doc.getString("region");
                    fetchRanking();
                });
        } else {
            fetchRanking();
        }
    }

    private void fetchRanking() {
        regionService.calculateRegionalRanking(ranking -> {
            if (binding != null) {
                binding.rvRegionRanking.setLayoutManager(new LinearLayoutManager(getContext()));
                binding.rvRegionRanking.setAdapter(new RegionRankingAdapter(ranking, currentUserRegion));
            }
        });
    }

    private void loadUserPoints() {
        regionService.loadUsersWithCoords(users -> {
            if (binding != null) {
                for (User user : users) {
                    addUserPoint(new GeoPoint(user.getLatitude(), user.getLongitude()), user.getKorisnickoIme());
                }
            }
        });
    }

    private void addUserPoint(GeoPoint point, String username) {
        Marker marker = new Marker(binding.mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        // Using a distinct dot icon with high contrast
        marker.setIcon(getResources().getDrawable(android.R.drawable.presence_online)); 
        marker.setAlpha(1.0f); // High visibility
        marker.setTitle(username);
        binding.mapView.getOverlays().add(marker);
    }

    private void loadRegionMarkers() {
        for (Region r : Region.values()) {
            drawRegionPolygon(r);
            addRegionCenterIcon(new GeoPoint(r.getCenterLat(), r.getCenterLon()), r);
        }
    }

    private void drawRegionPolygon(Region region) {
        Polygon polygon = new Polygon();
        List<GeoPoint> pts = getRegionPoints(region);
        polygon.setPoints(pts);

        int fillColor = Color.argb(40, 255, 255, 255); // Default white transparent
        int outlineColor = Color.WHITE;
        float outlineWidth = 2.0f;

        if (region.getFullName().equalsIgnoreCase(currentUserRegion)) {
            fillColor = Color.argb(80, 255, 215, 0); // Gold highlight
            outlineColor = Color.YELLOW;
            outlineWidth = 5.0f;
        }

        polygon.getFillPaint().setColor(fillColor);
        polygon.getOutlinePaint().setColor(outlineColor);
        polygon.getOutlinePaint().setStrokeWidth(outlineWidth);

        polygon.setOnClickListener((poly, map, eventPos) -> {
            prikaziRegion(region);
            return true;
        });

        binding.mapView.getOverlays().add(polygon);
    }

    private void addRegionCenterIcon(GeoPoint point, Region region) {
        Marker marker = new Marker(binding.mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(getResources().getDrawable(region.getIconRes()));
        marker.setTitle(region.getFullName());
        marker.setOnMarkerClickListener((m, mapView) -> {
            prikaziRegion(region);
            return true;
        });
        binding.mapView.getOverlays().add(marker);
    }

    private List<GeoPoint> getRegionPoints(Region r) {
        // Simplified but recognizable boundaries for Serbia's 5 regions
        switch (r) {
            case VOJVODINA:
                return Arrays.asList(
                    new GeoPoint(46.2, 18.8), new GeoPoint(46.2, 21.5),
                    new GeoPoint(44.8, 21.5), new GeoPoint(44.8, 18.8)
                );
            case BEOGRAD:
                return Arrays.asList(
                    new GeoPoint(44.9, 20.2), new GeoPoint(44.9, 20.7),
                    new GeoPoint(44.6, 20.7), new GeoPoint(44.6, 20.2)
                );
            case SUMADIJA:
                return Arrays.asList(
                    new GeoPoint(44.6, 19.0), new GeoPoint(44.6, 21.0),
                    new GeoPoint(43.0, 21.0), new GeoPoint(43.0, 19.0)
                );
            case ISTOCNA:
                return Arrays.asList(
                    new GeoPoint(44.6, 21.0), new GeoPoint(44.6, 23.0),
                    new GeoPoint(42.5, 23.0), new GeoPoint(42.5, 21.5),
                    new GeoPoint(43.3, 21.0)
                );
            case KOSOVO:
                return Arrays.asList(
                    new GeoPoint(43.3, 20.5), new GeoPoint(43.3, 21.5),
                    new GeoPoint(41.8, 21.5), new GeoPoint(41.8, 20.5)
                );
            default: return new ArrayList<>();
        }
    }

    private void prikaziRegion(Region r) {
        binding.llRegionDetails.setVisibility(View.VISIBLE);
        binding.tvRegionName.setText(r.getFullName());
        regionService.getRegionStats(r, (active, total, first, second, third) -> {
            if (binding != null) {
                binding.tvRegionStats.setText(
                    getString(R.string.label_region_active_players, active) + "\n" +
                    getString(R.string.label_region_total_players, total) + "\n" +
                    getString(R.string.label_region_top3_stats, first, second, third)
                );
            }
        });
    }

    @Override
    public void onResume() { super.onResume(); binding.mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); binding.mapView.onPause(); }
    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}