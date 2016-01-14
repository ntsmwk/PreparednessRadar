package at.jku.cis.radar.service;

import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

public class FeatureStyleService {
    private final static float WIDTH = 10;

    public GeoJsonPolygonStyle createDefaultPolygonStyle(int color) {
        return createPolygonStyle(color, color);
    }

    public GeoJsonPolygonStyle createEditPolygonStyle(int color) {
        return createPolygonStyle(color, Color.WHITE);
    }

    public GeoJsonLineStringStyle createDefaultLineStringStyle(int color) {
        return createLineStringStyle(color);
    }

    public GeoJsonLineStringStyle createEditLineStringStyle() {
        return createLineStringStyle(Color.WHITE);
    }

    public GeoJsonPointStyle createDefaultPointStyle(int color) {
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
        pointStyle.setIcon(BitmapDescriptorFactory
                .defaultMarker(convertToHSV(color)[0]));
        return pointStyle;
    }

    private GeoJsonPolygonStyle createPolygonStyle(int fillColor, int strokeColor) {
        GeoJsonPolygonStyle polygonStyle = new GeoJsonPolygonStyle();
        polygonStyle.setFillColor(fillColor);
        polygonStyle.setStrokeColor(fillColor);
        polygonStyle.setStrokeWidth(WIDTH);
        return polygonStyle;
    }

    private GeoJsonLineStringStyle createLineStringStyle(int color) {
        GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();
        lineStringStyle.setColor(color);
        lineStringStyle.setWidth(WIDTH);
        return lineStringStyle;
    }

    private float[] convertToHSV(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }
}
