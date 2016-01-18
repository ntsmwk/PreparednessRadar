package at.jku.cis.radar.command;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.Polygon;

import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;

public class AddFeatureCommand extends Command {

    private GeoJsonFeature geoJsonFeature;

    public AddFeatureCommand(GeoJsonFeature geoJsonFeature, GeoJsonLayer geoJsonLayer) {
        super(geoJsonLayer);
        this.geoJsonFeature = geoJsonFeature;
    }

    @Override
    public void doCommand() {
        geoJsonLayer.addFeature(geoJsonFeature);
    }

    @Override
    public void undoCommand() {
        geoJsonLayer.removeFeature(geoJsonFeature);
    }
}
