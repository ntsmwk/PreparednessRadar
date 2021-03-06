package at.jku.cis.radar.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import at.jku.cis.radar.R;
import at.jku.cis.radar.activity.EvolutionActivity;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.rest.FeatureEvolutionRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;
import at.jku.cis.radar.transformer.JsonObject2GeoJsonFeatureTransformer;


public class EvolutionView extends MapView implements OnMapReadyCallback {
    public static final int RANGEBAR_INDEX = 1;
    public static final int STARTTIME_INDEX = 0;
    public static final int ENDTIME_INDEX = 2;
    private static final int _24HOURS_IN_MILLIS = 24 * 60 * 60 * 1000;
    private static final int _30MIN_IN_MILLIS = 30 * 60 * 1000;

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

        drawFeatureEvolutions(event, System.currentTimeMillis() - _24HOURS_IN_MILLIS, System.currentTimeMillis());
        setRangeBarChangeListener();
        setStartEndBeginTime(System.currentTimeMillis() - _24HOURS_IN_MILLIS, System.currentTimeMillis());
    }

    private void removeFeaturesFromLayer() {
        List<GeoJsonFeature> removeList = new ArrayList<>();
        for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
            removeList.add(feature);
        }
        for (GeoJsonFeature feature : removeList) {
            geoJsonLayer.removeFeature(feature);
        }
    }

    @Nullable
    private RangeBar getRangeBarView() {
        RangeBar rangebar = null;
        ViewGroup parent = (ViewGroup) getParent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getId() == R.id.rangebarView) {
                rangebar = (RangeBar) ((ViewGroup) ((ViewGroup) parent.getChildAt(i)).getChildAt(RANGEBAR_INDEX)).getChildAt(0);
            }
        }
        return rangebar;
    }

    private TextView getRangeBarTextView(int index) {
        TextView textView = null;
        ViewGroup parent = (ViewGroup) getParent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getId() == R.id.rangebarView) {
                textView = (TextView) ((ViewGroup) ((ViewGroup) parent.getChildAt(i)).getChildAt(index)).getChildAt(0);
            }
        }
        return textView;
    }


    private void drawFeatureEvolutions(Event event, final long from, final long to) {


        List<GeoJsonFeature> geoJsonFeatures = loadGeoJsonFeatures();
        sortList(geoJsonFeatures);
        filterList(from, to, geoJsonFeatures);

        GeoJsonPolygonStyle style;
        float zIndex = 1.0f;
        float diff = 1.0f / geoJsonFeatures.size();
        int eventColor = event.getColor();
        int grayColor = Color.GRAY;

        for (GeoJsonFeature geoJsonFeature : geoJsonFeatures) {
            int color;
            if (geoJsonFeature.getProperty(GoogleView.STATUS_PROPERTY_NAME.toLowerCase()).equalsIgnoreCase(GoogleView.STATUS_ERASED)) {
                color = grayColor;
            } else {
                color = eventColor;
            }
            style = new GeoJsonPolygonStyle();
            style.setStrokeColor(color);
            style.setFillColor(color);
            style.setZIndex(zIndex);
            geoJsonFeature.setPolygonStyle(style);

            geoJsonLayer.addFeature(geoJsonFeature);
            zIndex -= diff;
            eventColor = reduceColor(eventColor);
            grayColor = reduceColor(grayColor);
        }
    }

    private void filterList(final long from, final long to, List<GeoJsonFeature> geoJsonFeatures) {
        CollectionUtils.filter(geoJsonFeatures, new Predicate<GeoJsonFeature>() {
            @Override
            public boolean evaluate(GeoJsonFeature object) {
                if ((Long.parseLong(object.getProperty("creation_date")) > from) && (Long.parseLong(object.getProperty("creation_date")) < to)) {
                    return true;
                }
                return false;
            }
        });
    }

    private void sortList(List<GeoJsonFeature> geoJsonFeatures) {
        Collections.sort(geoJsonFeatures, new Comparator<GeoJsonFeature>() {
            @Override
            public int compare(GeoJsonFeature lhs, GeoJsonFeature rhs) {
                return (int) (Long.parseLong(rhs.getProperty("creation_date")) - Long.parseLong(lhs.getProperty("creation_date")));
            }
        });
    }

    private int reduceColor(int color){
        int r = Color.red(color);
        int b = Color.blue(color);
        int g = Color.green(color);
        int a = Color.alpha(color);
        r = (int) (r * 0.9);
        g = (int) (g * 0.9);
        b = (int) (b * 0.9);
        return Color.argb(a,r,g,b);
    }

    private List<GeoJsonFeature> loadGeoJsonFeatures() {
        try {
            String token = ((Activity) getContext()).getIntent().getStringExtra("token");
            return new GetFeaturesEvolutionTask(event, featureId, token).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void setRangeBarChangeListener() {
        getRangeBarView().setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int i, int i1) {
                removeFeaturesFromLayer();
                long startTime = System.currentTimeMillis() + i * _30MIN_IN_MILLIS - _24HOURS_IN_MILLIS;
                long endTime = System.currentTimeMillis() + (i1 + 1) * _30MIN_IN_MILLIS - _24HOURS_IN_MILLIS;
                drawFeatureEvolutions((Event) ((EvolutionActivity) getContext()).getIntent().getExtras().get("event"),
                        startTime, endTime);
                setStartEndBeginTime(startTime, endTime);
            }
        });
    }

    private void setRangeBarStartEndTime(int startEndTime, Date date) {
        DateFormat dateFormat = new SimpleDateFormat("dd.MMM HH:mm");
        getRangeBarTextView(startEndTime).setText(dateFormat.format(date));
    }

    private void setStartEndBeginTime(long begin, long end) {
        Date date = new Date();
        date.setTime(begin);
        setRangeBarStartEndTime(STARTTIME_INDEX, date);
        date.setTime(end);
        setRangeBarStartEndTime(ENDTIME_INDEX, date);
    }

    public void handleFeatureGroupVisible(final Event event, final long featureId) {
        this.event = event;
        this.featureId = featureId;
    }

    private class GetFeaturesEvolutionTask extends AsyncTask<Void, Void, List<GeoJsonFeature>> {

        private final Event event;
        private final long featureId;
        private final String token;

        public GetFeaturesEvolutionTask(Event event, long featureId, String token) {
            this.event = event;
            this.featureId = featureId;
            this.token = token;
        }

        @Override
        protected List<GeoJsonFeature> doInBackground(Void... params) {
            FeatureEvolutionRestApi featuresEvolutionRest = RestServiceGenerator.createService(FeatureEvolutionRestApi.class, token);
            return new JsonObject2GeoJsonFeatureTransformer().transform(featuresEvolutionRest.getFeaturesEvolution(event.getId(), featureId));
        }
    }


}