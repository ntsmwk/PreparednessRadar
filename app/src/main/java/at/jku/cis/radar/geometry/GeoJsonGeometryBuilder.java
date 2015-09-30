package at.jku.cis.radar.geometry;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.convert.GeometryTransformator;
import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.service.JTSUtils;

public class GeoJsonGeometryBuilder {
    private DrawType drawType;
    private List<LatLng> coordinates = new ArrayList<>();
    private boolean simplify = true;

    public GeoJsonGeometryBuilder(DrawType drawType) {
        this.drawType = drawType;
    }

    public GeoJsonGeometryBuilder addCoordinate(LatLng coordinate) {
        coordinates.add(coordinate);
        return this;
    }

    public GeoJsonGeometryBuilder simplify(boolean simplify){
        this.simplify = simplify;
        return this;
    }

    public GeoJsonGeometryCollection build() {
        GeoJsonGeometryCollection geoJsonGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<GeoJsonGeometry>());
        if (DrawType.LINE == drawType) {
            geoJsonGeometryCollection.getGeometries().add(new GeoJsonLineString(coordinates));
        } else if (DrawType.POLYGON == drawType) {
            geoJsonGeometryCollection.getGeometries().add(createGeoJsonPolygon());
        } else {
            geoJsonGeometryCollection.getGeometries().add(new GeoJsonPoint(coordinates.get(coordinates.size() - 1)));
        }
        GeometryCollection geometryCollection = GeometryTransformator.transformToGeometryCollection(geoJsonGeometryCollection);
        return GeometryTransformator.transformToGeoJsonGeometryCollection(GeometryUtils.union(geometryCollection));
    }

    private GeoJsonGeometry createGeoJsonPolygon() {
        GeoJsonPolygon geoJsonPolygon = new GeoJsonPolygon(createListOfCoordinates(coordinates));
        Polygon polygon = (Polygon) GeometryTransformator.transformToGeometry(geoJsonPolygon);
        if (polygon.isSimple() || !simplify) {
            return geoJsonPolygon;
        }
        return createComplexPolygon(polygon);
    }

    private List<List<LatLng>> createListOfCoordinates(List<LatLng> coordinates) {
        List<List<LatLng>> listOfCoordinates = new ArrayList<>();
        listOfCoordinates.add(coordinates);
        return listOfCoordinates;
    }

    private GeoJsonMultiPolygon createComplexPolygon(Polygon polygon) {
        List<Polygon> polygons = JTSUtils.repair(polygon);
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
        GeoJsonMultiPolygon geoJsonPolygon = (GeoJsonMultiPolygon) GeometryTransformator.transformToGeoJsonGeometry(multiPolygon);
        return geoJsonPolygon;
    }
}
