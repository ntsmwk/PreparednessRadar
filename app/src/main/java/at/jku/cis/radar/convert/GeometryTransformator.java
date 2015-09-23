package at.jku.cis.radar.convert;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

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
        } else if(geoJsonGeometry instanceof GeoJsonMultiPolygon){
            Polygon[] polygons = generatePolygonListFromGeoJsonMultiPolygon((GeoJsonMultiPolygon) geoJsonGeometry, projection);
            geometry = new GeometryFactory().createMultiPolygon(polygons);
        }
        return geometry;
    }

    @NonNull
    private static Polygon[] generatePolygonListFromGeoJsonMultiPolygon(GeoJsonMultiPolygon geoJsonGeometry, Projection projection) {
        Polygon[] polygons = new Polygon[geoJsonGeometry.getPolygons().size()];
        for(int i = 0; i < geoJsonGeometry.getPolygons().size(); i++){
            GeoJsonPolygon geoJsonPolygon = geoJsonGeometry.getPolygons().get(i);
            Polygon polygon = (Polygon)transformToGeometry(geoJsonPolygon, projection);
            polygons[i] = polygon;
        }
        return polygons;
    }

    public static GeoJsonGeometry transformToGeoJsonGeometry(Geometry geometry, Projection projection){
        GeoJsonGeometry geoJsonGeometry = null;
        if (geometry instanceof LineString) {
            geoJsonGeometry = new GeoJsonLineString(convertCoordinateToLatLng(geometry.getCoordinates(), projection));
        } else if (geometry instanceof Polygon) {
            List<List<LatLng>> coordinates = new ArrayList<>();
            List<LatLng> exteriorRing = convertCoordinateToLatLng(geometry.getCoordinates(), projection);
            coordinates.add(exteriorRing);
            geoJsonGeometry = new GeoJsonPolygon(coordinates);
        } else if (geometry instanceof Point) {
            geoJsonGeometry = new GeoJsonPoint(convertCoordinateToLatLng(geometry.getCoordinate(), projection));
        } else if(geometry instanceof MultiPolygon){
            geoJsonGeometry = createGeoJsonMultiPolygon((MultiPolygon)geometry, projection);
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

    private static GeoJsonMultiPolygon createGeoJsonMultiPolygon(MultiPolygon geometry, Projection projection){
        List<GeoJsonPolygon> polygonList = new ArrayList<>();
        for(int i = 0; i < geometry.getNumGeometries(); i++){
            polygonList.add((GeoJsonPolygon)transformToGeoJsonGeometry(geometry.getGeometryN(i), projection));
        }
        return new GeoJsonMultiPolygon(polygonList);

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
