package at.jku.cis.radar.command;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;

public class AddGeometryEvolveCommand extends Command {


    GeoJsonFeature oldGeoJsonFeature, newGeoJsonFeature;

    public AddGeometryEvolveCommand(GeoJsonFeature newGeoJsonFeature, GeoJsonFeature oldGeoJsonFeature, GeoJsonLayer geoJsonLayer) {
        super(geoJsonLayer);
        this.oldGeoJsonFeature = oldGeoJsonFeature;
        this.newGeoJsonFeature = newGeoJsonFeature;
    }

    @Override
    public void doCommand() {
        geoJsonLayer.addFeature(newGeoJsonFeature);
        geoJsonLayer.removeFeature(oldGeoJsonFeature);
    }

    @Override
    public void undoCommand() {
        geoJsonLayer.addFeature(oldGeoJsonFeature);
        geoJsonLayer.removeFeature(newGeoJsonFeature);
    }
}
