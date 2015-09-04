package at.jku.cis.radar.layout;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.concurrent.CopyOnWriteArrayList;


public class GoogleView extends MapView {
    private GoogleMap googleMap;
    private CopyOnWriteArrayList<PolylineOptions> polyLines = new CopyOnWriteArrayList<>();
    private PolylineOptions line = null;
    private PolylineOptions eraserLine = null;
    private boolean isErasing = false;

    WindowManager wm;

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }


    public void setMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (googleMap != null) {
            Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
            LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
            if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                if (isErasing) {
                    //doPainting(motionEvent, currentLatLng);
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

    private void doPainting(MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            line = new PolylineOptions();
        }
        line.add(currentLatLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            googleMap.addPolyline(line);
            polyLines.add(line);
        }
    }

    private void doErasing(MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            eraserLine = new PolylineOptions();
        }
        eraserLine.add(currentLatLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            lineIntersected(eraserLine);
        }
    }

    public void setEraser() {
        isErasing = !isErasing;
    }

    private boolean lineIntersected(PolylineOptions eraserLine) {
        Projection projection = googleMap.getProjection();
        lineLoop:
        for (PolylineOptions line : polyLines) {

            Point prev = null;
            Point prevEraser = null;
            for (LatLng latLng : line.getPoints()) {
                Point currentPoint = projection.toScreenLocation(latLng);
                if (!pointInScreen(currentPoint)) {
                    continue;
                }
                for (LatLng eraserLatLng : eraserLine.getPoints()) {
                    Point currentEraserPoint = projection.toScreenLocation(eraserLatLng);
                    if (prev != null && prevEraser != null) {
                        int differenceXLine = Math.abs(prev.x - currentPoint.x);
                        int differenceYLine = Math.abs(prev.y - currentPoint.y);
                        int differenceXEraser = Math.abs(prevEraser.x - currentEraserPoint.x);
                        int differenceYEraser = Math.abs(prevEraser.y - currentEraserPoint.y);
                        int smallestXEraser = findSmallest(prevEraser.x, currentEraserPoint.x);
                        int smallestYEraser = findSmallest(prevEraser.y, currentEraserPoint.y);
                        int smallestYLine = findSmallest(prev.y, currentPoint.y);
                        int smallestXLine = findSmallest(prev.x, currentPoint.x);
                        int dividend = smallestXEraser * differenceYLine - smallestYEraser * differenceXLine + smallestYLine * differenceXLine - smallestXLine * differenceYLine;
                        int divisor = differenceYEraser * (differenceXLine) - differenceXEraser * differenceYLine;
                        if (divisor == 0) {
                            continue;
                        }
                        double factorY = (double) dividend / (double) divisor;

                        double eraserLineEquation = smallestXEraser + differenceXEraser * factorY;
                        double factorX = (eraserLineEquation - smallestXLine) / differenceXLine;
                        if (checkFactor(factorX) && checkFactor(factorY)) {
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
        return false;
    }

    private int findSmallest(int i, int j) {
        return (i < j) ? i : j;
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
