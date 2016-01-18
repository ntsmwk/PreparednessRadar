package at.jku.cis.radar.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
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
import at.jku.cis.radar.command.AddFeatureCommand;
import at.jku.cis.radar.command.AddGeometryEditCommand;
import at.jku.cis.radar.command.AddGeometryEvolveCommand;
import at.jku.cis.radar.command.RemoveFeatureCommand;
import at.jku.cis.radar.command.RemoveGeometryEditCommand;
import at.jku.cis.radar.command.RemoveGeometryEvolveCommand;
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
import at.jku.cis.radar.task.GetFeaturesTask;
import at.jku.cis.radar.transformer.GeoJsonFeature2JsonObjectTransformer;
import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;
import at.jku.cis.radar.transformer.Geometry2GeoJsonGeometryTransformer;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class GoogleView extends MapView implements OnMapReadyCallback, EventTreeFragment.EventClickListener, GoogleMap.OnMapLongClickListener {
    private final String TAG = "GoogleView";

    private GoogleMap googleMap;
    private boolean paintingEnabled = false;
    private PenSetting penSetting = new PenSetting();
    private ApplicationMode applicationMode = ApplicationMode.CREATING;

    private GeoJsonFeature currentFeature;
    private GeoJsonGeometryBuilder geoJsonGeometryBuilder;
    private Map<Event, GeoJsonLayer> event2GeoJsonLayer = new HashMap<>();

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
        if (currentFeature == null) {
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
        menu.add(NO_ID, 0, 0, R.string.create);
        menu.add(NO_ID, 1, 0, R.string.edit);
        menu.add(NO_ID, 2, 0, R.string.evolve);
        menu.add(NO_ID, 3, 0, R.string.evolution);
        super.onCreateContextMenu(menu);
    }

    public void onConextItemSelected(MenuItem item) {
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
                setCurrentFeature(null);
                GoogleView.this.applicationMode = ApplicationMode.CREATING;
                Intent intent = new Intent(getContext(), EvolutionActivity.class);
                intent.putExtra("event", penSetting.getEvent());
                intent.putExtra("featureId", currentFeature.getId());
                getActivity().startActivity(intent);
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
        LatLng latLng = googleMap.getProjection().fromScreenLocation(point);

        if (PenMode.ERASING == penSetting.getPenMode()) {
            doErasing(motionEvent.getAction(), latLng);
        } else {
            doPainting(motionEvent.getAction(), latLng);
        }
    }

    private void doPainting(int action, LatLng latLng) {
        if (MotionEvent.ACTION_DOWN == action) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(penSetting.getDrawType());
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
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

    private void doErasing(int action, LatLng latLng) {
        if (MotionEvent.ACTION_DOWN == action) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(DrawType.POLYGON);
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
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
        saveCreatedFeature(new GeoJsonFeatureBuilder(geoJsonGeometry).setColor(penSetting.getEvent().getColor()).build("-1"));
    }

    private void doEditModePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        new AddGeometryEditCommand(geoJsonGeometry, getCorrespondingGeoJsonLayer(), currentFeature).doCommand();
        saveEditedFeature(currentFeature);
    }

    private void doEvolveModePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonGeometryCollection geoJsonGeometryCollection = (GeoJsonGeometryCollection) currentFeature.getGeometry();
        geoJsonGeometryCollection.getGeometries().add(geoJsonGeometry);
        GeometryCollection geometryCollection = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(geoJsonGeometryCollection);
        GeoJsonFeatureBuilder featureBuilder = new GeoJsonFeatureBuilder((GeoJsonGeometryCollection) new Geometry2GeoJsonGeometryTransformer().transform(GeometryUtils.union(geometryCollection)));
        featureBuilder.setColor(currentFeature.getPolygonStyle().getFillColor());
        GeoJsonFeature geoJsonFeature = featureBuilder.build(currentFeature.getId());

        new AddGeometryEvolveCommand(geoJsonFeature, currentFeature, getCorrespondingGeoJsonLayer()).doCommand();
        saveEvolvedFeature(geoJsonFeature);
        setCurrentFeature(geoJsonFeature);
    }

    private void doCreateModeErasing(Iterable<GeoJsonFeature> geoJsonFeatures, GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(geoJsonFeatures, geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        new RemoveFeatureCommand(getCorrespondingGeoJsonLayer(), geoJsonIntersectionRemover.getAddList(), geoJsonIntersectionRemover.getRemoveList()).doCommand();
        for (GeoJsonFeature geoJsonFeature : geoJsonIntersectionRemover.getAddList()) {
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
        new RemoveGeometryEditCommand(getCorrespondingGeoJsonLayer(), currentFeature, newGeometryCollection).doCommand();
        saveEditedFeature(currentFeature);
    }

    private void doEvolveModeErasing(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Collections.singletonList(currentFeature), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        new RemoveGeometryEvolveCommand(getCorrespondingGeoJsonLayer(), geoJsonIntersectionRemover.getAddList(), geoJsonIntersectionRemover.getRemoveList()).doCommand();
        for (GeoJsonFeature geoJsonFeature : geoJsonIntersectionRemover.getAddList()) {
            saveEvolvedFeature(geoJsonFeature);
        }
    }

    private void saveCreatedFeature(final GeoJsonFeature geoJsonFeature) {
        FeaturesRestApi featuresRestApi = RestServiceGenerator.createService(FeaturesRestApi.class);
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(geoJsonFeature);
        featuresRestApi.saveFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                GeoJsonFeature newGeoJsonFeature = createNewGeoJsonFeatureWithCorrectID(o, geoJsonFeature);
                new AddFeatureCommand(newGeoJsonFeature, getCorrespondingGeoJsonLayer()).doCommand();
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
        FeaturesRestApi featuresRestApi = RestServiceGenerator.createService(FeaturesRestApi.class);
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
        FeaturesRestApi featuresRestApi = RestServiceGenerator.createService(FeaturesRestApi.class);
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
            return new GetFeaturesTask().execute(event.getId()).get();
        } catch (InterruptedException | ExecutionException e) {
            return new JSONObject();
        }
    }

    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return event2GeoJsonLayer.get(penSetting.getEvent());
    }

    private RadarActivity getActivity() {
        return (RadarActivity) getContext();
    }
}