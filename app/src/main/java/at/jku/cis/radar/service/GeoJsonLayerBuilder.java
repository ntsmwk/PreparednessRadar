package at.jku.cis.radar.service;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.geojson.GeoJsonLayer;

import org.json.JSONObject;

public class GeoJsonLayerBuilder {

    private JSONObject jsonObject;
    private GoogleMap googleMap;
    private int color;

    public GeoJsonLayerBuilder setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
        return this;
    }

    public GeoJsonLayerBuilder setColor(int color) {
        this.color = color;
        return this;
    }

    public GeoJsonLayerBuilder setGoogleMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
        return this;
    }

    public GeoJsonLayer build() {
        GeoJsonLayer geoJsonLayer = new GeoJsonLayer(googleMap, jsonObject);
        geoJsonLayer.getDefaultPointStyle().setIcon(BitmapDescriptorFactory
                .defaultMarker(convertToHSV(color)[0]));
        geoJsonLayer.getDefaultPolygonStyle().setFillColor(color);
        geoJsonLayer.getDefaultPolygonStyle().setStrokeColor(color);
        geoJsonLayer.getDefaultLineStringStyle().setColor(color);
        return geoJsonLayer;
    }

    private float[] convertToHSV(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }
}
