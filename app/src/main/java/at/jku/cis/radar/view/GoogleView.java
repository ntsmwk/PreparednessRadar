package at.jku.cis.radar.view;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
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


public class GoogleView extends MapView implements OnMapReadyCallback, EventTreeFragment.EventClickListener {
    private final String TAG = "GoogleView";

    private GoogleMap googleMap;
    private boolean paintingEnabled = false;
    private PenSetting penSetting = new PenSetting();
    private ApplicationMode applicationMode = ApplicationMode.PAINTING;

    private GeoJsonFeature currentEditingFeature;

    private GeoJsonGeometryBuilder geoJsonGeometryBuilder;
    private Map<Event, GeoJsonLayer> geoJsonLayers = new HashMap<>();


    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    public ApplicationMode getApplicationMode() {
        return applicationMode;
    }

    public void setApplicationMode(ApplicationMode applicationMode) {
        this.applicationMode = applicationMode;
    }

    public PenSetting getPenSetting() {
        return penSetting;
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
        penSetting.setColor(event.getColor());
        penSetting.setEvent(event);
    }

    private GeoJsonLayer findGeoJsonLayerByEvent(Event event) {
        loadAllFeatures(event);
        GeoJsonLayer geoJsonLayer = geoJsonLayers.get(event);
        return geoJsonLayer;
    }


    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent motionEvent) {
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
        Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
        LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);

        if ((ApplicationMode.EDITING == applicationMode || ApplicationMode.EVOLVING == applicationMode) && this.currentEditingFeature == null) {
            selectEditableFeature(motionEvent, currentLatLng);
        } else {
            if (PenMode.ERASING == penSetting.getPenMode()) {
                doErasing(motionEvent, currentLatLng);
            } else {
                doPainting(motionEvent, currentLatLng);
            }
        }
    }

    private void selectEditableFeature(@NonNull MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonPoint editPoint = new GeoJsonPoint(currentLatLng);
            setEditFeatureOnMap(editPoint);
        }
    }

    private void setEditFeatureOnMap(GeoJsonPoint editPoint) {
        Geometry editGeometry = new GeoJsonGeometry2GeometryTransformer().transform(editPoint);
        List<GeoJsonFeature> featureList = new ArrayList<>();
        if (this.currentEditingFeature != null) {
            GeometryUtils.setNotEditableFeature(this.currentEditingFeature);
        }
        for (GeoJsonLayer geoJsonLayer : this.geoJsonLayers.values()) {
            if (!geoJsonLayer.isLayerOnMap()) {
                continue;
            }
            for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
                GeometryCollection geometryCollection = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(feature.getGeometry());
                if (GeometryUtils.intersects(geometryCollection, editGeometry)) {
                    featureList.add(feature);
                }
            }
            if (featureList.size() == 1) {
                setEditableFeature(featureList.get(0));
            } else {
                Toast.makeText(getContext(), "Can only edit one Event. Please click on the specific area without other overlapping events.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doPainting(@NonNull MotionEvent motionEvent, LatLng latLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(penSetting.getDrawType());
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonGeometryCollection geoJsonGeometry = geoJsonGeometryBuilder.build();

            if (ApplicationMode.PAINTING == applicationMode) {
                doCreateModePainting(geoJsonGeometry);
            } else if (ApplicationMode.EDITING == applicationMode) {
                doEditModePainting(geoJsonGeometry);
            } else {
                doEvolveModePainting(geoJsonGeometry);
            }
        }
    }


    private void doErasing(@NonNull MotionEvent motionEvent, LatLng latLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(DrawType.POLYGON);
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonLayer geoJsonLayer = getCorrespondingGeoJsonLayer();
            GeoJsonGeometryCollection geoJsonGeometry = geoJsonGeometryBuilder.build();
            if (ApplicationMode.PAINTING == applicationMode) {
                doCreateModeErasing(geoJsonLayer, geoJsonGeometry);
            } else if (ApplicationMode.EDITING == applicationMode) {
                doEditModeErasing(geoJsonGeometry);
            } else {
                doEvolveModeErasing(geoJsonGeometry);
            }
        }
    }

    private void doCreateModePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonFeature geoJsonFeature = new GeoJsonFeatureBuilder(geoJsonGeometry).setColor(penSetting.getColor()).build("-1");
        saveCreatedFeature(geoJsonFeature);
    }


    private void doEditModePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        new AddGeometryEditCommand(geoJsonGeometry, getCorrespondingGeoJsonLayer(), currentEditingFeature).doCommand();
        saveEditedFeature(currentEditingFeature);
    }


    private void doEvolveModePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonGeometryCollection geoJsonGeometryCollection = (GeoJsonGeometryCollection) currentEditingFeature.getGeometry();
        geoJsonGeometryCollection.getGeometries().add(geoJsonGeometry);
        GeometryCollection geometryCollection = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(geoJsonGeometryCollection);
        GeoJsonFeatureBuilder featureBuilder = new GeoJsonFeatureBuilder((GeoJsonGeometryCollection) new Geometry2GeoJsonGeometryTransformer().transform(GeometryUtils.union(geometryCollection)));
        featureBuilder.setColor(currentEditingFeature.getPolygonStyle().getFillColor());
        GeoJsonFeature geoJsonFeature = featureBuilder.build(currentEditingFeature.getId());

        new AddGeometryEvolveCommand(geoJsonFeature, currentEditingFeature, getCorrespondingGeoJsonLayer()).doCommand();
        saveEvolvedFeature(geoJsonFeature);
        setCurrentEditingFeature(geoJsonFeature);
        GeometryUtils.setNotEditableFeature(geoJsonFeature);
    }


    private void doCreateModeErasing(GeoJsonLayer geoJsonLayer, GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(geoJsonLayer.getFeatures(), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        new RemoveFeatureCommand(getCorrespondingGeoJsonLayer(), geoJsonIntersectionRemover.getAddList(), geoJsonIntersectionRemover.getRemoveList()).doCommand();
        for (GeoJsonFeature geoJsonFeature : geoJsonIntersectionRemover.getAddList()) {
            saveEditedFeature(geoJsonFeature);
        }
    }

    private void doEditModeErasing(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Collections.singletonList(currentEditingFeature), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        GeoJsonGeometryCollection newGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<GeoJsonGeometry>());
        if (!geoJsonIntersectionRemover.getAddList().isEmpty()) {
            newGeometryCollection = (GeoJsonGeometryCollection) geoJsonIntersectionRemover.getAddList().get(0).getGeometry();
        }
        new RemoveGeometryEditCommand(getCorrespondingGeoJsonLayer(), currentEditingFeature, newGeometryCollection).doCommand();
        saveEditedFeature(currentEditingFeature);
    }

    private void doEvolveModeErasing(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Collections.singletonList(currentEditingFeature), geoJsonGeometry.getGeometries().get(0));
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

    private void loadAllFeatures(Event event) {
        GeoJsonLayer geoJsonLayer = geoJsonLayers.get(event);
        if (geoJsonLayer == null) {
            JSONObject jsonObject = loadFeatures(event);
            geoJsonLayer = new GeoJsonLayer(googleMap, jsonObject);
            geoJsonLayer.getDefaultPolygonStyle().setFillColor(event.getColor());
            geoJsonLayer.getDefaultPolygonStyle().setStrokeColor(event.getColor());
            geoJsonLayer.getDefaultLineStringStyle().setColor(event.getColor());
            geoJsonLayers.put(event, geoJsonLayer);
        }
    }

    private JSONObject loadFeatures(Event event) {
        try {
            return new GetFeaturesTask().execute(event.getId()).get();
        } catch (InterruptedException | ExecutionException e) {
            return new JSONObject();
        }
    }

    private void setEditableFeature(GeoJsonFeature feature) {
        this.currentEditingFeature = feature;
        GeometryUtils.setEditableFeature(feature);
    }


    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return geoJsonLayers.get(penSetting.getEvent());
    }


    public GeoJsonFeature getCurrentEditingFeature() {
        return currentEditingFeature;
    }

    public void setCurrentEditingFeature(GeoJsonFeature currentEditingFeature) {
        this.currentEditingFeature = currentEditingFeature;
    }

    public void handleNewLocation(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private GeoJsonFeature createNewGeoJsonFeatureWithCorrectID(Object o, GeoJsonFeature geoJsonFeature) {
        GeoJsonFeature geoJsonFeatureNew = new GeoJsonFeature(geoJsonFeature.getGeometry(), o.toString(), new HashMap<String, String>(), null);
        geoJsonFeatureNew.setPolygonStyle(geoJsonFeature.getPolygonStyle());
        geoJsonFeatureNew.setPointStyle(geoJsonFeature.getPointStyle());
        geoJsonFeatureNew.setLineStringStyle(geoJsonFeature.getLineStringStyle());
        geoJsonLayers.get(penSetting.getEvent()).removeFeature(geoJsonFeature);
        geoJsonLayers.get(penSetting.getEvent()).addFeature(geoJsonFeatureNew);
        return geoJsonFeatureNew;
    }
}
