package at.jku.cis.radar.service;

import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

public class GeoJsonFeatureBuilder {
    private int color;
    private GeoJsonGeometry geoJsonGeometry;

    public GeoJsonFeatureBuilder(GeoJsonGeometry geoJsonGeometry) {
        this.geoJsonGeometry = geoJsonGeometry;
    }

    public GeoJsonFeatureBuilder setColor(int color) {
        this.color = color;
        return this;
    }

    public GeoJsonFeature build() {
        GeoJsonFeature geoJsonFeature = new GeoJsonFeature(geoJsonGeometry, "id", null, null);
        if (geoJsonGeometry instanceof GeoJsonPoint) {
            geoJsonFeature.setPointStyle(createPointStyle());
        } else if (geoJsonGeometry instanceof GeoJsonLineString) {
            geoJsonFeature.setLineStringStyle(createLineStringStyle());
        } else if (geoJsonGeometry instanceof GeoJsonPolygon || geoJsonGeometry instanceof GeoJsonMultiPolygon) {
            geoJsonFeature.setPolygonStyle(createPolygonStyle());
        }
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
