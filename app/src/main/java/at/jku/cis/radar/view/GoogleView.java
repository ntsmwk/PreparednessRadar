package at.jku.cis.radar.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import at.jku.cis.radar.R;
import at.jku.cis.radar.activity.EvolutionActivity;
import at.jku.cis.radar.activity.RadarActivity;
import at.jku.cis.radar.geometry.GeometryUtils;
import at.jku.cis.radar.model.ApplicationMode;
import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;
import at.jku.cis.radar.rest.FeaturesRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;
import at.jku.cis.radar.service.FeatureStyleService;
import at.jku.cis.radar.service.GeoJsonFeatureBuilder;
import at.jku.cis.radar.service.GeoJsonGeometryBuilder;
import at.jku.cis.radar.service.GeoJsonIntersectionRemover;
import at.jku.cis.radar.transformer.GeoJsonFeature2JsonObjectTransformer;
import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;
import at.jku.cis.radar.transformer.Geometry2GeoJsonGeometryTransformer;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class GoogleView extends MapView implements OnMapReadyCallback, EventTreeFragment.EventClickListener, GoogleMap.OnMapLongClickListener {
    public static String STATUS_PROPERTY_NAME = "STATUS";
    public static String STATUS_CREATED = "CREATED";
    public static String STATUS_ERASED = "ERASED";
    private final String TAG = "GoogleView";

    private GoogleMap googleMap;
    private boolean paintingEnabled = false;
    private PenSetting penSetting = new PenSetting();
    private ApplicationMode applicationMode = ApplicationMode.CREATING;

    private GeoJsonFeature currentFeature;
    private GeoJsonGeometryBuilder geoJsonGeometryBuilder;
    private Map<Event, GeoJsonLayer> event2GeoJsonLayer = new HashMap<>();
    private Projection googleMapProjection;

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PenSetting getPenSetting() {
        return penSetting;
    }

    public ApplicationMode getApplicationMode() {
        return applicationMode;
    }

    private void setCurrentFeature(GeoJsonFeature currentFeature) {
        FeatureStyleService featureStyleService = new FeatureStyleService();
        int color = penSetting.getEvent().getColor();
        if (this.currentFeature == null && currentFeature == null) {
            return;
        } else if (currentFeature == null) {
            this.currentFeature.setPointStyle(featureStyleService.createDefaultPointStyle(color));
            this.currentFeature.setPolygonStyle(featureStyleService.createDefaultPolygonStyle(color));
            this.currentFeature.setLineStringStyle(featureStyleService.createDefaultLineStringStyle(color));
            this.currentFeature = null;
        } else {
            this.currentFeature = currentFeature;
            this.currentFeature.setPointStyle(featureStyleService.createDefaultPointStyle(color));
            this.currentFeature.setPolygonStyle(featureStyleService.createEditPolygonStyle(color));
            this.currentFeature.setLineStringStyle(featureStyleService.createEditLineStringStyle());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (currentFeature != null) {
            setCurrentFeature(null);
        }
        Geometry geometry = new GeoJsonGeometry2GeometryTransformer().transform(new GeoJsonPoint(latLng));

        List<GeoJsonFeature> featureList = new ArrayList<>();
        GeoJsonLayer geoJsonLayer = getCorrespondingGeoJsonLayer();
        for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
            GeometryCollection geometryCollection = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(feature.getGeometry());
            if (GeometryUtils.intersects(geometryCollection, geometry)) {
                featureList.add(feature);
            }
        }
        if (featureList.size() == 1) {
            setCurrentFeature(featureList.get(0));
            showContextMenu();
        } else {
            Toast.makeText(getContext(), "Please click on the specific area without other overlapping events.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        menu.setHeaderTitle(R.string.context_menu);
        switch (applicationMode) {
            case CREATING:
                menu.add(NO_ID, 1, 0, R.string.edit);
                menu.add(NO_ID, 2, 0, R.string.evolve);
                break;
            case EDITING:
                menu.add(NO_ID, 0, 0, R.string.create);
                menu.add(NO_ID, 2, 0, R.string.evolve);
                break;
            case EVOLVING:
                menu.add(NO_ID, 0, 0, R.string.create);
                menu.add(NO_ID, 1, 0, R.string.edit);
                break;
        }
        menu.add(NO_ID, 3, 0, R.string.evolution);
        super.onCreateContextMenu(menu);
    }

    public void onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                setCurrentFeature(null);
                GoogleView.this.applicationMode = ApplicationMode.CREATING;
                break;
            case 1:
                GoogleView.this.applicationMode = ApplicationMode.EDITING;
                break;
            case 2:
                GoogleView.this.applicationMode = ApplicationMode.EVOLVING;
                break;
            case 3:
                GoogleView.this.applicationMode = ApplicationMode.CREATING;
                Intent intent = new Intent(getContext(), EvolutionActivity.class);
                intent.putExtra("event", penSetting.getEvent());
                intent.putExtra("featureId", currentFeature.getId());
                intent.putExtra("token", determineToken());
                setCurrentFeature(null);
                ((RadarActivity) getContext()).startActivity(intent);
                break;
        }
    }

    public void onContextMenuClosed() {
        switch (applicationMode) {
            case CREATING:
                setCurrentFeature(null);
                break;
        }
    }


    @Override
    public void handleEventLoaded(Event event) {
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEvent(event);
        if (event.isVisible()) {
            geoJsonLayer.addLayerToMap();
        } else {
            geoJsonLayer.removeLayerFromMap();
        }
    }

    @Override
    public void handleEventVisibleChanged(Event event) {
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEvent(event);
        if (event.isVisible()) {
            geoJsonLayer.addLayerToMap();
        } else {
            geoJsonLayer.removeLayerFromMap();
        }
    }

    @Override
    public void handleEventSelectionChanged(Event event) {
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEvent(event);
        if (event.isSelected()) {
            geoJsonLayer.addLayerToMap();
        }
        paintingEnabled = event.isSelected();
        penSetting.setEvent(event);
    }

    private GeoJsonLayer findGeoJsonLayerByEvent(Event event) {
        GeoJsonLayer geoJsonLayer = event2GeoJsonLayer.get(event);
        if (geoJsonLayer == null) {
            JSONObject jsonObject = loadFeatures(event);
            geoJsonLayer = new GeoJsonLayer(googleMap, jsonObject);
            geoJsonLayer.getDefaultPolygonStyle().setFillColor(event.getColor());
            geoJsonLayer.getDefaultPolygonStyle().setStrokeColor(event.getColor());
            geoJsonLayer.getDefaultLineStringStyle().setColor(event.getColor());
            event2GeoJsonLayer.put(event, geoJsonLayer);
        }
        return geoJsonLayer;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (googleMap != null) {
            if (paintingEnabled && motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS && penSetting.getEvent() != null) {
                dispatchStylusTouchEvent(motionEvent);
            } else if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    private void dispatchStylusTouchEvent(MotionEvent motionEvent) {
        Point point = new Point((int) motionEvent.getX(), (int) motionEvent.getY());

        if (PenMode.ERASING == penSetting.getPenMode()) {
            doErasing(motionEvent.getAction(), point);
        } else {
            doPainting(motionEvent.getAction(), point);
        }
    }

    private void doPainting(int action, Point point) {
        if (MotionEvent.ACTION_DOWN == action) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(penSetting.getDrawType());
            googleMapProjection = googleMap.getProjection();
        }
        geoJsonGeometryBuilder.addCoordinate(googleMapProjection.fromScreenLocation(point));
        if (MotionEvent.ACTION_UP == action) {
            GeoJsonGeometryCollection geoJsonGeometry = geoJsonGeometryBuilder.build();
            if (ApplicationMode.CREATING == applicationMode) {
                doCreateModePainting(geoJsonGeometry);
            } else if (ApplicationMode.EDITING == applicationMode) {
                doEditModePainting(geoJsonGeometry);
            } else {
                doEvolveModePainting(geoJsonGeometry);
            }
        }
    }

    private void doErasing(int action, Point point) {
        if (MotionEvent.ACTION_DOWN == action) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(DrawType.POLYGON);
            googleMapProjection = googleMap.getProjection();
        }
        geoJsonGeometryBuilder.addCoordinate(googleMapProjection.fromScreenLocation(point));
        if (MotionEvent.ACTION_UP == action) {
            GeoJsonLayer geoJsonLayer = getCorrespondingGeoJsonLayer();
            GeoJsonGeometryCollection geoJsonGeometry = geoJsonGeometryBuilder.build();
            if (ApplicationMode.CREATING == applicationMode) {
                doCreateModeErasing(geoJsonLayer.getFeatures(), geoJsonGeometry);
            } else if (ApplicationMode.EDITING == applicationMode) {
                doEditModeErasing(geoJsonGeometry);
            } else {
                doEvolveModeErasing(geoJsonGeometry);
            }
        }
    }

    private void doCreateModePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        GeometryCollection geometry = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(geoJsonGeometry);
        GeometryUtils.union(geometry);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(STATUS_PROPERTY_NAME, STATUS_CREATED);
        saveCreatedFeature(new GeoJsonFeatureBuilder(geoJsonGeometry).setColor(penSetting.getEvent().getColor()).setProperties(properties).build("-1"));
    }

    private void doEditModePainting(GeoJsonGeometryCollection geoJsonGeometry) {

        GeoJsonGeometryCollection geoJsonGeometryCollection = (GeoJsonGeometryCollection) currentFeature.getGeometry();
        geoJsonGeometryCollection.getGeometries().addAll(geoJsonGeometry.getGeometries());
        GeometryCollection geometryCollection = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(geoJsonGeometryCollection);
        currentFeature.setGeometry(new Geometry2GeoJsonGeometryTransformer().transform(GeometryUtils.union(geometryCollection)));
        refreshLayer();
        currentFeature.setProperty(STATUS_PROPERTY_NAME, STATUS_CREATED);
        saveEditedFeature(currentFeature);
    }

    private void doEvolveModePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(STATUS_PROPERTY_NAME, STATUS_CREATED);
        GeoJsonGeometryCollection geoJsonGeometryCollection = (GeoJsonGeometryCollection) currentFeature.getGeometry();
        geoJsonGeometryCollection.getGeometries().addAll(geoJsonGeometry.getGeometries());
        GeometryCollection geometryCollection = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(geoJsonGeometryCollection);
        Geometry unionGeometry = GeometryUtils.union(geometryCollection);
        GeoJsonFeature geoJsonFeature = buildFeature((GeoJsonGeometryCollection) new Geometry2GeoJsonGeometryTransformer().transform(unionGeometry), properties, currentFeature.getPolygonStyle().getFillColor(), currentFeature.getId());
        addGeometryEvolution(geoJsonFeature);

        GeoJsonFeature intersectedFeature = buildFeature(geoJsonGeometry, properties, currentFeature.getPolygonStyle().getFillColor(), currentFeature.getId());
        GeoJsonIntersectionRemover intersectionRemover = new GeoJsonIntersectionRemover(intersectedFeature, currentFeature.getGeometry());
        intersectionRemover.intersectGeoJsonFeatures();

        for (GeoJsonFeature feature : intersectionRemover.getAddList()) {
            GeoJsonFeature addedFeature = buildFeature((GeoJsonGeometryCollection) feature.getGeometry(), properties, currentFeature.getPolygonStyle().getFillColor(), currentFeature.getId());
            saveEvolvedFeature(addedFeature);
        }

        setCurrentFeature(geoJsonFeature);
    }

    private void addGeometryEvolution(GeoJsonFeature geoJsonFeature) {
        getCorrespondingGeoJsonLayer().addFeature(geoJsonFeature);
        getCorrespondingGeoJsonLayer().removeFeature(currentFeature);
    }

    private GeoJsonFeature buildFeature(GeoJsonGeometryCollection geometry, HashMap<String, String> properties, int color, String id) {
        GeoJsonFeatureBuilder featureBuilder;
        featureBuilder = new GeoJsonFeatureBuilder(geometry);
        featureBuilder.setColor(color);
        featureBuilder.setProperties(properties);
        return featureBuilder.build(id);
    }

    private void doCreateModeErasing(Iterable<GeoJsonFeature> geoJsonFeatures, GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(geoJsonFeatures, geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        removeFeature(geoJsonIntersectionRemover);

        for (GeoJsonFeature geoJsonFeature : geoJsonIntersectionRemover.getRemoveList()) {
            saveEditedFeature(geoJsonFeature);
        }
    }


    private void doEditModeErasing(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Collections.singletonList(currentFeature), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        GeoJsonGeometryCollection newGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<GeoJsonGeometry>());
        if (!geoJsonIntersectionRemover.getAddList().isEmpty()) {
            newGeometryCollection = (GeoJsonGeometryCollection) geoJsonIntersectionRemover.getAddList().get(0).getGeometry();
        }
        removeGeometryEdit(newGeometryCollection);
        for (GeoJsonFeature geoJsonFeature : geoJsonIntersectionRemover.getRemoveList()) {
            saveEditedFeature(geoJsonFeature);
        }
    }

    private void removeGeometryEdit(GeoJsonGeometryCollection newGeometryCollection) {
        this.currentFeature.setGeometry(newGeometryCollection);
        refreshLayer();
    }

    private void doEvolveModeErasing(GeoJsonGeometryCollection geoJsonGeometry) {

        HashMap<String, String> properties = new HashMap<>();
        properties.put(STATUS_PROPERTY_NAME, STATUS_ERASED);
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Collections.singletonList(currentFeature), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();

        removeGeometryEvolution(geoJsonIntersectionRemover);


        for (GeoJsonFeature geoJsonFeature : geoJsonIntersectionRemover.getRemoveList()) {
            saveEvolvedFeature(geoJsonFeature);
        }
    }


    private void saveCreatedFeature(final GeoJsonFeature geoJsonFeature) {
        FeaturesRestApi featuresRestApi = RestServiceGenerator.createService(FeaturesRestApi.class, determineToken());
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(geoJsonFeature);
        featuresRestApi.saveFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                GeoJsonFeature newGeoJsonFeature = createNewGeoJsonFeatureWithCorrectID(o, geoJsonFeature);
                getCorrespondingGeoJsonLayer().addFeature(newGeoJsonFeature);
                Toast.makeText(getContext(), "Feature is saved", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not save feature", error);
                Toast.makeText(getContext(), "Feature is not saved", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveEditedFeature(GeoJsonFeature geoJsonFeature) {
        FeaturesRestApi featuresRestApi = RestServiceGenerator.createService(FeaturesRestApi.class, determineToken());
        geoJsonFeature.setProperty("PenAction", "Created");
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(geoJsonFeature);
        featuresRestApi.updateFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                Toast.makeText(getContext(), "Feature is edited", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not save feature", error);
                Toast.makeText(getContext(), "Feature is not edited", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveEvolvedFeature(GeoJsonFeature geoJsonFeature) {
        FeaturesRestApi featuresRestApi = RestServiceGenerator.createService(FeaturesRestApi.class, determineToken());
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(geoJsonFeature);
        featuresRestApi.saveFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                Toast.makeText(getContext(), "Feature is evolved", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not evolve feature", error);
                Toast.makeText(getContext(), "Feature is not evolved", Toast.LENGTH_LONG).show();
            }
        });

    }

    private GeoJsonFeature createNewGeoJsonFeatureWithCorrectID(Object o, GeoJsonFeature geoJsonFeature) {
        GeoJsonFeature geoJsonFeatureNew = new GeoJsonFeature(geoJsonFeature.getGeometry(), o.toString(), new HashMap<String, String>(), null);
        geoJsonFeatureNew.setPolygonStyle(geoJsonFeature.getPolygonStyle());
        geoJsonFeatureNew.setPointStyle(geoJsonFeature.getPointStyle());
        geoJsonFeatureNew.setLineStringStyle(geoJsonFeature.getLineStringStyle());
        event2GeoJsonLayer.get(penSetting.getEvent()).removeFeature(geoJsonFeature);
        event2GeoJsonLayer.get(penSetting.getEvent()).addFeature(geoJsonFeatureNew);
        return geoJsonFeatureNew;
    }

    private JSONObject loadFeatures(Event event) {
        try {
            return new GetFeaturesTask(event, determineToken()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return event2GeoJsonLayer.get(penSetting.getEvent());
    }

    private void removeGeometryEvolution(GeoJsonIntersectionRemover geoJsonIntersectionRemover) {
        removeFeaturesFromLayer(Collections.singletonList(currentFeature));
        addFeatureToLayer(geoJsonIntersectionRemover);
        refreshLayer();
    }


    private void removeFeature(GeoJsonIntersectionRemover geoJsonIntersectionRemover) {
        removeFeaturesFromLayer(geoJsonIntersectionRemover.getPrevList());
        addFeatureToLayer(geoJsonIntersectionRemover);
        refreshLayer();
    }

    private void removeFeaturesFromLayer(List<GeoJsonFeature> geoJsonFeatures) {
        for (GeoJsonFeature feature : geoJsonFeatures) {
            getCorrespondingGeoJsonLayer().removeFeature(feature);
        }
    }

    private void addFeatureToLayer(GeoJsonIntersectionRemover geoJsonIntersectionRemover) {
        for (GeoJsonFeature feature : geoJsonIntersectionRemover.getAddList()) {
            getCorrespondingGeoJsonLayer().addFeature(feature);
        }
    }

    private void refreshLayer() {
        getCorrespondingGeoJsonLayer().removeLayerFromMap();
        getCorrespondingGeoJsonLayer().addLayerToMap();
    }

    private class GetFeaturesTask extends AsyncTask<Void, Void, JSONObject> {

        private final Event event;
        private final String token;

        public GetFeaturesTask(Event event, String token) {
            this.event = event;
            this.token = token;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            return RestServiceGenerator.createService(FeaturesRestApi.class, token).getFeatures(event.getId());
        }
    }

    private String determineToken() {
        return (String) ((RadarActivity) getContext()).getIntent().getStringExtra("token");
    }
}