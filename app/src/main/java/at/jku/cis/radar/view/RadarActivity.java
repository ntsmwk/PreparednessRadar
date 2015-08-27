package at.jku.cis.radar.view;

import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
import com.samsung.android.sdk.pen.settingui.SpenSettingEraserLayout;
import com.samsung.android.sdk.pen.settingui.SpenSettingPenLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.R;

public class RadarActivity extends AppCompatActivity implements
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
    private SpenPageDoc spenPageDoc;
    private SpenSurfaceView spenSurfaceView;
    private SpenSettingPenLayout spenSettingView;
    private SpenSettingEraserLayout spenEraserSettingView;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.getItem(1).setOnMenuItemClickListener(getColorClickListener());
        menu.getItem(0).setOnMenuItemClickListener(getEarserClickListener());
        return true;
    }

    @NonNull
    private MenuItem.OnMenuItemClickListener getEarserClickListener() {
        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (spenEraserSettingView.isShown()) {
                    spenEraserSettingView.setVisibility(View.GONE);
                } else {
                    spenEraserSettingView.setVisibility(View.VISIBLE);
                    spenEraserSettingView.setViewMode(SpenSettingEraserLayout.VIEW_MODE_NORMAL);
                }
                return false;
            }
        };
    }
    
    @NonNull
    private MenuItem.OnMenuItemClickListener getColorClickListener() {
        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (spenSettingView.isShown()) {
                    spenSettingView.setVisibility(View.GONE);
                } else {
                    spenSettingView.setVisibility(View.VISIBLE);
                    spenSettingView.setViewMode(SpenSettingPenLayout.VIEW_MODE_COLOR);
                    spenSettingView.setExtendedPresetEnable(true);
                }
                return false;
            }
        };
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (MotionEvent.TOOL_TYPE_FINGER == event.getToolType(0) && MotionEvent.ACTION_MOVE == event.getAction()) {
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
        spenSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);

        spenEraserSettingView = new SpenSettingEraserLayout(this, "", new RelativeLayout(this));
        spenEraserSettingView.setCanvasView(spenSurfaceView);
        spenSettingView = new SpenSettingPenLayout(this, "", new RelativeLayout(this));
        spenSettingView.setCanvasView(spenSurfaceView);
        FrameLayout frameLayout = ((FrameLayout) getSupportMapFragment().getView());
        frameLayout.addView(spenSurfaceView);
        frameLayout.addView(spenEraserSettingView.getRootView());
        frameLayout.addView(spenSettingView.getRootView());
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
            private List<PolylineOptions> polyLines = new ArrayList<>();
            private PolylineOptions line = null;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    line = new PolylineOptions();
                }

                Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
                LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
                line.add(currentLatLng);

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    googleMap.addPolyline(line);
                    polyLines.add(line);
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
        if (spenEraserSettingView != null) {
            spenEraserSettingView.close();
        }
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