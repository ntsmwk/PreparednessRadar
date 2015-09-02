package at.jku.cis.radar.view;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


public class GoogleMapView extends View{

    private final static double SIDEBAR_WIDTH_PERCENTAGE = 0.25;
    private GoogleMap googleMap;


    private List<PolylineOptions> polyLines = new ArrayList<>();
    private PolylineOptions line = null;
    private PolylineOptions eraserLine = null;

    public GoogleMapView(Context context, GoogleMap googleMap) {
        super(context);
        this.googleMap = googleMap;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        Point currentPosition = new Point((int) motionEvent.getRawX() - getSideBarWidth(), (int) motionEvent.getRawY() - getStatusBarHeight());
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
        } else {
            //mapView.dispatchTouchEvent(motionEvent);
        }
        return true;



        //return super.dispatchTouchEvent(motionEventevent);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public int getSideBarWidth() {
        Display display = getDisplay();
        Point size = new Point();
        display.getSize(size);
        return (int) (size.x * SIDEBAR_WIDTH_PERCENTAGE);
    }
}
