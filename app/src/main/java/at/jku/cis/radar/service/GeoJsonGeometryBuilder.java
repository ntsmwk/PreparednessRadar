package at.jku.cis.radar.service;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;
import at.jku.cis.radar.transformer.Geometry2GeoJsonGeometryTransformer;

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

    public GeoJsonGeometryCollection build() {
        GeoJsonGeometryCollection geoJsonGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<GeoJsonGeometry>());
        if (DrawType.LINE == drawType) {
            geoJsonGeometryCollection.getGeometries().add(new GeoJsonLineString(coordinates));
        } else if (DrawType.POLYGON == drawType) {
            geoJsonGeometryCollection.getGeometries().add(createGeoJsonPolygon());
        } else {
            geoJsonGeometryCollection.getGeometries().add(new GeoJsonPoint(coordinates.get(coordinates.size() - 1)));
        }
        return geoJsonGeometryCollection;
    }

    private GeoJsonGeometry createGeoJsonPolygon() {
        GeoJsonPolygon geoJsonPolygon = new GeoJsonPolygon(Collections.singletonList(coordinates));
        Polygon polygon = transformToPolygon(geoJsonPolygon);
        if (polygon.isSimple()) {
            return geoJsonPolygon;
        }
        return transformToGeoJsonGeometry(PolygonRepairerService.repair(polygon));
    }

    private GeoJsonGeometry transformToGeoJsonGeometry(List<Polygon> polygons) {
        if (polygons.size() == 1) {
            return transformToGeoJsonPolygon(polygons.get(0));
        }
        return transformToGeoJsonMultiPolygon(polygons);
    }

    private GeoJsonGeometry transformToGeoJsonMultiPolygon(List<Polygon> polygons) {
        Polygon[] polygonArray = polygons.toArray(new Polygon[polygons.size()]);
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygonArray);
        return new Geometry2GeoJsonGeometryTransformer().transform(multiPolygon);
    }

    private GeoJsonGeometry transformToGeoJsonPolygon(Polygon polygon) {
        return new Geometry2GeoJsonGeometryTransformer().transform(polygon);
    }

    private Polygon transformToPolygon(GeoJsonPolygon geoJsonPolygon) {
        return (Polygon) new GeoJsonGeometry2GeometryTransformer().transform(geoJsonPolygon);
    }
}
