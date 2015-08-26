package at.jku.cis.radar.view;

import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

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
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.SpenSettingPenInfo;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.engine.SpenColorPickerListener;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;
import com.samsung.android.sdk.pen.settingui.SpenSettingPenLayout;

import java.io.IOException;
import java.util.ArrayList;

import at.jku.cis.radar.R;

public class RadarActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    public static final String TAG = RadarActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private ArrayList<PolylineOptions> lines = new ArrayList<>();
    private SpenPageDoc spenPageDoc;

    private View mapView;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;

    private Spen spen = new Spen();
    private SpenNoteDoc spenNoteDoc;
    private SpenSurfaceView spenSurfaceView;
    private SpenSettingPenLayout spenSettingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);
        intializeMapView();
        initalizeGoogleMap();
        try {
            initalizeSpenSurfaceView();
            initializeSpenNoteDoc();
            initalizeColorPicker();
            initializeTouchListener();
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

    private void initalizeSpenSurfaceView() throws Exception {
        spen.initialize(getApplicationContext());
        spenSurfaceView = new SpenSurfaceView(this);
        spenSurfaceView.setZOrderOnTop(true);
        SurfaceHolder surfaceHolder = spenSurfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);

        RelativeLayout spenViewLayout = new RelativeLayout(this);
        spenViewLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        spenSettingView = new SpenSettingPenLayout(this, new String(), spenViewLayout);
        spenSettingView.setCanvasView(spenSurfaceView);
        FrameLayout frameLayout = ((FrameLayout) getSupportMapFragment().getView());
        frameLayout.addView(spenViewLayout);
        frameLayout.addView(spenSurfaceView);
    }

    private void initializeSpenNoteDoc() throws IOException {
        Rect rect = new Rect();
        getWindowManager().getDefaultDisplay().getRectSize(rect);
        spenNoteDoc = new SpenNoteDoc(this, rect.width(), rect.height());
        spenPageDoc = spenNoteDoc.appendPage();

        spenPageDoc.clearHistory();
        spenPageDoc.setBackgroundColor(Color.TRANSPARENT);
        spenSurfaceView.setPageDoc(spenPageDoc, true);
        spenSurfaceView.update();
    }

    private void initalizeColorPicker() {
        spenSurfaceView.setColorPickerListener(new SpenColorPickerListener() {
            @Override
            public void onChanged(int color, int x, int y) {
                // Set the color from the Color Picker to the setting view.
                if (spenSettingView != null) {
                    SpenSettingPenInfo penInfo = spenSettingView.getInfo();
                    penInfo.color = color;
                    spenSettingView.setInfo(penInfo);
                }
            }
        });
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

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private void initializeTouchListener() {
        spenSurfaceView.setTouchListener(new SpenTouchListener() {

            private PolylineOptions currentLine = null;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    currentLine = new PolylineOptions();
                }

                Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
                LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
                currentLine.add(currentLatLng);

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    googleMap.addPolyline(currentLine);
                    lines.add(currentLine);
                    spenPageDoc.removeAllObject();
                    spenSurfaceView.update();
                }
                return false;
            }
        });
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
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (spenSettingView != null) {
            spenSettingView.close();
        }
        if (spenSurfaceView != null) {
            spenSurfaceView.close();
        }
        if (spenNoteDoc != null) {
            try {
                spenNoteDoc.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }
}