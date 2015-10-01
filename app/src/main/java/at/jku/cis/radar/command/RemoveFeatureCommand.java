package at.jku.cis.radar.command;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;

import java.util.List;

public class RemoveFeatureCommand extends Command {

    private List<GeoJsonFeature> addList, removeList;

    public RemoveFeatureCommand(GeoJsonLayer geoJsonLayer, List<GeoJsonFeature> addList, List<GeoJsonFeature> removeList) {
        super(geoJsonLayer);
        this.addList = addList;
        this.removeList = removeList;
    }

    @Override
    public void doCommand() {

        for (GeoJsonFeature feature : removeList) {
                geoJsonLayer.removeFeature(feature);
        }
        for (GeoJsonFeature feature : addList) {
                geoJsonLayer.addFeature(feature);
        }
    }

    @Override
    public void undoCommand() {
        for (GeoJsonFeature feature : addList) {
                geoJsonLayer.removeFeature(feature);
        }
        for (GeoJsonFeature feature : removeList) {
            geoJsonLayer.addFeature(feature);
        }
    }
}
