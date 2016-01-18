package at.jku.cis.radar.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewParent;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

import at.jku.cis.radar.activity.EvolutionActivity;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.task.GetFeaturesEvolutionTask;


public class EvolutionView extends MapView implements OnMapReadyCallback {
    private GeoJsonLayer geoJsonLayer;
    private GoogleMap googleMap;

    private Event event;
    private long featureId;

    public EvolutionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.geoJsonLayer = new GeoJsonLayer(googleMap, new JSONObject());
        this.geoJsonLayer.addLayerToMap();
        Event event =(Event) ((EvolutionActivity)getContext()).getIntent().getExtras().get("event");

        int color = event.getColor();
        List<GeoJsonFeature> geoJsonFeatures = loadGeoJsonFeatures();
        GeoJsonPolygonStyle style;
        for (GeoJsonFeature geoJsonFeature : geoJsonFeatures) {
            //Color.RED;
            style = new GeoJsonPolygonStyle();
            style.setFillColor(color);
            style.setStrokeColor(Color.WHITE);
            geoJsonFeature.setPolygonStyle(style);
            color =  color - 0x000F0F0F;
            color = color | 0xFF000000;
            geoJsonLayer.addFeature(geoJsonFeature);
        }
    }

    private List<GeoJsonFeature> loadGeoJsonFeatures() {
        try {
            return new GetFeaturesEvolutionTask().execute(event.getId(), featureId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleFeatureGroupVisible(final Event event, final long featureId) {
        this.event = event;
        this.featureId = featureId;
    }
}