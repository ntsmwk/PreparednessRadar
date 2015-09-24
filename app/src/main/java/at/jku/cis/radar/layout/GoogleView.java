package at.jku.cis.radar.layout;

import android.content.Context;
import android.graphics.Color;
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
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jts.operation.valid.TopologyValidationError;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import at.jku.cis.radar.R;
import at.jku.cis.radar.convert.GeometryTransformator;
import at.jku.cis.radar.fragment.SelectableTreeFragment;
import at.jku.cis.radar.model.DrawMode;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;


public class GoogleView extends MapView implements OnMapReadyCallback, SelectableTreeFragment.EventClickListener {
    private final int POLYGON_EXTERIOR_RING_INDEX = 0;

    private GoogleMap googleMap;
    private PenSetting penSetting = new PenSetting();
    private Map<String, GeoJsonLayer> geoJsonLayers = new HashMap<>();
    private GeoJsonGeometry geoJsonGeometry = null;
    private GeoJsonPolygon eraserPolygon = null;
    private boolean paintingEnabled = true;
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
                if (PenMode.EDIT == penSetting.getPenMode()) {
                    doEditing(motionEvent, currentLatLng);
                } else if (PenMode.ERASING == penSetting.getPenMode()) {
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

    private void doEditing(@NonNull MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonPoint editPoint = new GeoJsonPoint(currentLatLng);
            setEditPointsOnMap(editPoint);
        }
    }

    private void setEditPointsOnMap(GeoJsonPoint editPoint) {
        Projection projection = googleMap.getProjection();
        Geometry editGeometry = GeometryTransformator.transformToGeometry(editPoint, projection);
        List<GeoJsonFeature> featureList = new ArrayList<>();
        for (GeoJsonLayer geoJsonLayer : this.geoJsonLayers.values()) {
            if (!geoJsonLayer.isLayerOnMap()) {
                continue;
            }
            for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
                Geometry geometry = GeometryTransformator.transformToGeometry(feature.getGeometry(), projection);
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
            List<List<LatLng>> coordinates = new ArrayList<>();
            List<LatLng> exteriorRing = new ArrayList<>();
            coordinates.add(exteriorRing);
            eraserPolygon = new GeoJsonPolygon(coordinates);
        }
        eraserPolygon.getCoordinates().get(POLYGON_EXTERIOR_RING_INDEX).add(currentLatLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            removeIntersectedGeometry(eraserPolygon);
        }
    }

    private void addToGeoJsonLayer(GeoJsonGeometry geometry, GeoJsonLayer geoJsonLayer) {
        //TODO geometry id
        GeoJsonFeature feature = new GeoJsonFeature(geometry, "id", null, null);
        if (geometry instanceof GeoJsonLineString) {
            createNewLineStringFeature(feature);
        } else if (geometry instanceof GeoJsonPolygon) {
            Polygon polygon = (Polygon) GeometryTransformator.transformToGeometry(geometry, googleMap.getProjection());
            if (!polygon.isSimple()) {
                feature = createComplexPolygon(polygon);
            }
            createNewSimplePolygonFeature(feature);
        } else if (geometry instanceof GeoJsonPoint) {
            createNewPointFeature(feature);
        }
        geoJsonLayer.addFeature(feature);
    }

    @NonNull
    private GeoJsonFeature createComplexPolygon(Polygon polygon) {
        List<Polygon> polygons = repair(polygon);
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
        GeoJsonMultiPolygon geoJsonPolygon = (GeoJsonMultiPolygon) GeometryTransformator.transformToGeoJsonGeometry(multiPolygon, googleMap.getProjection());
        GeoJsonFeature feature = new GeoJsonFeature(geoJsonPolygon, "id", null, null);
        return feature;
    }

