package at.jku.cis.radar.command;


import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLayer;

public class AddGeometryEditCommand extends Command {

    GeoJsonGeometryCollection geoJsonGeometryCollection;
    GeoJsonFeature geoJsonFeature;

    public AddGeometryEditCommand(GeoJsonGeometryCollection geoJsonGeometryCollection, GeoJsonLayer geoJsonLayer, GeoJsonFeature geoJsonFeature) {
        super(geoJsonLayer);
        this.geoJsonGeometryCollection = geoJsonGeometryCollection;
        this.geoJsonFeature = geoJsonFeature;
    }

    @Override
    public void doCommand() {
        ((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().addAll(geoJsonGeometryCollection.getGeometries());
        refreshLayer();
    }

    @Override
    public void undoCommand() {
        ((GeoJsonGeometryCollection) geoJsonFeature.getGeometry()).getGeometries().removeAll(geoJsonGeometryCollection.getGeometries());
    }

    private void refreshLayer() {
        geoJsonLayer.removeLayerFromMap();
        geoJsonLayer.addLayerToMap();
    }
}
