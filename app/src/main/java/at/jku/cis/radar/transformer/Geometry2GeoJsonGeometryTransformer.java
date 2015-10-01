package at.jku.cis.radar.transformer;

import android.support.annotation.NonNull;

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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;

import java.util.ArrayList;
import java.util.List;

public class Geometry2GeoJsonGeometryTransformer implements Transformer<Geometry, GeoJsonGeometry> {

    @Override
    public GeoJsonGeometry transform(Geometry geometry) {
        GeoJsonGeometry geoJsonGeometry = null;
        if (geometry instanceof LineString) {
            geoJsonGeometry = new GeoJsonLineString(toLatLngList(geometry.getCoordinates()));
        } else if (geometry instanceof Polygon) {
            List<List<LatLng>> coordinates = new ArrayList<>();
            List<LatLng> exteriorRing = toLatLngList(geometry.getCoordinates());
            coordinates.add(exteriorRing);
            geoJsonGeometry = new GeoJsonPolygon(coordinates);
        } else if (geometry instanceof Point) {
            geoJsonGeometry = new GeoJsonPoint(toLatLng(geometry.getCoordinate()));
        } else if (geometry instanceof MultiPolygon) {
            geoJsonGeometry = createGeoJsonMultiPolygon((MultiPolygon) geometry);
        } else if (geometry instanceof GeometryCollection){
            geoJsonGeometry = createGeoJsonGeometryCollection((GeometryCollection)geometry);
        }
        return geoJsonGeometry;
    }

    @NonNull
    private GeoJsonGeometry createGeoJsonGeometryCollection(GeometryCollection geometry) {
        GeoJsonGeometry geoJsonGeometry;List<GeoJsonGeometry> geometries = new ArrayList<>(CollectionUtils.collect(getGeometries(geometry), this));
        geoJsonGeometry = new GeoJsonGeometryCollection(geometries);
        return geoJsonGeometry;
    }

    private List<Geometry> getGeometries(GeometryCollection geometryCollection){
        List<Geometry> geometries = new ArrayList<>();
        for(int i = 0; i < geometryCollection.getNumGeometries(); i++){
            geometries.add(geometryCollection.getGeometryN(i));
        }
        return geometries;
    }

    private GeoJsonMultiPolygon createGeoJsonMultiPolygon(MultiPolygon geometry) {
        List<GeoJsonPolygon> polygonList = new ArrayList<>();
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            polygonList.add((GeoJsonPolygon) transform(geometry.getGeometryN(i)));
        }
        return new GeoJsonMultiPolygon(polygonList);

    }

    private List<LatLng> toLatLngList(Coordinate[] coordinates) {
        List<LatLng> latLngList = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i++) {
            latLngList.add(toLatLng(coordinates[i]));
        }
        return latLngList;
    }

    private LatLng toLatLng(Coordinate coordinate) {
        return new LatLng(coordinate.x, coordinate.y);
    }
}

