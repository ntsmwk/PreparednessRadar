package at.jku.cis.radar.activity;

import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.MapFragment;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.ApplicationMode;
import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;
import at.jku.cis.radar.timer.CountDownRunner;
import at.jku.cis.radar.view.EventTreeFragment;
import at.jku.cis.radar.view.GoogleView;

public class RadarActivity extends AppCompatActivity {
    public static final float ALPHA_VISIBLE = 1.0f;
    public static final float ALPHA_HIDDEN = 0.2f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);
        startClockThread();
        initializeSideBar();
        initializeGoogleMap();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        findGoogleView().onContextItemSelected(item);
        return super.onContextItemSelected(item);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        handleApplicationModeChanged(findGoogleView().getApplicationMode());
    }

    private void handleApplicationModeChanged(ApplicationMode applicationMode) {
        if (ApplicationMode.EDITING.equals(applicationMode) || ApplicationMode.EVOLVING.equals(applicationMode)) {
            setSidebarDisabled(true, ALPHA_HIDDEN, Color.GRAY);
        } else {
            setSidebarDisabled(false, ALPHA_VISIBLE, Color.WHITE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_radar, menu);
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
        menu.findItem(R.id.polygon).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PenSetting penSetting = findGoogleView().getPenSetting();
                deactivateDrawMenuItems(menu);
                item.setIcon(R.drawable.polygonselected);
                menu.findItem(R.id.erase).setIcon(R.drawable.pen_icon);

                penSetting.setPenMode(PenMode.DRAWING);
                penSetting.setDrawType(DrawType.POLYGON);
                return true;
            }
        });
        menu.findItem(R.id.marker).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PenSetting penSetting = findGoogleView().getPenSetting();
                deactivateDrawMenuItems(menu);
                item.setIcon(R.drawable.markerselected);
                menu.findItem(R.id.erase).setIcon(R.drawable.pen_icon);
                penSetting.setPenMode(PenMode.DRAWING);
                penSetting.setDrawType(DrawType.MARKER);
                return true;
            }
        });
        return true;
    }

    private void initializeGoogleMap() {
        MapFragment mapFragment = MapFragment.newInstance();
        mapFragment.getMapAsync(findGoogleView());
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.mapView, mapFragment);
        fragmentTransaction.commit();
    }

    private void initializeSideBar() {
        EventTreeFragment eventTreeFragment = new EventTreeFragment();
        eventTreeFragment.addEventClickListener(findGoogleView());
        getFragmentManager().beginTransaction().add(R.id.sidebarLayout, eventTreeFragment).commit();
    }

    private GoogleView findGoogleView() {
        return (GoogleView) findViewById(R.id.mapView);
    }

    private void setSidebarDisabled(boolean disabled, float alpha, int gray) {
        ((EventTreeFragment) getFragmentManager().findFragmentById(R.id.sidebarLayout)).setDisabled(disabled);
        View sideLayout = findViewById(R.id.sidebarLayout);
        sideLayout.setAlpha(alpha);
        sideLayout.setBackgroundColor(gray);
    }

    private void deactivateDrawMenuItems(Menu menu) {
        menu.findItem(R.id.polygon).setIcon(R.drawable.polygonnotselected);
        menu.findItem(R.id.line).setIcon(R.drawable.line_icon);
        menu.findItem(R.id.marker).setIcon(R.drawable.markernotselected);
    }

    private void startClockThread() {
        Thread myThread = null;
        Runnable myRunnableThread = new CountDownRunner(this, findGoogleView());
        myThread = new Thread(myRunnableThread);
        myThread.start();
    }


}