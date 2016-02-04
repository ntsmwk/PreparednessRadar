package at.jku.cis.radar.view;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.edmodo.rangebar.RangeBar;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import at.jku.cis.radar.R;
import at.jku.cis.radar.activity.EvolutionActivity;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.task.GetFeaturesEvolutionTask;


public class EvolutionView extends MapView implements OnMapReadyCallback {
    private static final int _24HOURS_IN_MILLIS = 24 * 60 * 60 * 1000;
    private static final int _30MIN_IN_MILLIS = 30 * 60 * 1000;
    public static final int RANGEBAR_INDEX = 0;
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
        Event event = (Event) ((EvolutionActivity) getContext()).getIntent().getExtras().get("event");

        drawFeatureEvolutions(event, System.currentTimeMillis()-_24HOURS_IN_MILLIS, System.currentTimeMillis());
        getRangeBarView().setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int i, int i1) {
                removeFeaturesFromLayer();
                drawFeatureEvolutions((Event) ((EvolutionActivity) getContext()).getIntent().getExtras().get("event"),
                        System.currentTimeMillis() + i * _30MIN_IN_MILLIS - _24HOURS_IN_MILLIS, System.currentTimeMillis() + (i1 + 1) * _30MIN_IN_MILLIS - _24HOURS_IN_MILLIS);

            }
        });
    }

    private void removeFeaturesFromLayer() {
        List<GeoJsonFeature> removeList = new ArrayList<>();
        for(GeoJsonFeature feature : geoJsonLayer.getFeatures()){
            removeList.add(feature);
        }
        for(GeoJsonFeature feature : removeList){
            geoJsonLayer.removeFeature(feature);
        }
    }

    @Nullable
    private RangeBar getRangeBarView() {
        RangeBar rangebar = null;
        for(int i = 0; i <  ((ViewGroup)getParent()).getChildCount(); i++){
            if(((ViewGroup)getParent()).getChildAt(i).getId() == R.id.rangebarView){
                rangebar = (RangeBar) ((ViewGroup)((ViewGroup)getParent()).getChildAt(i)).getChildAt(RANGEBAR_INDEX);
            }
        }
        return rangebar;
    }


    private void drawFeatureEvolutions(Event event, final long from, final long to) {
        int color = event.getColor();
        int r = Color.red(color);
        int b = Color.blue(color);
        int g = Color.green(color);
        int a = Color.alpha(color);
        List<GeoJsonFeature> geoJsonFeatures = loadGeoJsonFeatures();
        Collections.sort(geoJsonFeatures, new Comparator<GeoJsonFeature>() {
            @Override
            public int compare(GeoJsonFeature lhs, GeoJsonFeature rhs) {
                return (int) (Long.parseLong(rhs.getProperty("date")) - Long.parseLong(lhs.getProperty("date")));
            }
        });
        CollectionUtils.filter(geoJsonFeatures, new Predicate<GeoJsonFeature>() {
            @Override
            public boolean evaluate(GeoJsonFeature object) {
                if ((Long.parseLong(object.getProperty("date")) > from) && (Long.parseLong(object.getProperty("date")) < to)) {
                    return true;
                }
                return false;
            }
        });
        GeoJsonPolygonStyle style;
        for (GeoJsonFeature geoJsonFeature : geoJsonFeatures) {
            style = new GeoJsonPolygonStyle();
            style.setStrokeColor(Color.argb(a, r, g, b));
            style.setFillColor(Color.argb(a, r, g, b));
            geoJsonFeature.setPolygonStyle(style);
            r = (int) (r * 0.9);
            g = (int) (g * 0.9);
            b = (int) (b * 0.9);
            a = (int) (a * 0.9);
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