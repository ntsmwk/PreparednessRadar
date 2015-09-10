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
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;

import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.IOException;
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
    private HashMap<String, CopyOnWriteArrayList<PolylineOptions>> polylineHashMap = new HashMap<>();
    private PolylineOptions line = null;
    private PolylineOptions eraserLine = null;
    private static final String EVENT_TREE_XML = "eventTree.xml";
    private GeoJsonLayer geoJSONLayer;

    WindowManager wm;

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initializePolylineMap();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        initializeGeoJSONLayer();

    }

    @Override
    public void handleEventClick(Event event) {
        penSetting.setColor(event.getColor());
        penSetting.setPaintingEvent(event.getName());
        repaintMap();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (googleMap != null && penSetting.getPaintingEvent() != null) {
            Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
            LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
            if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                if (PenMode.ERASING == penSetting.getPenMode()) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        eraserLine = new PolylineOptions();
                    }
                    eraserLine.add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        //TODO excange when selection of additional Events on the Sidebar is available
                        calculateLineIntersection(eraserLine, getCorrespondingPolylineList());
                    }
                } else {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        line = new PolylineOptions().color(penSetting.getColor());
                    }
                    line.add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        googleMap.addPolyline(line);
                        addLineToMap(line);
                    }
                }
            } else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    private void initializePolylineMap() {
        List<Event> eventList = null;
        try {
            eventList = new EventDOMParser().processXML(getContext().getAssets().open(EVENT_TREE_XML));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
        addSubeventsToPolylineMap(eventList);
    }

    private void initializeGeoJSONLayer() {
        //TODO get JSONObject from postGis DB....
        geoJSONLayer = new GeoJsonLayer(getMap(), new JSONObject());
        geoJSONLayer.addLayerToMap();
    }

    private void addSubeventsToPolylineMap(List<Event> eventList) {
        for (Event event : eventList) {
            polylineHashMap.put(event.getName(), new CopyOnWriteArrayList<PolylineOptions>());
            if (event.getEvents() != null) {
                addSubeventsToPolylineMap(event.getEvents());
            }
        }
    }

    private CopyOnWriteArrayList<PolylineOptions> getCorrespondingPolylineList() {
        return polylineHashMap.get(penSetting.getPaintingEvent());
    }

    private void addLineToMap(PolylineOptions line) {
        getCorrespondingPolylineList().add(line);
    }

    private boolean calculateLineIntersection(PolylineOptions eraserLine, CopyOnWriteArrayList<PolylineOptions> polyLines) {
        Projection projection = googleMap.getProjection();
        lineLoop:
        for (PolylineOptions line : polyLines) {
            Point prev = null;
            for (LatLng latLng : line.getPoints()) {
                Point currentPoint = projection.toScreenLocation(latLng);
                if (!pointInScreen(currentPoint)) {
                    continue;
                }
                Point prevEraser = null;
                for (LatLng eraserLatLng : eraserLine.getPoints()) {
                    Point currentEraserPoint = projection.toScreenLocation(eraserLatLng);
                    if (prev != null && prevEraser != null) {
                        if (calculateIntersectionPoint(prev, currentPoint, prevEraser, currentEraserPoint)) {
                            polyLines.remove(line);
                            continue lineLoop;
                        }
                    }
                    prevEraser = currentEraserPoint;
                }
                prev = currentPoint;
            }
        }
        repaintMap();
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

    private void repaintMap() {
        googleMap.clear();
        //TODO excange when selection of additional Events on the Sidebar is available
        paintPolylinesOnMap(getCorrespondingPolylineList());
    }

    private void paintPolylinesOnMap(CopyOnWriteArrayList<PolylineOptions> polyLines) {
        for (PolylineOptions line : polyLines) {
            googleMap.addPolyline(line);
        }
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
