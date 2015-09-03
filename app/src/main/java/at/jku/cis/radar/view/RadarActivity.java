package at.jku.cis.radar.view;

import android.app.FragmentTransaction;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import at.jku.cis.radar.R;
import at.jku.cis.radar.fragment.SelectableTreeFragment;
import at.jku.cis.radar.layout.GoogleView;

public class RadarActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, OnMapReadyCallback {
    public static final String TAG = RadarActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);
        initializeSideBar();
        initializeGoogleMap();
        initializeGoogleApiClient();
    }

    private void initializeGoogleMap() {
        MapFragment mapFragment = MapFragment.newInstance();
        mapFragment.getMapAsync(this);
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.MapLayout, mapFragment);
        fragmentTransaction.commit();
    }

    private void initializeSideBar() {
        SelectableTreeFragment selectableTreeFragment = new SelectableTreeFragment();
        getFragmentManager().beginTransaction().add(R.id.SidebarLayout, selectableTreeFragment).commit();
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        GoogleView googleView = findGoogleView();
        googleView.setMap(googleMap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.getItem(0).setOnMenuItemClickListener(getEraserClickListener());
        return true;
    }

    private MenuItem.OnMenuItemClickListener getEraserClickListener() {

        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                GoogleView googleView = findGoogleView();
                googleView.setEraser();

                return true;
            }
        };
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
            findGoogleView().handleNewLocation(location);
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
        findGoogleView().handleNewLocation(location);
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

    private GoogleView findGoogleView() {
        return (GoogleView) findViewById(R.id.MapLayout);
    }
}