package at.jku.cis.radar.view;

import android.content.IntentSender;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.R;

public class RadarActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    public static final String TAG = RadarActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private View mapView;
    private GoogleMap googleMap;
    private List<PolylineOptions> polyLines = new ArrayList<>();
    private PolylineOptions line = null;
    private PolylineOptions eraserLine = null;

    private GoogleApiClient googleApiClient;

    private ImageView mEraserBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);
        intializeMapView();
        initializeGoogleMap();
        initializeGoogleApiClient();
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent motionEvent) {

        Point currentPosition = new Point((int) motionEvent.getRawX(), (int) motionEvent.getRawY() - getStatusBarHeight());
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
            mapView.dispatchTouchEvent(motionEvent);
        }
        return true;
    }

    private SupportMapFragment getSupportMapFragment() {
        return (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    }

    private void intializeMapView() {
        mapView = ((FrameLayout) getSupportMapFragment().getView()).getChildAt(0);
    }

    private void initializeGoogleMap() {
        googleMap = getSupportMapFragment().getMap();
        googleMap.setMyLocationEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private void initializeGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = buildGoogleApiClient();
        }
        googleApiClient.connect();
    }

    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder googleApiClientBuilder = new GoogleApiClient.Builder(this);
        googleApiClientBuilder.addConnectionCallbacks(this);
        googleApiClientBuilder.addOnConnectionFailedListener(this);
        googleApiClientBuilder.addApi(LocationServices.API);
        return googleApiClientBuilder.build();
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
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
                        //double factorY = prevEraser.x*(prev.y-currentPoint.y)-prevEraser.y*(prev.x-currentPoint.x)+prev.y*()
                    }
                    prevEraser = currentEraserPoint;

                }
                prev = currentPoint;
            }
        }

        return false;
    }


    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location == null) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                    .setFastestInterval(1000) // 1 second, in milliseconds
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

        } else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeGoogleMap();
        initializeGoogleApiClient();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}