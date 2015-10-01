package at.jku.cis.radar.transformer;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;

import java.util.ArrayList;
import java.util.List;

public class GeoJsonGeometry2GeometryTransformer implements Transformer<GeoJsonGeometry, Geometry> {

    private GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    public Geometry transform(GeoJsonGeometry geoJsonGeometry) {
        Geometry geometry = null;
        if (geoJsonGeometry instanceof GeoJsonLineString) {
            geometry = createLineString((GeoJsonLineString) geoJsonGeometry);
        } else if (geoJsonGeometry instanceof GeoJsonPolygon) {
            geometry = createPolygon((GeoJsonPolygon) geoJsonGeometry);
        } else if (geoJsonGeometry instanceof GeoJsonPoint) {
            geometry = createPoint((GeoJsonPoint) geoJsonGeometry);
        } else if (geoJsonGeometry instanceof GeoJsonMultiPolygon) {
            geometry = createMultiPolygon((GeoJsonMultiPolygon) geoJsonGeometry);
        } else if (geoJsonGeometry instanceof GeoJsonGeometryCollection) {
            geometry = createGeometryCollection((GeoJsonGeometryCollection) geoJsonGeometry);
        }
        return geometry;
    }

    private Point createPoint(GeoJsonPoint geoJsonPoint) {
        return geometryFactory.createPoint(toCoordinate(geoJsonPoint.getCoordinates()));
    }

    private LineString createLineString(GeoJsonLineString geoJsonGeometry) {
        Coordinate[] coordinates = toCoordinates(geoJsonGeometry.getCoordinates());
        return geometryFactory.createLineString(coordinates);
    }

    private Geometry createGeometryCollection(GeoJsonGeometryCollection geoJsonGeometryCollection) {
        List<Geometry> geometries = new ArrayList<>(CollectionUtils.collect(geoJsonGeometryCollection.getGeometries(), this));
        return geometryFactory.createGeometryCollection(geometries.toArray(new Geometry[geometries.size()]));
    }

    private Geometry createMultiPolygon(GeoJsonMultiPolygon geoJsonMultiPolygon) {
        Polygon[] polygons = new Polygon[geoJsonMultiPolygon.getPolygons().size()];
        for (int i = 0; i < geoJsonMultiPolygon.getPolygons().size(); i++) {
            polygons[i] = createPolygon(geoJsonMultiPolygon.getPolygons().get(i));
        }
        return geometryFactory.createMultiPolygon(polygons);
    }

    private Polygon createPolygon(GeoJsonPolygon geoJsonGeometry) {
        List<LatLng> latLngList = geoJsonGeometry.getCoordinates().get(0);
        latLngList.add(latLngList.get(0));
        Coordinate[] coordinates = toCoordinates(latLngList);
        return geometryFactory.createPolygon(coordinates);
    }

    private Coordinate[] toCoordinates(List<LatLng> latLngList) {
        Coordinate[] coordinates = new Coordinate[latLngList.size()];
        for (int i = 0; i < latLngList.size(); i++) {
            LatLng latLng = latLngList.get(i);
            coordinates[i] = toCoordinate(latLng);
        }
        return coordinates;
    }

    private Coordinate toCoordinate(LatLng latLng) {
        return new Coordinate(latLng.latitude, latLng.longitude);
    }
}

