package at.jku.cis.radar.layout;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.WindowManager;

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
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;
import com.vividsolutions.jts.geom.Geometry;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import at.jku.cis.radar.convert.GeometryTransformator;
import at.jku.cis.radar.fragment.SelectableTreeFragment;
import at.jku.cis.radar.model.DrawMode;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;


public class GoogleView extends MapView implements OnMapReadyCallback, SelectableTreeFragment.EventClickListener {
    private GoogleMap googleMap;

    private PenSetting penSetting = new PenSetting();
    private Map<String, GeoJsonLayer> geoJsonLayers = new HashMap<>();

    private GeoJsonGeometry geoJsonGeometry = null;
    private GeoJsonLineString eraserLine = null;
    private final int POLYGON_EXTERIOR_RING_INDEX = 0;

    private boolean paintingEnabled = true;

    WindowManager wm;

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
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
            Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
            LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
            if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                if (PenMode.ERASING == penSetting.getPenMode()) {
                    doErasing(motionEvent, currentLatLng);
                } else {
                    doPainting(motionEvent, currentLatLng);
                }
            } else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    private void doPainting(@NonNull MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            createGeometry();
        }
        addLatLngToGeometry(currentLatLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (penSetting.getDrawMode() == DrawMode.MARKER) {
                geoJsonGeometry = new GeoJsonPoint(currentLatLng);
            }
            addToGeoJsonLayer(geoJsonGeometry, getCorrespondingGeoJsonLayer());
        }
    }

    private void addLatLngToGeometry(LatLng currentLatLng) {
        if (penSetting.getDrawMode() == DrawMode.LINE) {
            ((GeoJsonLineString) geoJsonGeometry).getCoordinates().add(currentLatLng);
        } else if (penSetting.getDrawMode() == DrawMode.POLYGON) {
            //TODO check for interior rings....
            ((GeoJsonPolygon) geoJsonGeometry).getCoordinates().get(POLYGON_EXTERIOR_RING_INDEX).add(currentLatLng);
        }
    }

    private void createGeometry() {
        if (penSetting.getDrawMode() == DrawMode.LINE) {
            geoJsonGeometry = new GeoJsonLineString(new CopyOnWriteArrayList<LatLng>());
        } else if (penSetting.getDrawMode() == DrawMode.POLYGON) {
            List<List<LatLng>> coordinates = new ArrayList<>();
            List<LatLng> exteriorRing = new ArrayList<>();
            coordinates.add(exteriorRing);
            geoJsonGeometry = new GeoJsonPolygon(coordinates);
        }
    }

    private void doErasing(@NonNull MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            eraserLine = new GeoJsonLineString(new CopyOnWriteArrayList<LatLng>());
        }
        eraserLine.getCoordinates().add(currentLatLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            removeIntersectedGeometry(eraserLine);
        }
    }

    private void addToGeoJsonLayer(GeoJsonGeometry geometry, GeoJsonLayer geoJsonLayer) {
        //TODO geometry id....
        //TODO properties
        GeoJsonFeature feature = new GeoJsonFeature(geometry, "id", null, null);
        if (geometry instanceof GeoJsonLineString) {
            GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();
            lineStringStyle.setColor(penSetting.getColor());
            feature.setLineStringStyle(lineStringStyle);
        } else if (geometry instanceof GeoJsonPolygon) {
            GeoJsonPolygonStyle polygonStyle = new GeoJsonPolygonStyle();
            polygonStyle.setFillColor(penSetting.getColor());
            feature.setPolygonStyle(polygonStyle);
        } else if (geometry instanceof GeoJsonPoint) {
            float[] hsv = new float[3];
            Color.colorToHSV(penSetting.getColor(), hsv);
            BitmapDescriptor pointIcon = BitmapDescriptorFactory
                    .defaultMarker(hsv[0]);
            GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
            pointStyle.setIcon(pointIcon);
            feature.setPointStyle(pointStyle);
        }
        geoJsonLayer.addFeature(feature);
    }

    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return geoJsonLayers.get(penSetting.getPaintingEvent());
    }

    private void removeIntersectedGeometry(GeoJsonLineString eraserLine) {
        Projection projection = googleMap.getProjection();
        Geometry eraser = GeometryTransformator.transformToGeometry(eraserLine, projection);
        for (GeoJsonLayer geoJsonLayer : this.geoJsonLayers.values()) {
            if (!geoJsonLayer.isLayerOnMap()) {
                continue;
            }
            ArrayList<GeoJsonFeature> removeList = new ArrayList<>();
            for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
                Geometry line = GeometryTransformator.transformToGeometry(feature.getGeometry(), projection);
                if (line.intersects(eraser)) {
                    removeList.add(feature);
                    continue;
                }
            }
            for (GeoJsonFeature feature : removeList) {
                geoJsonLayer.removeFeature(feature);
            }
        }
    }

    public void handleNewLocation(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    public PenSetting getPenSetting() {
        return penSetting;
    }
}
