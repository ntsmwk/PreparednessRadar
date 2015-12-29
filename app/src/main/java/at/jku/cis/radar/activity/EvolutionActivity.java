package at.jku.cis.radar.activity;

import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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
        ///initializeEvolutionView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Event event = new Event();
        event.setId(15);
        event.setColor(Color.RED);
        ((EvolutionView) findViewById(R.id.mapView)).handleFeatureGroupVisible(event, 1);
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