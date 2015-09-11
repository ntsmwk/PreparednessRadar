package at.jku.cis.radar.layout;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;

import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.ParserConfigurationException;

import at.jku.cis.radar.fragment.SelectableTreeFragment;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;
import at.jku.cis.radar.service.EventDOMParser;


public class GoogleView extends MapView implements OnMapReadyCallback, SelectableTreeFragment.EventClickListener {
    private GoogleMap googleMap;

    private PenSetting penSetting = new PenSetting();
    private HashMap<String, GeoJsonLayer> geoJsonLayerHashMap = new HashMap<>();
    private GeoJsonLineString line = null;
    private GeoJsonLineString eraserLine = null;
    private static final String EVENT_TREE_XML = "eventTree.xml";
    private boolean paintingEnabled = true;

    WindowManager wm;

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initializeGeoJsonLayerMap();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        initializeGeoJsonLayerMap();
    }

    @Override
    public void handleEventVisibleChanged(Event event) {
        GeoJsonLayer geoJsonLayer = geoJsonLayerHashMap.get(event.getName());
        if (event.isVisible()) {
            geoJsonLayer.addLayerToMap();
        } else {
            geoJsonLayer.removeLayerFromMap();
        }
    }

    @Override
    public void handleEventSelectionChanged(Event event) {
        paintingEnabled = event.isSelected();
        penSetting.setColor(event.getColor());
        penSetting.setPaintingEvent(event.getName());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (googleMap != null && penSetting.getPaintingEvent() != null && paintingEnabled) {
            Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
            LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
            if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                if (PenMode.ERASING == penSetting.getPenMode()) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        eraserLine = new GeoJsonLineString(new CopyOnWriteArrayList<LatLng>());
                    }
                    eraserLine.getCoordinates().add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        //TODO excange when selection of additional Events on the Sidebar is available
                        calculateLineIntersection(eraserLine);
                    }
                } else {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        line = new GeoJsonLineString(new CopyOnWriteArrayList<LatLng>());
                    }
                    line.getCoordinates().add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        addToGeoJsonLayer(line, getCorrespondingGeoJsonLayer());
                    }
                }
            } else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    private void addToGeoJsonLayer(GeoJsonGeometry geometry, GeoJsonLayer geoJsonLayer) {
        GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();
        lineStringStyle.setColor(penSetting.getColor());
        GeoJsonFeature feature = new GeoJsonFeature(geometry, "id", null, null);
        feature.setLineStringStyle(lineStringStyle);
        geoJsonLayer.addFeature(feature);
    }

    private void initializeGeoJsonLayerMap() {
        List<Event> eventList = null;
        try {
            eventList = new EventDOMParser().processXML(getContext().getAssets().open(EVENT_TREE_XML));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
        addSubeventsToGeoJsonLayerMap(eventList);
    }

    private void addSubeventsToGeoJsonLayerMap(List<Event> eventList) {
        GeoJsonLayer geoJsonLayer;
        for (Event event : eventList) {
            //TODO init layer from DB
            geoJsonLayer = new GeoJsonLayer(googleMap, new JSONObject());
            geoJsonLayerHashMap.put(event.getName(), geoJsonLayer);
            geoJsonLayer.addLayerToMap();
            if (event.getEvents() != null) {
                addSubeventsToGeoJsonLayerMap(event.getEvents());
            }
        }
    }

    private GeoJsonLayer getCorrespondingGeoJsonLayer() {
        return geoJsonLayerHashMap.get(penSetting.getPaintingEvent());
    }

    private boolean calculateLineIntersection(GeoJsonLineString eraserLine) {
        Projection projection = googleMap.getProjection();
        for (GeoJsonLayer geoJsonLayer : geoJsonLayerHashMap.values()) {
            if(!geoJsonLayer.isLayerOnMap()){
                continue;
            }
            ArrayList<GeoJsonFeature> removeList = new ArrayList<>();
            lineLoop:
            for (GeoJsonFeature feature : geoJsonLayer.getFeatures()) {
                Point prev = null;
                for (LatLng latLng : ((GeoJsonLineString) feature.getGeometry()).getCoordinates()) {
                    Point currentPoint = projection.toScreenLocation(latLng);
                    if (!pointInScreen(currentPoint)) {
                        continue;
                    }
                    Point prevEraser = null;
                    for (LatLng eraserLatLng : eraserLine.getCoordinates()) {
                        Point currentEraserPoint = projection.toScreenLocation(eraserLatLng);
                        if (prev != null && prevEraser != null) {
                            if (calculateIntersectionPoint(prev, currentPoint, prevEraser, currentEraserPoint)) {
                                removeList.add(feature);
                                continue lineLoop;
                            }
                        }
                        prevEraser = currentEraserPoint;
                    }
                    prev = currentPoint;
                }
            }
            for(GeoJsonFeature feature : removeList){
                geoJsonLayer.removeFeature(feature);
            }
        }
        return true;
    }

    private boolean calculateIntersectionPoint(Point prev, Point currentPoint, Point prevEraser, Point currentEraserPoint) {
        int differenceXLine = prev.x - currentPoint.x;
        int differenceYLine = prev.y - currentPoint.y;
        int differenceXEraser = prevEraser.x - currentEraserPoint.x;
        int differenceYEraser = prevEraser.y - currentEraserPoint.y;
        int dividend = currentEraserPoint.x * differenceYLine - currentEraserPoint.y * differenceXLine + currentPoint.y * differenceXLine - currentPoint.x * differenceYLine;
        int divisor = differenceYEraser * (differenceXLine) - differenceXEraser * differenceYLine;
        if (divisor == 0) {
            return false;
        }
        double factorY = (double) dividend / (double) divisor;

        double eraserLineEquation = currentEraserPoint.x + differenceXEraser * factorY;
        double factorX = (eraserLineEquation - currentPoint.x) / differenceXLine;
        return (checkFactor(factorX) && checkFactor(factorY));
    }

    public void handleNewLocation(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private boolean pointInScreen(Point p) {
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        if (p.x < 0 || p.y < 0) {
            return false;
        }
        if (p.x > size.x || p.y > size.y) {
            return false;
        }
        return true;
    }

    private boolean checkFactor(double factor) {
        return (factor <= 1.0) && (factor >= 0.0);
    }


    public PenSetting getPenSetting() {
        return penSetting;
    }
}
