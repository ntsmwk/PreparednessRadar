package at.jku.cis.radar.view;

import android.app.FragmentTransaction;
import android.content.IntentSender;
import android.graphics.Color;
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
import com.google.android.gms.maps.MapFragment;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.ApplicationMode;
import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;

public class RadarActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    public static final String TAG = RadarActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final float ALPHA_VISIBLE = 1.0f;
    public static final float ALPHA_HIDDEN = 0.2f;

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);
        initializeSideBar();
        initializeGoogleMap();
        initializeGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        setPenModeMenuClickListener(menu);
        setLineMenuClickListener(menu);
        setPolygonMenuClickListener(menu);
        setMarkerMenuClickListener(menu);
        setEditMenuClickListener(menu);
        setEvolveMenuClickListener(menu);
        return true;
    }

    private void initializeGoogleMap() {
        MapFragment mapFragment = MapFragment.newInstance();
        mapFragment.getMapAsync(findGoogleView());
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.MapLayout, mapFragment);
        fragmentTransaction.commit();
    }

    private void initializeSideBar() {
        EventTreeFragment eventTreeFragment = new EventTreeFragment();
        eventTreeFragment.addEventClickListener(findGoogleView());
        getFragmentManager().beginTransaction().add(R.id.SidebarLayout, eventTreeFragment).commit();
    }

    private void initializeGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = buildGoogleApiClient();
        }
        googleApiClient.connect();
    }

    private GoogleView findGoogleView() {
        return (GoogleView) findViewById(R.id.MapLayout);
    }

    private void setPenModeMenuClickListener(Menu menu) {
        menu.findItem(R.id.erase).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                PenSetting penSetting = findGoogleView().getPenSetting();
                if (PenMode.DRAWING.equals(penSetting.getPenMode())) {
                    menuItem.setIcon(R.drawable.eraser_icon);
                    penSetting.setPenMode(PenMode.ERASING);
                } else {
                    menuItem.setIcon(R.drawable.pen_icon);
                    penSetting.setPenMode(PenMode.DRAWING);
                }
                return true;
            }
        });
    }

    private void setLineMenuClickListener(final Menu menu) {
        menu.findItem(R.id.line).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PenSetting penSetting = findGoogleView().getPenSetting();
                deactivateDrawMenuItems(menu);
                item.setIcon(R.drawable.line_icon_activated);
                penSetting.setPenMode(PenMode.DRAWING);
                menu.findItem(R.id.erase).setIcon(R.drawable.pen_icon);
                penSetting.setDrawType(DrawType.LINE);
                return true;
            }
        });
    }


    private void setPolygonMenuClickListener(final Menu menu) {
        menu.findItem(R.id.polygon).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PenSetting penSetting = findGoogleView().getPenSetting();
                deactivateDrawMenuItems(menu);
                //item.setIcon(R.drawable.polygon_icon_activated);
                item.setIcon(R.drawable.polygonselected);
                menu.findItem(R.id.erase).setIcon(R.drawable.pen_icon);

                penSetting.setPenMode(PenMode.DRAWING);
                penSetting.setDrawType(DrawType.POLYGON);
                return true;
            }
        });
    }

    private void setMarkerMenuClickListener(final Menu menu) {
        menu.findItem(R.id.marker).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PenSetting penSetting = findGoogleView().getPenSetting();
                deactivateDrawMenuItems(menu);
                //item.setIcon(R.drawable.marker_icon_activated);
                item.setIcon(R.drawable.markerselected);
                menu.findItem(R.id.erase).setIcon(R.drawable.pen_icon);
                penSetting.setPenMode(PenMode.DRAWING);
                penSetting.setDrawType(DrawType.MARKER);
                return true;
            }
        });
    }

    private void setEditMenuClickListener(final Menu menu) {
        menu.findItem(R.id.edit).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                GoogleView googleView = findGoogleView();
                if (ApplicationMode.PAINT == googleView.getApplicationMode()) {
                    menu.findItem(R.id.edit).setTitle(R.string.edit);
                    disableEventTreeFragement();
                    googleView.handleApplicationModeChanged(ApplicationMode.EDIT);
                } else {
                    menu.findItem(R.id.edit).setTitle(R.string.noEdit);
                    enableEventTreeFragement();
                    googleView.handleApplicationModeChanged(ApplicationMode.PAINT);
                }
                return true;
            }
        });
    }

    private void setEvolveMenuClickListener(final Menu menu) {
        menu.findItem(R.id.evolve).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                GoogleView googleView = findGoogleView();
                if (ApplicationMode.PAINT == googleView.getApplicationMode()) {
                    menu.findItem(R.id.evolve).setTitle(R.string.evolve);
                    disableEventTreeFragement();
                    googleView.handleApplicationModeChanged(ApplicationMode.EVOLE);
                } else {
                    enableEventTreeFragement();
                    menu.findItem(R.id.evolve).setTitle(R.string.noEdit);
                    googleView.handleApplicationModeChanged(ApplicationMode.PAINT);
                }
                return true;
            }
        });
    }

    private void enableEventTreeFragement() {
        ((EventTreeFragment) getFragmentManager().findFragmentById(R.id.SidebarLayout)).setDisabled(false);
        findViewById(R.id.SidebarLayout).setAlpha(ALPHA_VISIBLE);
        findViewById(R.id.SidebarLayout).setBackgroundColor(Color.WHITE);
    }


    private void disableEventTreeFragement() {
        ((EventTreeFragment) getFragmentManager().findFragmentById(R.id.SidebarLayout)).setDisabled(true);
        findViewById(R.id.SidebarLayout).setAlpha(ALPHA_HIDDEN);
        findViewById(R.id.SidebarLayout).setBackgroundColor(Color.GRAY);
    }

    private void deactivateDrawMenuItems(Menu menu) {
        //menu.findItem(R.id.polygon).setIcon(R.drawable.polygon_icon);
        menu.findItem(R.id.polygon).setIcon(R.drawable.polygonnotselected);

        menu.findItem(R.id.line).setIcon(R.drawable.line_icon);
        //menu.findItem(R.id.marker).setIcon(R.drawable.marker_icon);
        menu.findItem(R.id.marker).setIcon(R.drawable.markernotselected);
    }

    private GoogleApiClient buildGoogleApiClient() {
        GoogleApiClient.Builder googleApiClientBuilder = new GoogleApiClient.Builder(this);
        googleApiClientBuilder.addConnectionCallbacks(this);
        googleApiClientBuilder.addOnConnectionFailedListener(this);
        googleApiClientBuilder.addApi(LocationServices.API);
        return googleApiClientBuilder.build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location == null) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(10 * 1000)   // 10 seconds, in milliseconds
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
            removeLocationUpdates();
            googleApiClient.disconnect();
        }
    }

    private void removeLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }
}