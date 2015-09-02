package at.jku.cis.radar.layout;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


public class GoogleView extends FrameLayout implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private List<PolylineOptions> polyLines = new ArrayList<>();
    private PolylineOptions line = null;

    public GoogleView(Context context) {
        super(context);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
        LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
        if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                line = new PolylineOptions();
            }
            line.add(currentLatLng);
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                googleMap.addPolyline(line);
                polyLines.add(line);
            }
        }
        return true;
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
                        //double factorY = prevEraser.x*(prev.y-currentPoint.y)-prevEraser.y*(prev.x-currentPoint.x)+prev.y*()
                    }
                    prevEraser = currentEraserPoint;

                }
                prev = currentPoint;
            }
        }
        return false;
    }
}
