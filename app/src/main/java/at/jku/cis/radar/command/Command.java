package at.jku.cis.radar.command;


import com.google.maps.android.geojson.GeoJsonLayer;

public abstract class Command {

    protected final GeoJsonLayer geoJsonLayer;

    public Command(GeoJsonLayer geoJsonLayer) {
        this.geoJsonLayer = geoJsonLayer;
    }

    public abstract void doCommand();

    public abstract void undoCommand();
}
