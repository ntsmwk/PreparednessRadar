package at.jku.cis.radar.command;


import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;

public class RemoveGeometryEditCommand extends Command {

    private GeoJsonFeature newFeature, oldFeature;

    public RemoveGeometryEditCommand(GeoJsonLayer geoJsonLayer, GeoJsonFeature oldfeature, GeoJsonFeature newFeature) {
        super(geoJsonLayer);
        this.newFeature = newFeature;
        this.oldFeature = oldfeature;
    }

    @Override
    public void doCommand() {
        geoJsonLayer.removeFeature(oldFeature);
        geoJsonLayer.addFeature(newFeature);
    }

    @Override
    public void undoCommand() {
        geoJsonLayer.removeFeature(newFeature);
        geoJsonLayer.addFeature(oldFeature);
    }
}
