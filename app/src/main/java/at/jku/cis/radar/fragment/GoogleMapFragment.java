package at.jku.cis.radar.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import at.jku.cis.radar.layout.GoogleView;

public class GoogleMapFragment extends SupportMapFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //GoogleView googleView = new GoogleView(inflater.getContext());
        return super.onCreateView(inflater, container, savedInstanceState);
        //getMapAsync(googleView);
        //return googleView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
