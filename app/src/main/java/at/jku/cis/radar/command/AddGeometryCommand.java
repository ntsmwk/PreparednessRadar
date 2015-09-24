package at.jku.cis.radar.command;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;

public class AddGeometryCommand extends Command {

    private GeoJsonFeature geoJsonFeature;

    public AddGeometryCommand(GeoJsonFeature geoJsonFeature, GeoJsonLayer geoJsonLayer) {
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
