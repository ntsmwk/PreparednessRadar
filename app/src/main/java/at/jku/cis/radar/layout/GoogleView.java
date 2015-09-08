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
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.concurrent.CopyOnWriteArrayList;

import at.jku.cis.radar.fragment.SelectableTreeFragment;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;


public class GoogleView extends MapView implements OnMapReadyCallback, SelectableTreeFragment.EventClickListener {
    private GoogleMap googleMap;

    private PenSetting penSetting = new PenSetting();

    private CopyOnWriteArrayList<PolylineOptions> polyLines = new CopyOnWriteArrayList<>();
    private PolylineOptions line = null;
    private PolylineOptions eraserLine = null;

    WindowManager wm;

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }


    public PenSetting getPenSetting() {
        return penSetting;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);

    }

    @Override
    public void handleEventClick(Event event) {
        penSetting.setColor(event.getColor());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (googleMap != null) {
            Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
            LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
            if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                if (PenMode.ERASING == penSetting.getPenMode()) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        eraserLine = new PolylineOptions();
                    }
                    eraserLine.add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        calculateLineIntersection(eraserLine);
                    }
                } else {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        line = new PolylineOptions().color(penSetting.getColor());
                    }
                    line.add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        googleMap.addPolyline(line);
                        polyLines.add(line);
                    }
                }
            } else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }


    private boolean calculateLineIntersection(PolylineOptions eraserLine) {
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
}
