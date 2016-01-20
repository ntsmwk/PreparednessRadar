package at.jku.cis.radar.activity;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.google.android.gms.maps.MapFragment;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.view.EvolutionView;

public class EvolutionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evolution);
        initializeEvolutionView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        evolveFeature(getIntent().getExtras());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_evolution, menu);
        MenuItem.OnMenuItemClickListener menuItemClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.back:
                        finish();
                }
                return true;
            }
        };
        menu.findItem(R.id.back).setOnMenuItemClickListener(menuItemClickListener);
        return super.onCreateOptionsMenu(menu);
    }

    private void evolveFeature(Bundle extras) {
        Event event = (Event) extras.getSerializable("event");
        EvolutionView evolutionView = (EvolutionView) findViewById(R.id.mapView);
        evolutionView.handleFeatureGroupVisible(event, Long.valueOf(extras.getString("featureId")));
    }

    private void initializeEvolutionView() {
        MapFragment mapFragment = MapFragment.newInstance();
        mapFragment.getMapAsync((EvolutionView) findViewById(R.id.mapView));
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.mapView, mapFragment);
        fragmentTransaction.commit();
    }
}