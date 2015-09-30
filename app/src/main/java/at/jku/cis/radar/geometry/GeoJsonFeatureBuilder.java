package at.jku.cis.radar.geometry;

import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonPointStyle;
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

    public GeoJsonFeature build() {
        GeoJsonFeature geoJsonFeature = new GeoJsonFeature(geoJsonGeometryCollection, "id", null, null);
        geoJsonFeature.setPointStyle(createPointStyle());
        geoJsonFeature.setLineStringStyle(createLineStringStyle());
        geoJsonFeature.setPolygonStyle(createPolygonStyle());
        return geoJsonFeature;
    }

    private GeoJsonPointStyle createPointStyle() {
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
        pointStyle.setIcon(BitmapDescriptorFactory
                .defaultMarker(convertToHSV()[0]));
        return pointStyle;
    }

    private GeoJsonPolygonStyle createPolygonStyle() {
        GeoJsonPolygonStyle polygonStyle = new GeoJsonPolygonStyle();
        polygonStyle.setFillColor(color);
        polygonStyle.setStrokeColor(color);
        return polygonStyle;
    }

    private GeoJsonLineStringStyle createLineStringStyle() {
        GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();
        lineStringStyle.setColor(color);
        return lineStringStyle;
    }

    private float[] convertToHSV() {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }
}
