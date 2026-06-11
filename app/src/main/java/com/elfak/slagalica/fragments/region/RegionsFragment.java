package com.elfak.slagalica.fragments.region;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elfak.slagalica.databinding.FragmentRegionsBinding;
import com.elfak.slagalica.model.Region;
import com.elfak.slagalica.service.RegionService;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;

import java.util.Random;

public class RegionsFragment extends Fragment {
    private FragmentRegionsBinding binding;
    private RegionService regionService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // OSMdroid zahteva ucitanu konfiguraciju pre inflatovanja mape
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        binding = FragmentRegionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        regionService = new RegionService();

        setupMap();
        loadAllMarkers();
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        binding.mapView.setMultiTouchControls(true);

        // Centriraj na Srbiju
        GeoPoint serbiaCenter = new GeoPoint(44.0165, 21.0059);
        binding.mapView.getController().setZoom(7.5);
        binding.mapView.getController().setCenter(serbiaCenter);
    }

    private void loadAllMarkers() {
        // Dodaj markere za regione (Vojvodina, Beograd, Nis...)
        addRegionMarker(new GeoPoint(45.2609, 19.8317), Region.VOJVODINA);
        addRegionMarker(new GeoPoint(44.7866, 20.4489), Region.BEOGRAD);
        addRegionMarker(new GeoPoint(43.9373, 20.3703), Region.SUMADIJA);
        addRegionMarker(new GeoPoint(43.3209, 21.8958), Region.ISTOCNA);
        addRegionMarker(new GeoPoint(42.6629, 21.1655), Region.KOSOVO);
    }

    private void addRegionMarker(GeoPoint point, Region region) {
        Marker marker = new Marker(binding.mapView);
        marker.setPosition(point);
        marker.setTitle(region.getFullName());
        marker.setOnMarkerClickListener((m, mapView) -> {
            prikaziRegion(region);
            return true;
        });
        binding.mapView.getOverlays().add(marker);
    }

    private void prikaziRegion(Region r) {
        binding.llRegionDetails.setVisibility(View.VISIBLE);
        binding.tvRegionName.setText(r.getFullName());
        regionService.getRegionStats(r, (active, total, first, second, third) -> {
            if (binding != null) {
                binding.tvRegionStats.setText("Aktivni igrači: " + active + "\nUkupno korisnika: " + total + "\nTop 3 mesta: " + first + "x");
            }
        });
    }

    @Override
    public void onResume() { super.onResume(); binding.mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); binding.mapView.onPause(); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}