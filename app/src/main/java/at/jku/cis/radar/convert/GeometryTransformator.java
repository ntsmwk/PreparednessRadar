package at.jku.cis.radar.convert;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

public class GeometryTransformator {

    private final static int POLYGON_EXTERIOR_RING_INDEX = 0;

    public static Geometry transformToGeometry(GeoJsonGeometry geoJsonGeometry) {
        Geometry geometry = null;
        if (geoJsonGeometry instanceof GeoJsonLineString) {
            geometry = createLineString((GeoJsonLineString) geoJsonGeometry);
        } else if (geoJsonGeometry instanceof GeoJsonPolygon) {
            geometry = createPolygon((GeoJsonPolygon) geoJsonGeometry);
        } else if (geoJsonGeometry instanceof GeoJsonPoint) {
            geometry = createPoint((GeoJsonPoint) geoJsonGeometry);
        } else if (geoJsonGeometry instanceof GeoJsonMultiPolygon) {
            Polygon[] polygons = generatePolygonListFromGeoJsonMultiPolygon((GeoJsonMultiPolygon) geoJsonGeometry);
            geometry = new GeometryFactory().createMultiPolygon(polygons);
        }
        return geometry;
    }

    public static GeometryCollection transformToGeometryCollection(GeoJsonGeometryCollection geoJsonGeometryCollection) {
        ArrayList<Geometry> geometryArrayList = new ArrayList<>();
        for (GeoJsonGeometry geoJsonGeometry : geoJsonGeometryCollection.getGeometries()) {
            geometryArrayList.add(transformToGeometry(geoJsonGeometry));
        }
        return new GeometryFactory().createGeometryCollection(geometryArrayList.toArray(new Geometry[geometryArrayList.size()]));
    }

    public static GeoJsonGeometry transformToGeoJsonGeometry(Geometry geometry) {
        GeoJsonGeometry geoJsonGeometry = null;
        if (geometry instanceof LineString) {
            geoJsonGeometry = new GeoJsonLineString(toLatLng(geometry.getCoordinates()));
        } else if (geometry instanceof Polygon) {
            List<List<LatLng>> coordinates = new ArrayList<>();
            List<LatLng> exteriorRing = toLatLng(geometry.getCoordinates());
            coordinates.add(exteriorRing);
            geoJsonGeometry = new GeoJsonPolygon(coordinates);
        } else if (geometry instanceof Point) {
            geoJsonGeometry = new GeoJsonPoint(new LatLng(geometry.getCoordinate().x, geometry.getCoordinate().y));
        } else if (geometry instanceof MultiPolygon) {
            geoJsonGeometry = createGeoJsonMultiPolygon((MultiPolygon) geometry);
        }
        return geoJsonGeometry;

    }

    private static Polygon[] generatePolygonListFromGeoJsonMultiPolygon(GeoJsonMultiPolygon geoJsonGeometry) {
        Polygon[] polygons = new Polygon[geoJsonGeometry.getPolygons().size()];
        for (int i = 0; i < geoJsonGeometry.getPolygons().size(); i++) {
            GeoJsonPolygon geoJsonPolygon = geoJsonGeometry.getPolygons().get(i);
            Polygon polygon = (Polygon) transformToGeometry(geoJsonPolygon);
            polygons[i] = polygon;
        }
        return polygons;
    }

    private static Point createPoint(GeoJsonPoint geoJsonGeometry) {
        return new GeometryFactory().createPoint(new Coordinate(geoJsonGeometry.getCoordinates().latitude, geoJsonGeometry.getCoordinates().longitude));
    }

    private static LineString createLineString(GeoJsonLineString geoJsonGeometry) {
        List<LatLng> latLngList = geoJsonGeometry.getCoordinates();
        Coordinate[] coordinates = toCoordinates(latLngList);
        return new GeometryFactory().createLineString(coordinates);
    }

    private static Polygon createPolygon(GeoJsonPolygon geoJsonGeometry) {
        List<LatLng> latLngList = geoJsonGeometry.getCoordinates().get(POLYGON_EXTERIOR_RING_INDEX);
        addEndingElementToClosePolygon(latLngList);
        Coordinate[] coordinates = toCoordinates(latLngList);
        return new GeometryFactory().createPolygon(coordinates);
    }

    private static GeoJsonMultiPolygon createGeoJsonMultiPolygon(MultiPolygon geometry) {
        List<GeoJsonPolygon> polygonList = new ArrayList<>();
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            polygonList.add((GeoJsonPolygon) transformToGeoJsonGeometry(geometry.getGeometryN(i)));
        }
        return new GeoJsonMultiPolygon(polygonList);

    }

    private static Coordinate[] toCoordinates(List<LatLng> latLngList) {
        Coordinate[] coordinates = new Coordinate[latLngList.size()];
        for (int i = 0; i < latLngList.size(); i++) {
            coordinates[i] = new Coordinate(latLngList.get(i).latitude, latLngList.get(i).longitude);
        }
        return coordinates;
    }

    private static List<LatLng> toLatLng(Coordinate[] coordinates) {
        List<LatLng> latLngList = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i++) {
            latLngList.add(new LatLng(coordinates[i].x, coordinates[i].y));
        }
        return latLngList;
    }

    private static void addEndingElementToClosePolygon(List<LatLng> latLngList) {
        latLngList.add(latLngList.get(0));
    }
}
