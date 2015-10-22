package at.jku.cis.radar.service;

import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;

import java.util.ArrayList;

import at.jku.cis.radar.model.MyGeoJsonFeature;

public class GeoJsonFeatureBuilder {
    private String id;
    private int color;
    private GeoJsonGeometryCollection geoJsonGeometryCollection;

    public GeoJsonFeatureBuilder() {
        this(new GeoJsonGeometryCollection(new ArrayList<GeoJsonGeometry>()));
    }

    public GeoJsonFeatureBuilder(GeoJsonGeometryCollection geoJsonGeometryCollection) {
        this.geoJsonGeometryCollection = geoJsonGeometryCollection;
    }

    public GeoJsonFeatureBuilder setColor(int color) {
        this.color = color;
        return this;
    }

    public MyGeoJsonFeature build() {
        return new MyGeoJsonFeature(geoJsonGeometryCollection, id, null, null);
    }

    public GeoJsonFeatureBuilder setId(String id) {
        this.id = id;
        return this;
    }
}
