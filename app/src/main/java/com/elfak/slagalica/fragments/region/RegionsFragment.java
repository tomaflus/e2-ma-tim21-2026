package com.elfak.slagalica.fragments.region;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elfak.slagalica.R;
import com.elfak.slagalica.databinding.FragmentRegionsBinding;
import com.elfak.slagalica.model.Region;
import com.elfak.slagalica.model.User;
import com.elfak.slagalica.service.RegionService;
import com.elfak.slagalica.util.RegionUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionsFragment extends Fragment {
    private FragmentRegionsBinding binding;
    private RegionService regionService;
    private String currentUserRegion = "";
    private Region selectedRegion = null;

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
        loadInitialData();

        if (!com.elfak.slagalica.BuildConfig.DEBUG) {
            binding.btnSeed.setVisibility(View.GONE);
        }

        binding.btnSeed.setOnClickListener(v -> {
            binding.btnSeed.setEnabled(false);
            regionService.seedDebugData(() -> {
                Toast.makeText(getContext(), "Podaci generisani!", Toast.LENGTH_SHORT).show();
                refreshData();
                binding.btnSeed.setEnabled(true);
            });
        });

        binding.fabInfo.setOnClickListener(v -> {
            if (binding.llLeaderboard.getVisibility() == View.VISIBLE) {
                binding.llLeaderboard.setVisibility(View.GONE);
            } else {
                binding.llLeaderboard.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadInitialData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(auth.getCurrentUser().getUid())
                .get().addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setId(doc.getId());
                        currentUserRegion = user.getRegion();
                        
                        // Check for cycle transition and rewards
                        regionService.handleCycleTransition(user, this::refreshData);
                    } else {
                        refreshData();
                    }
                });
        } else {
            refreshData();
        }
    }

    private void refreshData() {
        if (binding == null) return;
        binding.mapView.getOverlays().clear();
        fetchRanking();
        loadUserMarkers();
        loadRegionPolygons();
        binding.mapView.invalidate();
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        binding.mapView.setMultiTouchControls(true);

        GeoPoint serbiaCenter = new GeoPoint(44.0, 20.8);
        binding.mapView.getController().setZoom(7.0);
        binding.mapView.getController().setCenter(serbiaCenter);
    }

    private void fetchRanking() {
        regionService.calculateRegionalRanking(ranking -> {
            if (binding != null) {
                binding.rvRegionRanking.setLayoutManager(new LinearLayoutManager(getContext()));
                binding.rvRegionRanking.setAdapter(new RegionRankingAdapter(ranking, currentUserRegion));
            }
        });
    }

    private void loadUserMarkers() {
        regionService.loadUsersWithCoords(users -> {
            if (binding != null) {
                // Simple manual clustering
                Map<String, List<User>> clusters = new HashMap<>();
                double gridSize = 0.15; // Roughly group players within ~15km
                
                for (User user : users) {
                    long latCell = Math.round(user.getLatitude() / gridSize);
                    long lonCell = Math.round(user.getLongitude() / gridSize);
                    String key = latCell + "_" + lonCell;
                    
                    if (!clusters.containsKey(key)) clusters.put(key, new ArrayList<>());
                    clusters.get(key).add(user);
                }
                
                for (List<User> cluster : clusters.values()) {
                    if (cluster.size() == 1) {
                        User u = cluster.get(0);
                        addUserMarker(new GeoPoint(u.getLatitude(), u.getLongitude()), u);
                    } else {
                        addClusterMarker(cluster);
                    }
                }
            }
        });
    }

    private void addClusterMarker(List<User> cluster) {
        double avgLat = 0, avgLon = 0;
        for (User u : cluster) {
            avgLat += u.getLatitude();
            avgLon += u.getLongitude();
        }
        avgLat /= cluster.size();
        avgLon /= cluster.size();

        Marker marker = new Marker(binding.mapView);
        marker.setPosition(new GeoPoint(avgLat, avgLon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(createClusterDrawable(cluster.size()));
        marker.setTitle(cluster.size() + " igrača");
        binding.mapView.getOverlays().add(marker);
    }

    private Drawable createClusterDrawable(int count) {
        int size = (int) (28 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(size/2f, size/2f, size/2f, paint);

        paint.setColor(Color.parseColor("#1976D2")); // Darker blue for clusters
        canvas.drawCircle(size/2f, size/2f, size/2f * 0.85f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(String.valueOf(count), size/2f, size/2f + 12, paint);

        return new BitmapDrawable(getResources(), bitmap);
    }

    private void addUserMarker(GeoPoint point, User user) {
        Marker marker = new Marker(binding.mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        
        marker.setIcon(createAvatarMarker()); 
        marker.setTitle(user.getKorisnickoIme());
        marker.setSnippet("Liga: " + user.getLiga() + " | Zvezde: " + user.getZvezde());
        binding.mapView.getOverlays().add(marker);
    }

    private Drawable createAvatarMarker() {
        int size = (int) (22 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size/2f, size/2f, size/2f, paint);
        
        paint.setColor(Color.parseColor("#2196F3")); // Modern blue
        canvas.drawCircle(size/2f, size/2f, size/2f * 0.85f, paint);
        
        return new BitmapDrawable(getResources(), bitmap);
    }

    private void loadRegionPolygons() {
        for (Region r : Region.values()) {
            drawRegionPolygon(r);
            addRegionLabel(r);
        }
    }

    private void drawRegionPolygon(Region region) {
        Polygon polygon = new Polygon();
        List<GeoPoint> pts = RegionUtil.getPolygonForRegion(region.getFullName());
        polygon.setPoints(pts);

        int fillColor = region.getColor(); 
        int outlineColor = Color.WHITE;
        float outlineWidth = 4.0f;

        if (region.getFullName().equalsIgnoreCase(currentUserRegion)) {
            outlineColor = Color.parseColor("#FFD700"); // Gold for user's region
            outlineWidth = 8.0f;
        }

        if (selectedRegion != null && selectedRegion == region) {
            fillColor = Color.argb(120, Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor));
            outlineWidth = 10.0f;
        }

        polygon.getFillPaint().setColor(fillColor);
        polygon.getOutlinePaint().setColor(outlineColor);
        polygon.getOutlinePaint().setStrokeWidth(outlineWidth);
        polygon.getOutlinePaint().setStrokeJoin(android.graphics.Paint.Join.ROUND);
        polygon.getOutlinePaint().setStrokeCap(android.graphics.Paint.Cap.ROUND);

        polygon.setOnClickListener((poly, map, eventPos) -> {
            selectedRegion = region;
            refreshData(); // Redraw with highlight
            prikaziRegionDialog(region);
            return true;
        });

        binding.mapView.getOverlays().add(polygon);
    }

    private void addRegionLabel(Region region) {
        Marker label = new Marker(binding.mapView);
        label.setPosition(new GeoPoint(region.getCenterLat(), region.getCenterLon()));
        label.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        label.setIcon(createLabelDrawable(region));
        label.setOnMarkerClickListener((m, mapView) -> {
            prikaziRegionDialog(region);
            return true;
        });
        binding.mapView.getOverlays().add(label);
    }

    private Drawable createLabelDrawable(Region region) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(36);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        paint.setShadowLayer(12, 0, 0, Color.BLACK);
        
        String text = region.getShortName(); // Use short name for map labels
        float textWidth = paint.measureText(text);
        
        Drawable icon = ContextCompat.getDrawable(getContext(), region.getIconRes());
        int iconSize = 52;
        int padding = 15;
        
        Bitmap bitmap = Bitmap.createBitmap((int)textWidth + iconSize + padding * 2, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        if (icon != null) {
            icon.setTint(Color.WHITE);
            icon.setBounds(0, 24, iconSize, iconSize + 24);
            icon.draw(canvas);
        }
        
        canvas.drawText(text, iconSize + padding, 64, paint);
        
        return new BitmapDrawable(getResources(), bitmap);
    }

    private void prikaziRegionDialog(Region r) {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_region_stats, null);
        
        TextView tvName = view.findViewById(R.id.tvRegionName);
        TextView tvPrvo = view.findViewById(R.id.tvPrvoMesto);
        TextView tvDrugo = view.findViewById(R.id.tvDrugoMesto);
        TextView tvTrece = view.findViewById(R.id.tvTreceMesto);
        TextView tvActive = view.findViewById(R.id.tvActivePlayers);
        TextView tvTotal = view.findViewById(R.id.tvTotalPlayers);
        Button btnClose = view.findViewById(R.id.btnClose);

        tvName.setText(r.getFullName());
        tvName.setCompoundDrawablesWithIntrinsicBounds(r.getIconRes(), 0, 0, 0);
        tvName.setCompoundDrawablePadding(24);
        
        regionService.getRegionStats(r, (active, total, first, second, third) -> {
            tvPrvo.setText("🥇 Prvo mesto: " + first);
            tvDrugo.setText("🥈 Drugo mesto: " + second);
            tvTrece.setText("🥉 Treće mesto: " + third);
            tvActive.setText("👤 Trenutno aktivnih igrača: " + active);
            tvTotal.setText("👥 Ukupno registrovanih igrača: " + total);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public void onResume() { super.onResume(); binding.mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); binding.mapView.onPause(); }
    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
