package at.jku.cis.preparednessradar.view;


import android.content.Context;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;

import java.io.IOException;

import at.jku.cis.preparednessradar.R;

public class MapPane extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, OnMapClickListener, SpenTouchListener {

    public static final String TAG = MapPane.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Context mContext;
    private SpenNoteDoc mSpenNoteDoc;
    private SpenPageDoc mSpenPageDoc;
    private SpenSurfaceView mSpenSurfaceView;
    private boolean isSpenFeatureEnabled = false;
    private Spen spenPackage = new Spen();
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_map_pane);
        MapsInitializer.initialize(this);
        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        // Gets to GoogleMap from the MapView and does initialization stuff
        mMap = mapView.getMap();
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setMyLocationEnabled(true);
        mContext = this;
        setUpMap();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        initializePen();
        createPenView();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        setUpMap();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        mapView.onPause();
        super.onPause();
    }


    private void setUpMap() {
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.setOnMapClickListener(new OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                mMap.addMarker(new MarkerOptions().position(point).title("Touch Marker"));
            }
        });
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //handleNewLocation(location);
    }


    @Override
    public void onMapClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title("MarkerTouch"));
    }

    private void initializeSPen(Context context) {

    }

    private void initializePen() {
        try {
            spenPackage.initialize(this, 5, Spen.SPEN_STATIC_LIB_MODE);
            isSpenFeatureEnabled = spenPackage.isFeatureEnabled(Spen.DEVICE_PEN);
        } catch (SsdkUnsupportedException e) {
            Toast.makeText(mContext, "This device does not support S pen.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        } catch (Exception e1) {
            Toast.makeText(mContext, "Cannot initialize Pen.", Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
            finish();
        }
    }

    private void createPenView() {
        // Create Pen View.
        MapView spenViewLayout = (MapView) findViewById(R.id.map);
        mSpenSurfaceView = new SpenSurfaceView(mContext);
        if (mSpenSurfaceView == null) {
            Toast.makeText(mContext, "Cannot create new SpenSurfaceView.", Toast.LENGTH_SHORT).show();
            finish();
        }
        mSpenSurfaceView.setZOrderOnTop(true);
        SurfaceHolder sfhTrackHolder = mSpenSurfaceView.getHolder();
        sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);


        spenViewLayout.addView(mSpenSurfaceView);
        // Get the dimensions of the screen.
        mSpenSurfaceView.setTouchListener(this);
       // mSpenSurfaceView.setHoverListener(this);
       // mSpenSurfaceView.setZoomListener(this);
        // Get the dimensions of the screen.
        Display display = getWindowManager().getDefaultDisplay();
        Rect rect = new Rect();
        display.getRectSize(rect);

        // Create SpenNoteDoc.
        try {
            mSpenNoteDoc = new SpenNoteDoc(mContext, 10*rect.width(), 10*rect.height());
        } catch (IOException e) {
            Toast.makeText(mContext, "Cannot create new NoteDoc.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        // After adding a page to NoteDoc, get an instance and set it
        // as a member variable.
        mSpenPageDoc = mSpenNoteDoc.appendPage();
        mSpenPageDoc.setBackgroundColor(Color.TRANSPARENT);
        mSpenPageDoc.clearHistory();
        // Set PageDoc to View.
        mSpenSurfaceView.setPageDoc(mSpenPageDoc, true);
        mSpenSurfaceView.update();
        if (isSpenFeatureEnabled == false) {
            mSpenSurfaceView.setToolTypeAction(SpenSurfaceView.TOOL_FINGER, SpenSurfaceView.ACTION_STROKE);
            Toast.makeText(mContext, "Device does not support S pen. \n You can draw strokes with your finger", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
       // Toast.makeText(mContext, "Touching", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}