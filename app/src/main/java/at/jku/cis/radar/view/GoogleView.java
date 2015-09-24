package at.jku.cis.radar.view;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.Geometry;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.jku.cis.radar.R;
import at.jku.cis.radar.command.AddGeometryCommand;
import at.jku.cis.radar.command.RemoveGeometryCommand;
import at.jku.cis.radar.convert.GeometryTransformator;
import at.jku.cis.radar.model.ApplicationMode;
import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;
import at.jku.cis.radar.service.GeoJsonFeatureBuilder;
import at.jku.cis.radar.service.GeoJsonGeometryBuilder;
import at.jku.cis.radar.service.GeoJsonIntersectionRemover;


public class GoogleView extends MapView implements OnMapReadyCallback, EventTreeFragment.EventClickListener {
    private final int POLYGON_EXTERIOR_RING_INDEX = 0;

    private GoogleMap googleMap;

    private boolean paintingEnabled = false;
    private PenSetting penSetting = new PenSetting();
    private ApplicationMode applicationMode = ApplicationMode.PAINTING;

    private GeoJsonGeometryBuilder geoJsonGeometryBuilder;

    private Map<String, GeoJsonLayer> geoJsonLayers = new HashMap<>();
    private HashMap<GeoJsonLayer, GeoJsonFeature> activeEditMarkerMap = new HashMap<>();

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
        if (googleMap != null && penSetting.getPaintingEvent() != null && paintingEnabled) {
            if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                dispatchStylusTouchEvent(motionEvent);
            } else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    private void dispatchStylusTouchEvent(@NonNull MotionEvent motionEvent) {
        Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
        LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
        if (ApplicationMode.PAINTING == applicationMode) {
            if (PenMode.ERASING == penSetting.getPenMode()) {
                doErasing(motionEvent, currentLatLng);
            } else {
                doPainting(motionEvent, currentLatLng);
            }
        } else {
            if (PenMode.ERASING == penSetting.getPenMode()) {
                doErasing(motionEvent, currentLatLng);
            } else {
                doPainting(motionEvent, currentLatLng);
            }
            // doEditing(motionEvent, currentLatLng);
        }
    }

    private void doEditing(@NonNull MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonPoint editPoint = new GeoJsonPoint(currentLatLng);
            setEditPointsOnMap(editPoint);
        }
    }

    private void setEditPointsOnMap(GeoJsonPoint editPoint) {
        Projection projection = googleMap.getProjection();
        Geometry editGeometry = GeometryTransformator.transformToGeometry(editPoint);
        List<GeoJsonFeature> featureList = new ArrayList<>();
        for (GeoJsonLayer geoJsonLayer : this.geoJsonLayers.values()) {
            if (!geoJsonLayer.isLayerOnMap()) {
                continue;
            }
            for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
                Geometry geometry = GeometryTransformator.transformToGeometry(feature.getGeometry());
                if (geometry.intersects(editGeometry)) {
                    featureList.add(feature);
                    continue;
                }
            }
            addEditMarkerForFeatures(featureList, geoJsonLayer);
        }
    }

    private void addEditMarkerForFeatures(List<GeoJsonFeature> featureList, GeoJsonLayer geoJsonLayer) {
        for (GeoJsonFeature feature : featureList) {
            if (feature.getGeometry() instanceof GeoJsonLineString) {
                List<LatLng> latLngList = ((GeoJsonLineString) feature.getGeometry()).getCoordinates();
                setGeometryEditable(latLngList, geoJsonLayer);
            }
            if (feature.getGeometry() instanceof GeoJsonPolygon) {
                List<LatLng> latLngList = ((GeoJsonPolygon) feature.getGeometry()).getCoordinates().get(POLYGON_EXTERIOR_RING_INDEX);
                setGeometryEditable(latLngList, geoJsonLayer);
            }
            if (feature.getGeometry() instanceof GeoJsonPoint) {
                setGeometryEditable(((GeoJsonPoint) feature.getGeometry()).getCoordinates(), geoJsonLayer);
            }
        }
    }

    private void setGeometryEditable(List<LatLng> latLngList, GeoJsonLayer layer) {
        for (LatLng latLng : latLngList) {
            setGeometryEditable(latLng, layer);
        }
    }

    private void setGeometryEditable(LatLng latLng, GeoJsonLayer layer) {
        GeoJsonPointStyle pointStyle = getEditMarkerPointStyle();
        GeoJsonFeature editMarkerFeature = getEditMarkerFeature(pointStyle, latLng);
        activeEditMarkerMap.put(layer, editMarkerFeature);
        layer.addFeature(editMarkerFeature);

    }

    @NonNull
    private GeoJsonFeature getEditMarkerFeature(GeoJsonPointStyle pointStyle, LatLng latLng) {
        GeoJsonPoint editMarker = new GeoJsonPoint(latLng);
        GeoJsonFeature editMarkerFeature = new GeoJsonFeature(editMarker, "editMarker", null, null);
        editMarkerFeature.setPointStyle(pointStyle);
        return editMarkerFeature;
    }

    @NonNull
    private GeoJsonPointStyle getEditMarkerPointStyle() {
        BitmapDescriptor pointIcon = BitmapDescriptorFactory.fromResource(R.drawable.diamond_icon);
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
        pointStyle.setIcon(pointIcon);
        pointStyle.setDraggable(true);
        return pointStyle;
    }

    private void doPainting(@NonNull MotionEvent motionEvent, LatLng latLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(penSetting.getDrawType());
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonGeometry geoJsonGeometry = geoJsonGeometryBuilder.build();
            GeoJsonFeature geoJsonFeature = new GeoJsonFeatureBuilder(geoJsonGeometry).setColor(penSetting.getColor()).build();
            new AddGeometryCommand(geoJsonFeature, getCorrespondingGeoJsonLayer()).doCommand();
        }
    }

    private void doErasing(@NonNull MotionEvent motionEvent, LatLng latLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
           geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(DrawType.POLYGON);
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonLayer geoJsonLayer = getCorrespondingGeoJsonLayer();
            GeoJsonGeometry geoJsonGeometry = geoJsonGeometryBuilder.build();
            GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(geoJsonLayer.getFeatures(), geoJsonGeometry);
            geoJsonIntersectionRemover.removeIntersectedGeometry(googleMap.getProjection());
            new RemoveGeometryCommand(getCorrespondingGeoJsonLayer(), geoJsonIntersectionRemover.getAddList(), geoJsonIntersectionRemover.getRemoveList()).doCommand();
        }
    }

    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return geoJsonLayers.get(penSetting.getPaintingEvent());
    }

    public void handleNewLocation(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }
}
