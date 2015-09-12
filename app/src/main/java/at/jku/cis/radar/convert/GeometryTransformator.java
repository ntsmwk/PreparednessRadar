package at.jku.cis.radar.convert;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

public class GeometryTransformator {

    private final static int POLYGON_EXTERIOR_RING_INDEX = 0;

    public static Geometry transformToGeometry(GeoJsonGeometry geoJsonGeometry, Projection projection) {
        Geometry geometry = null;
        if (geoJsonGeometry instanceof GeoJsonLineString) {
            geometry = createLineString((GeoJsonLineString) geoJsonGeometry, projection);
        } else if (geoJsonGeometry instanceof GeoJsonPolygon) {
            //TODO interior Ring
            geometry = createPolygon((GeoJsonPolygon) geoJsonGeometry, projection);
        } else if (geoJsonGeometry instanceof GeoJsonPoint) {
            geometry = createPoint((GeoJsonPoint) geoJsonGeometry, projection);
        }
        return geometry;
    }

    public static GeoJsonGeometry transformToGeoJsonGeomety(Geometry geometry, Projection projection){
        GeoJsonGeometry geoJsonGeometry = null;
        if (geometry instanceof LineString) {
            geoJsonGeometry = new GeoJsonLineString(convertCoordinateToLatLng(geometry.getCoordinates(), projection));
        } else if (geometry instanceof Polygon) {
        } else if (geometry instanceof Point) {
        }
        return geoJsonGeometry;

    }

    private static List<LatLng> convertCoordinateToLatLng(Coordinate[] coordinates, Projection projection){
        List<LatLng> latLngList = new ArrayList<>();
        for(int i = 0; i < coordinates.length; i++){
            latLngList.add(convertCoordinateToLatLng(coordinates[i], projection));
        }
        return latLngList;
    }

    private static LatLng convertCoordinateToLatLng(Coordinate coordinate, Projection projection) {
        android.graphics.Point point = new android.graphics.Point((int) coordinate.x, (int) coordinate.y);
        return projection.fromScreenLocation(point);
    }


    private static Point createPoint(GeoJsonPoint geoJsonGeometry, Projection projection) {
        return new GeometryFactory().createPoint(getCoordinate(geoJsonGeometry.getCoordinates(), projection));
    }

    private static LineString createLineString(GeoJsonLineString geoJsonGeometry, Projection projection) {
        List<LatLng> latLngList = geoJsonGeometry.getCoordinates();
        Coordinate[] coordinates = getCoordinates(projection, latLngList);
        return new GeometryFactory().createLineString(coordinates);
    }

    private static Polygon createPolygon(GeoJsonPolygon geoJsonGeometry, Projection projection) {
        List<LatLng> latLngList = geoJsonGeometry.getCoordinates().get(POLYGON_EXTERIOR_RING_INDEX);
        addEndingElementToClosePolygon(latLngList);
        Coordinate[] coordinates = getCoordinates(projection, latLngList);
        return new GeometryFactory().createPolygon(coordinates);
    }

    @NonNull
    private static Coordinate[] getCoordinates(Projection projection, List<LatLng> latLngList) {
        Coordinate[] coordinates = new Coordinate[latLngList.size()];
        generateCoordinates(latLngList, projection, coordinates);
        return coordinates;
    }

    private static void addEndingElementToClosePolygon(List<LatLng> latLngList) {
        latLngList.add(latLngList.get(0));
    }


    private static void generateCoordinates(List<LatLng> latLngList, Projection projection, Coordinate[] coordinates) {
        int i = 0;
        for (LatLng latLng : latLngList) {
            coordinates[i] = getCoordinate(latLng, projection);
            i++;
        }
    }

    @NonNull
    private static Coordinate getCoordinate(LatLng latLng, Projection projection) {
        return new Coordinate(projection.toScreenLocation(latLng).x, projection.toScreenLocation(latLng).y);
    }
}
