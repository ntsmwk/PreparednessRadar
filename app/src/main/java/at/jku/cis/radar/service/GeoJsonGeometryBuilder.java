package at.jku.cis.radar.service;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.convert.GeometryTransformator;
import at.jku.cis.radar.model.DrawType;

public class GeoJsonGeometryBuilder {
    private DrawType drawType;
    private List<LatLng> coordinates = new ArrayList<>();

    public GeoJsonGeometryBuilder(DrawType drawType) {
        this.drawType = drawType;
    }

    public GeoJsonGeometryBuilder addCoordinate(LatLng coordinate) {
        coordinates.add(coordinate);
        return this;
    }

    public GeoJsonGeometry build(Projection projection) {
        if (DrawType.LINE == drawType) {
            return new GeoJsonLineString(coordinates);
        } else if (DrawType.POLYGON == drawType) {
            return createGeoJsonPolygon(projection);
        } else {
            return new GeoJsonPoint(coordinates.get(coordinates.size() - 1));
        }
    }

    private GeoJsonGeometry createGeoJsonPolygon(Projection projection) {
        GeoJsonPolygon geoJsonPolygon = new GeoJsonPolygon(createListOfCoordinates(coordinates));
        Polygon polygon = (Polygon) GeometryTransformator.transformToGeometry(geoJsonPolygon, projection);
        if (polygon.isSimple()) {
            return geoJsonPolygon;
        }
        return createComplexPolygon(polygon, projection);
    }

    private List<List<LatLng>> createListOfCoordinates(List<LatLng> coordinates) {
        List<List<LatLng>> listOfCoordinates = new ArrayList<>();
        listOfCoordinates.add(coordinates);
        return listOfCoordinates;
    }

    private GeoJsonMultiPolygon createComplexPolygon(Polygon polygon, Projection projection) {
        List<Polygon> polygons = JTSUtils.repair(polygon);
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
        GeoJsonMultiPolygon geoJsonPolygon = (GeoJsonMultiPolygon) GeometryTransformator.transformToGeoJsonGeometry(multiPolygon, projection);
        return geoJsonPolygon;
    }
}