    @NonNull
    private GeoJsonFeature createComplexMultiPolygon(MultiPolygon polygon) {
        List<Polygon> polygons = repair(polygon);
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
        GeoJsonMultiPolygon geoJsonPolygon = (GeoJsonMultiPolygon) GeometryTransformator.transformToGeoJsonGeometry(multiPolygon, googleMap.getProjection());
        GeoJsonFeature feature = new GeoJsonFeature(geoJsonPolygon, "id", null, null);
        return feature;
    }

    private void createNewPointFeature(GeoJsonFeature feature) {
        float[] hsv = new float[3];
        Color.colorToHSV(penSetting.getColor(), hsv);
        BitmapDescriptor pointIcon = BitmapDescriptorFactory
                .defaultMarker(hsv[0]);
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
        pointStyle.setIcon(pointIcon);
        feature.setPointStyle(pointStyle);
    }

    private void createNewSimplePolygonFeature(GeoJsonFeature feature) {
        GeoJsonPolygonStyle polygonStyle = new GeoJsonPolygonStyle();
        polygonStyle.setFillColor(penSetting.getColor());
        feature.setPolygonStyle(polygonStyle);
    }

    private void createNewLineStringFeature(GeoJsonFeature feature) {
        GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();
        lineStringStyle.setColor(penSetting.getColor());
        feature.setLineStringStyle(lineStringStyle);
    }

    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return geoJsonLayers.get(penSetting.getPaintingEvent());
    }

    private void removeIntersectedGeometry(GeoJsonPolygon eraserPolygon) {
        Projection projection = googleMap.getProjection();
        Geometry eraser = GeometryTransformator.transformToGeometry(eraserPolygon, projection);
        for (GeoJsonLayer geoJsonLayer : this.geoJsonLayers.values()) {
            if (!geoJsonLayer.isLayerOnMap()) {
                continue;
            }
            ArrayList<GeoJsonFeature> removeList = new ArrayList<>();
            ArrayList<GeoJsonFeature> addList = new ArrayList<>();
            for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
                Geometry line = GeometryTransformator.transformToGeometry(feature.getGeometry(), projection);
                GeoJsonFeature intersectionFeature = null;
                if (line.intersects(eraser)) {
                    Geometry intersectionGeometry;
                    try {
                        intersectionGeometry = line.difference(eraser);
                    } catch (TopologyException e) {
                        continue;
                    }
                    if (intersectionGeometry instanceof Polygon) {
                        intersectionFeature = createComplexPolygon((Polygon) intersectionGeometry);
                        createNewSimplePolygonFeature(intersectionFeature);
                        addList.add(intersectionFeature);
                    } else if (intersectionGeometry instanceof MultiPolygon) {
                        intersectionFeature = createComplexMultiPolygon((MultiPolygon) intersectionGeometry);
                        createNewSimplePolygonFeature(intersectionFeature);
                        addList.add(intersectionFeature);
                    }
                    removeList.add(feature);
                    continue;
                }
            }
            for (GeoJsonFeature feature : addList) {
                geoJsonLayer.addFeature(feature);
            }
            for (GeoJsonFeature feature : removeList) {
                geoJsonLayer.removeFeature(feature);
            }
        }
    }

    private List<Polygon> repair(Polygon polygon) {
        TopologyValidationError err = new IsValidOp(polygon).getValidationError();
        if (err != null && TopologyValidationError.SELF_INTERSECTION == err.getErrorType()) {
            Geometry boundary = polygon.getBoundary();
            boundary = boundary.union(boundary);
            Polygonizer polygonizer = new Polygonizer();
            polygonizer.add(boundary);
            List<Polygon> newPolygons = new ArrayList<>();
            for (Object newPolygon : polygonizer.getPolygons()) {
                if (newPolygon != null) {
                    newPolygons.addAll(repair((Polygon) newPolygon));
                }
            }
            return newPolygons;
        }
        return Arrays.asList(polygon);
    }

    private List<Polygon> repair(MultiPolygon multiPolygon) {
        ArrayList<Polygon> polygons = new ArrayList<>();
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            polygons.addAll(repair((Polygon) multiPolygon.getGeometryN(i)));
        }
        return polygons;
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
