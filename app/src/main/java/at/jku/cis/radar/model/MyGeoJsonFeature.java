package at.jku.cis.radar.model;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.geojson.GeoJsonGeometry;

import java.util.HashMap;

public class MyGeoJsonFeature extends com.google.maps.android.geojson.GeoJsonFeature {

    private String id;

    public MyGeoJsonFeature(GeoJsonGeometry geometry, String id, HashMap<String, String> properties, LatLngBounds boundingBox) {
        super(geometry, null, properties, boundingBox);
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
