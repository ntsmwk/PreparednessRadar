package at.jku.cis.radar.view;

import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;

import at.jku.cis.radar.R;

public class RadarActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    public static final String TAG = RadarActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private View mapView;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;

    private Spen spen = new Spen();
    private SpenNoteDoc spenNoteDoc;
    private SpenSurfaceView spenSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);
        intializeMapView();
        initalizeGoogleMap();
        try {
            intializeSpenSurfaceView();
            initializeMoveListener();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        initalizeGoogleApiClient();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (MotionEvent.TOOL_TYPE_FINGER == event.getToolType(0)) {
            return mapView.dispatchTouchEvent(event);
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    private SupportMapFragment getSupportMapFragment() {
        return (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    }

    private void intializeMapView() {
        mapView = ((FrameLayout) getSupportMapFragment().getView()).getChildAt(0);
    }

    private void intializeSpenSurfaceView() throws Exception {
        spen.initialize(getApplicationContext());
        spenSurfaceView = new SpenSurfaceView(this);
        spenSurfaceView.setZOrderOnTop(true);
        SurfaceHolder surfaceHolder = spenSurfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        ((FrameLayout) getSupportMapFragment().getView()).addView(spenSurfaceView);

        Rect rect = new Rect();
        getWindowManager().getDefaultDisplay().getRectSize(rect);
        spenNoteDoc = new SpenNoteDoc(this, rect.width(), rect.height());
        SpenPageDoc spenPageDoc = spenNoteDoc.appendPage();

        spenPageDoc.clearHistory();
        spenPageDoc.setBackgroundColor(Color.TRANSPARENT);
        spenSurfaceView.setPageDoc(spenPageDoc, true);
        spenSurfaceView.update();
    }

    private void initalizeGoogleMap() {
        googleMap = getSupportMapFragment().getMap();
        googleMap.setMyLocationEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private void initalizeGoogleApiClient() {
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

    private synchronized void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private void initializeMoveListener() {
        spenSurfaceView.setTouchListener(new SpenTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                LatLngBounds curScreen = googleMap.getProjection().getVisibleRegion().latLngBounds;
                LatLng northeast = curScreen.northeast;
                LatLng southwest = curScreen.southwest;
                LatLng northwest = new LatLng(northeast.latitude, southwest.longitude);

                googleMap.addMarker(new MarkerOptions().position(northeast).title("northeast" + northeast.latitude + " " + northeast.longitude));
                googleMap.addMarker(new MarkerOptions().position(southwest).title("southwest" + southwest.latitude + " " + southwest.longitude));

                googleMap.addMarker(new MarkerOptions().position(northwest).title("southwest" + southwest.latitude + " " + southwest.longitude));
                //get Screen Size
                Rect rect = new Rect();
                getWindowManager().getDefaultDisplay().getRectSize(rect);
                double latFactor = calculateLatitudeDifference(northeast, southwest) / rect.width();
                double lngFactor = calculateLongitudeDifference(northeast, southwest) / rect.height();

                //LatLng currentPos = new LatLng(-(motionEvent.getX() * calculateLatitudeDifference(northeast, southwest)/rect.height()), (motionEvent.getY() * calculateLongitudeDifference(northeast, southwest)/rect.width()));
                LatLng currentPos = new LatLng(northwest.latitude + motionEvent.getX() * latFactor, southwest.longitude + motionEvent.getY() * lngFactor);
                //LatLng currentPos2 = new LatLng(northwest.latitude - motionEvent.getX() * latFactor, southwest.longitude + motionEvent.getY() * lngFactor);
                //LatLng currentPos3 = new LatLng(-(-calculateLatitudeDifference(northeast, southwest) / 2 - motionEvent.getX() * latFactor), -calculateLongitudeDifference(northeast, southwest) / 2 + motionEvent.getY() * lngFactor);
                // LatLng currentPos2 = new LatLng(northeast.latitude-(motionEvent.getY() * calculateLongitudeDifference(northeast, southwest) / rect.width()), northeast.latitude+(motionEvent.getX() * calculateLatitudeDifference(northeast, southwest) / rect.height()));
                //LatLng currentPos3 = new LatLng((motionEvent.getY() * calculateLongitudeDifference(northeast, southwest) / rect.width())+2, (motionEvent.getX() * calculateLatitudeDifference(northeast, southwest) / rect.height())+2);
                //  LatLng currentPos4 = new LatLng(-((motionEvent.getX() * latFactor)+10), -(-(motionEvent.getY() * lngFactor)+10));
                googleMap.addMarker(new MarkerOptions().position(currentPos).title("abc Pos1"));
                //googleMap.addMarker(new MarkerOptions().position(currentPos2).title("abc Pos2"));
                //googleMap.addMarker(new MarkerOptions().position(currentPos3).title("abc Pos3"));
                googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Nullpunkt")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                //googleMap.addMarker(new MarkerOptions().position(currentPos4).title("abc Pos4"));
                //googleMap.addMarker(new MarkerOptions().position(curScreen.getCenter()).title("Factor: " + latFactor + "     " + lngFactor));
                Log.i(TAG, "Northwest: " + northwest.latitude + " " + northwest.longitude);
                Log.i(TAG, "Northeast: " + northeast.latitude + " " + northeast.longitude);
                Log.i(TAG, "Southwest: " + southwest.latitude + " " + southwest.longitude);
                Log.i(TAG, "Factor: " + latFactor + " " + lngFactor);
                Log.i(TAG, "Pen: " + motionEvent.getY() * latFactor + " " + motionEvent.getX() * lngFactor);
                // Log.i(TAG, "Point: " + currentPos.latitude + " " + currentPos.longitude);
                //TODO
                return false;
            }
        });

    }

    private double calculateLatitudeDifference(LatLng northeast, LatLng southwest) {
        if (northeast.latitude > 0) {
            return northeast.latitude - southwest.latitude;
        } else {
            return Math.abs(southwest.latitude) - Math.abs(northeast.latitude);
        }
    }

    private double calculateLongitudeDifference(LatLng northeast, LatLng southwest) {
        if (northeast.longitude > 0) {
            return northeast.longitude - southwest.longitude;
        } else {
            return Math.abs(southwest.longitude) - Math.abs(northeast.longitude);
        }
    }


    private LatLng getScreenLocation() {
        return googleMap.getCameraPosition().target;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location == null) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                    .setFastestInterval(1 * 1000) // 1 second, in milliseconds
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
        initalizeGoogleMap();
        initalizeGoogleApiClient();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (spenSurfaceView != null) {
            spenSurfaceView.close();
            spenSurfaceView = null;
        }
        if (spenNoteDoc != null) {
            try {
                spenNoteDoc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            spenNoteDoc = null;
        }
    }
}