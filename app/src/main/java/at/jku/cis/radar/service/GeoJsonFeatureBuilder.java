package at.jku.cis.radar.service;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

public class GeoJsonFeatureBuilder {
    private int color;
    private GeoJsonGeometryCollection geoJsonGeometryCollection;

    public GeoJsonFeatureBuilder(GeoJsonGeometryCollection geoJsonGeometryCollection) {
        this.geoJsonGeometryCollection = geoJsonGeometryCollection;
    }

    public GeoJsonFeatureBuilder setColor(int color) {
        this.color = color;
        return this;
    }

    public GeoJsonFeature build(String id) {
        FeatureStyleService featureStyleService = new FeatureStyleService();

        GeoJsonFeature geoJsonFeature = new GeoJsonFeature(geoJsonGeometryCollection, id, null, null);
        geoJsonFeature.setPointStyle(featureStyleService.createDefaultPointStyle(color));
        geoJsonFeature.setLineStringStyle(featureStyleService.createDefaultLineStringStyle(color));
        geoJsonFeature.setPolygonStyle(featureStyleService.createDefaultPolygonStyle(color));
        return geoJsonFeature;
    }
}
