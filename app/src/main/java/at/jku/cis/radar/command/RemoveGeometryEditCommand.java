package at.jku.cis.radar.command;


import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLayer;

public class RemoveGeometryEditCommand extends Command {

    GeoJsonFeature feature;
    GeoJsonGeometryCollection newGeoJsonGeometryCollection, oldGeoJsonGeometryCollection;

    public RemoveGeometryEditCommand(GeoJsonLayer geoJsonLayer, GeoJsonFeature feature, GeoJsonGeometryCollection newGeoJsonGeometryCollection) {
        super(geoJsonLayer);
        this.feature = feature;
        this.newGeoJsonGeometryCollection = newGeoJsonGeometryCollection;
        this.oldGeoJsonGeometryCollection = (GeoJsonGeometryCollection)feature.getGeometry();
    }

    @Override
    public void doCommand() {
        this.feature.setGeometry(newGeoJsonGeometryCollection);
        refreshLayer();
    }

    @Override
    public void undoCommand() {
        this.feature.setGeometry(oldGeoJsonGeometryCollection);
        refreshLayer();
    }

    private void refreshLayer() {
        geoJsonLayer.removeLayerFromMap();
        geoJsonLayer.addLayerToMap();
    }
}
