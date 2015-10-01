package at.jku.cis.radar.view;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
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

import at.jku.cis.radar.command.AddFeatureCommand;
import at.jku.cis.radar.command.AddGeometryEditCommand;
import at.jku.cis.radar.command.RemoveFeatureCommand;
import at.jku.cis.radar.command.RemoveGeometryEditCommand;
import at.jku.cis.radar.geometry.GeometryUtils;
import at.jku.cis.radar.model.ApplicationMode;
import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;
import at.jku.cis.radar.service.GeoJsonFeatureBuilder;
import at.jku.cis.radar.service.GeoJsonGeometryBuilder;
import at.jku.cis.radar.service.GeoJsonIntersectionRemover;
import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;


public class GoogleView extends MapView implements OnMapReadyCallback, EventTreeFragment.EventClickListener {
    private GoogleMap googleMap;

    private boolean paintingEnabled = false;
    private PenSetting penSetting = new PenSetting();
    private ApplicationMode applicationMode = ApplicationMode.PAINTING;
    private GeoJsonFeature currentEditingFeature;

    private GeoJsonGeometryBuilder geoJsonGeometryBuilder;

    private Map<String, GeoJsonLayer> geoJsonLayers = new HashMap<>();

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
    public void handleEventVisibleChanged(Event event) {
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEventName(event.getName());
        if (event.isVisible()) {
            geoJsonLayer.addLayerToMap();
        } else {
            geoJsonLayer.removeLayerFromMap();
        }
    }

    @Override
    public void handleEventSelectionChanged(Event event) {
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEventName(event.getName());
        if (event.isSelected()) {
            geoJsonLayer.addLayerToMap();
        }
        paintingEnabled = event.isSelected();
        penSetting.setColor(event.getColor());
        penSetting.setPaintingEvent(event.getName());
    }

    private GeoJsonLayer findGeoJsonLayerByEventName(String eventName) {
        GeoJsonLayer geoJsonLayer = geoJsonLayers.get(eventName);
        if (geoJsonLayer == null) {
            geoJsonLayer = new GeoJsonLayer(googleMap, new JSONObject());
            geoJsonLayers.put(eventName, geoJsonLayer);
        }
        return geoJsonLayer;
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent motionEvent) {
        if (paintingEnabled && googleMap != null && penSetting.getPaintingEvent() != null) {
            if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                dispatchStylusTouchEvent(motionEvent);
            } else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    private void dispatchStylusTouchEvent(MotionEvent motionEvent) {
        Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
        LatLng latLng = googleMap.getProjection().fromScreenLocation(currentPosition);
        if (ApplicationMode.PAINTING == applicationMode) {
            if (PenMode.ERASING == penSetting.getPenMode()) {
                doErasing(motionEvent, latLng);
            } else {
                doPainting(motionEvent, latLng);
            }
        } else {
            if (editFeatureSelected()) {
                if (PenMode.ERASING == penSetting.getPenMode()) {
                    doErasing(motionEvent, currentLatLng);
                } else {
                    doPainting(motionEvent, currentLatLng);
                }
            } else {
                selectEditableFeature(motionEvent, currentLatLng);
            }
        }
    }

    private boolean editFeatureSelected() {
        return this.currentEditingFeature != null;
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
                getSetEditableFeature(featureList.get(0), geoJsonLayer);
            } else {
                Toast.makeText(getContext(), "Can only edit one Event. Please click on the specific area without other overlapping events.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getSetEditableFeature(GeoJsonFeature feature, GeoJsonLayer geoJsonLayer) {
        this.currentEditingFeature = feature;
        GeometryUtils.setEditableFeature(feature);
    }

    private void doPainting(@NonNull MotionEvent motionEvent, LatLng latLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(penSetting.getDrawType());
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonGeometryCollection geoJsonGeometry = geoJsonGeometryBuilder.build();
            if (ApplicationMode.PAINTING == applicationMode) {
                GeoJsonFeature geoJsonFeature = new GeoJsonFeatureBuilder(geoJsonGeometry).setColor(penSetting.getColor()).build();
                new AddFeatureCommand(geoJsonFeature, getCorrespondingGeoJsonLayer()).doCommand();
            } else {
                new AddGeometryEditCommand(geoJsonGeometry, getCorrespondingGeoJsonLayer(), currentEditingFeature).doCommand();
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
            } else {
                doEditModeErasing(geoJsonGeometry);
            }
        }
    }

    private void doEditModeErasing(GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Arrays.asList(currentEditingFeature), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        GeoJsonGeometryCollection newGeometryCollection = (GeoJsonGeometryCollection) geoJsonIntersectionRemover.getAddList().get(0).getGeometry();
        new RemoveGeometryEditCommand(getCorrespondingGeoJsonLayer(), currentEditingFeature, newGeometryCollection).doCommand();
    }

    private void doCreateModeErasing(GeoJsonLayer geoJsonLayer, GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(geoJsonLayer.getFeatures(), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        new RemoveFeatureCommand(getCorrespondingGeoJsonLayer(), geoJsonIntersectionRemover.getAddList(), geoJsonIntersectionRemover.getRemoveList()).doCommand();
    }

    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return geoJsonLayers.get(penSetting.getPaintingEvent());
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
}
