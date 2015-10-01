package at.jku.cis.radar.command;


import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.vividsolutions.jts.geom.GeometryCollection;

import at.jku.cis.radar.convert.GeometryTransformator;
import at.jku.cis.radar.geometry.GeometryUtils;

public class AddGeometryEditCommand extends Command {

    GeoJsonGeometryCollection additionalGeometries;
    GeoJsonGeometryCollection oldGeoJsonGeometryCollection;
    GeoJsonFeature geoJsonFeature;

    public AddGeometryEditCommand(GeoJsonGeometryCollection additionalGeometries, GeoJsonLayer geoJsonLayer, GeoJsonFeature geoJsonFeature) {
        super(geoJsonLayer);
        this.additionalGeometries = additionalGeometries;
        this.geoJsonFeature = geoJsonFeature;
        this.oldGeoJsonGeometryCollection = (GeoJsonGeometryCollection)geoJsonFeature.getGeometry();
    }

    @Override
    public void doCommand() {
        GeoJsonGeometryCollection geoJsonGeometryCollection = (GeoJsonGeometryCollection)geoJsonFeature.getGeometry();
        geoJsonGeometryCollection.getGeometries().addAll(additionalGeometries.getGeometries());
        GeometryCollection geometryCollection = GeometryTransformator.transformToGeometryCollection(geoJsonGeometryCollection);
        geoJsonFeature.setGeometry(GeometryTransformator.transformToGeoJsonGeometryCollection(GeometryUtils.union(geometryCollection)));
        refreshLayer();
    }

    @Override
    public void undoCommand() {
        geoJsonFeature.setGeometry(oldGeoJsonGeometryCollection);
        refreshLayer();
    }

    private void refreshLayer() {
        geoJsonLayer.removeLayerFromMap();
        geoJsonLayer.addLayerToMap();
    }
}
