package at.jku.cis.radar.layout;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.fragment.SelectableTreeFragment;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;


public class GoogleView extends MapView implements OnMapReadyCallback, SelectableTreeFragment.EventClickListener {
    private GoogleMap googleMap;

    private PenSetting penSetting = new PenSetting();

    private List<PolylineOptions> polyLines = new ArrayList<>();
    private PolylineOptions line = null;
    private PolylineOptions eraserLine = null;

    public GoogleView(Context context) {
        super(context);
    }

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoogleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
                       lineIntersected(eraserLine);
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
            }else {
                super.dispatchTouchEvent(motionEvent);
            }
        }
        return true;
    }

    private boolean lineIntersected(PolylineOptions eraserLine) {
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

    public void handleNewLocation(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }
}
