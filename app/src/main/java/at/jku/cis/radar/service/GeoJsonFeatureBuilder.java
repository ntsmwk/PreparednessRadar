package at.jku.cis.radar.service;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;

import java.util.HashMap;
import java.util.Map;

public class GeoJsonFeatureBuilder {
    private int color;
    private GeoJsonGeometryCollection geoJsonGeometryCollection;
    private HashMap<String, String> properties = null;

    public GeoJsonFeatureBuilder(GeoJsonGeometryCollection geoJsonGeometryCollection) {
        this.geoJsonGeometryCollection = geoJsonGeometryCollection;
    }

    public GeoJsonFeatureBuilder setColor(int color) {
        this.color = color;
        return this;
    }

    public GeoJsonFeatureBuilder setProperties(HashMap<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public GeoJsonFeature build(String id) {
        FeatureStyleService featureStyleService = new FeatureStyleService();
        GeoJsonFeature geoJsonFeature = new GeoJsonFeature(geoJsonGeometryCollection, id, this.properties, null);
        geoJsonFeature.setPointStyle(featureStyleService.createDefaultPointStyle(color));
        geoJsonFeature.setLineStringStyle(featureStyleService.createDefaultLineStringStyle(color));
        geoJsonFeature.setPolygonStyle(featureStyleService.createDefaultPolygonStyle(color));
        return geoJsonFeature;
    }
}
