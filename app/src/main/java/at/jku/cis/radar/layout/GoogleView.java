package at.jku.cis.radar.layout;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


public class GoogleView extends MapView {
    private GoogleMap googleMap;
    private List<PolylineOptions> polyLines = new ArrayList<>();
    private PolylineOptions line = null;
    private PolylineOptions eraserLine = null;
    private boolean isErasing = false;

    public GoogleView(Context context) {
        super(context);
    }

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoogleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        eraserLine = new PolylineOptions();
                    }
                    eraserLine.add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                       lineIntersected(eraserLine);
                    }
                } else {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        line = new PolylineOptions();
                    }
                    line.add(currentLatLng);
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        googleMap.addPolyline(line);
                        polyLines.add(line);
                    }
                }
            }else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    public void setEraser() {
        isErasing = !isErasing;
    }

    private boolean lineIntersected(PolylineOptions eraserLine) {
        lineLoop:
        for (PolylineOptions line : polyLines) {
            Point prev = null;
            Point prevEraser = null;
            for (LatLng latLng : line.getPoints()) {
                Point currentPoint = googleMap.getProjection().toScreenLocation(latLng);
                for (LatLng eraserLatLng : eraserLine.getPoints()) {
                    Point currentEraserPoint = googleMap.getProjection().toScreenLocation(eraserLatLng);
                    if (prev != null && prevEraser != null) {
                        //Statement not finished!!!

                        int differenceXLine = prev.x - currentPoint.x;
                        int differenceYLine = prev.y - currentPoint.y;
                        int differenceXEraser = prevEraser.x - currentEraserPoint.x;
                        int differenceYEraser = prevEraser.y - currentEraserPoint.y;
                        int dividend = prevEraser.x * differenceYLine - prevEraser.y * differenceXLine + prev.y * differenceXLine - prev.x * differenceYLine;

                        int divisor = differenceYEraser * (differenceXLine) - differenceXEraser * differenceYLine;
                        double factorY = dividend/ divisor;

                    }
                    prevEraser = currentEraserPoint;

                }
                prev = currentPoint;
            }
        }
        return false;
    }
}
