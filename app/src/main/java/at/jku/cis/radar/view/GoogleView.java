package at.jku.cis.radar.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.support.annotation.NonNull;
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

import at.jku.cis.radar.activity.EvolutionActivity;
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


public class GoogleView extends MapView implements OnMapReadyCallback, EventTreeFragment.EventClickListener, GoogleMap.OnMapLongClickListener {
    private final String TAG = "GoogleView";

    private GoogleMap googleMap;
    private boolean paintingEnabled = false;
    private PenSetting penSetting = new PenSetting();
    private ApplicationMode applicationMode = ApplicationMode.PAINTING;

    private GeoJsonFeature currentFeature;
    private GeoJsonGeometryBuilder geoJsonGeometryBuilder;
    private Map<Event, GeoJsonLayer> event2GeoJsonLayer = new HashMap<>();


    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        Geometry geometry = new GeoJsonGeometry2GeometryTransformer().transform(new GeoJsonPoint(latLng));
        List<GeoJsonFeature> featureList = new ArrayList<>();
        if (currentFeature != null) {
            GeometryUtils.setNotEditableFeature(this.currentFeature);
        }
        GeoJsonLayer geoJsonLayer = getCorrespondingGeoJsonLayer();
        for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
            GeometryCollection geometryCollection = (GeometryCollection) new GeoJsonGeometry2GeometryTransformer().transform(feature.getGeometry());
            if (GeometryUtils.intersects(geometryCollection, geometry)) {
                featureList.add(feature);
            }
        }
        if (featureList.size() == 1) {
            currentFeature = featureList.get(0);
            showContextMenu();
        } else {
            Toast.makeText(getContext(), "Please click on the specific area without other overlapping events.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        menu.setHeaderTitle("Context Menu");
        menu.add(NO_ID, 0, 0, ApplicationMode.EDITING.name());
        menu.add(NO_ID, 1, 0, ApplicationMode.EVOLVING.name());
        menu.add(NO_ID, 2, 0, "Show Evolution");
        MenuItem.OnMenuItemClickListener contextMenuClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 0:
                        GoogleView.this.applicationMode = ApplicationMode.EDITING;
                        break;
                    case 1:
                        GoogleView.this.applicationMode = ApplicationMode.EVOLVING;
                        break;
                    case 2:
                        Intent intent = new Intent(getContext(), EvolutionActivity.class);
                        intent.putExtra("event", penSetting.getEvent());
                        intent.putExtra("featureId", currentFeature.getId());
                        ((Activity) getContext()).startActivity(intent);
                        break;
                }
                return true;
            }
        };
        menu.getItem(0).setOnMenuItemClickListener(contextMenuClickListener);
        menu.getItem(1).setOnMenuItemClickListener(contextMenuClickListener);
        menu.getItem(2).setOnMenuItemClickListener(contextMenuClickListener);

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

        if ((ApplicationMode.EDITING == applicationMode || ApplicationMode.EVOLVING == applicationMode) && this.currentFeature == null) {
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
        if (this.currentFeature != null) {
            GeometryUtils.setNotEditableFeature(this.currentFeature);
        }
        for (GeoJsonLayer geoJsonLayer : this.event2GeoJsonLayer.values()) {
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

    private JSONObject loadFeatures(Event event) {
        try {
            return new GetFeaturesTask().execute(event.getId()).get();
        } catch (InterruptedException | ExecutionException e) {
            return new JSONObject();
        }
    }

    private void setEditableFeature(GeoJsonFeature feature) {
        this.currentFeature = feature;
        GeometryUtils.setEditableFeature(feature);
    }


    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return event2GeoJsonLayer.get(penSetting.getEvent());
    }


    public GeoJsonFeature getCurrentFeature() {
        return currentFeature;
    }

    public void setCurrentFeature(GeoJsonFeature currentFeature) {
        this.currentFeature = currentFeature;
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


}