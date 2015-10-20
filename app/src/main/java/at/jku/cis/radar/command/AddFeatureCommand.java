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
        //addTestMarker();
    }

    private void addTestMarker() {
        if (((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0) instanceof GeoJsonPolygon) {
            LatLng latLng = ((GeoJsonPolygon) ((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0)).getCoordinates().get(0).get(0);
            GeoJsonPoint point = new GeoJsonPoint(latLng);
            GeoJsonPointStyle ps = new GeoJsonPointStyle();
            boolean valid = ( new GeoJsonGeometry2GeometryTransformer().transform((((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0)))).isValid();
            boolean simple = ( new GeoJsonGeometry2GeometryTransformer().transform((((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0)))).isSimple();

            ps.setTitle("Valid: " + valid + "  Simple: " + simple);
            GeoJsonFeature feature = new GeoJsonFeature(point, "adsf", null, null);
            feature.setPointStyle(ps);
            geoJsonLayer.addFeature(feature);
        } else if (((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0) instanceof GeoJsonMultiPolygon) {
            LatLng latLng = ((GeoJsonMultiPolygon) ((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0)).getPolygons().get(0).getCoordinates().get(0).get(0);
            GeoJsonPoint point = new GeoJsonPoint(latLng);
            GeoJsonPointStyle ps = new GeoJsonPointStyle();
            boolean valid = ( new GeoJsonGeometry2GeometryTransformer().transform((((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0)))).isValid();
            boolean simple = ( new GeoJsonGeometry2GeometryTransformer().transform((((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().get(0)))).isSimple();

            ps.setTitle("Multi: Valid: " + valid + "  Simple: " + simple);
            GeoJsonFeature feature = new GeoJsonFeature(point, "adsf", null, null);
            feature.setPointStyle(ps);
            geoJsonLayer.addFeature(feature);
        }
    }

    @Override
    public void undoCommand() {
        geoJsonLayer.removeFeature(geoJsonFeature);
    }
}
