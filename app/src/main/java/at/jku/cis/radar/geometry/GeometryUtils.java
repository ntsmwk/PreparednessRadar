package at.jku.cis.radar.geometry;

import android.graphics.Color;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.TopologyException;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.service.PolygonRepairerService;


public class GeometryUtils {
    private final static float EDIT_LINE_WIDTH = 10;
    private final static float NORMAL_LINE_WIDTH = 10;
    private final static int EDIT_COLOR = Color.WHITE;
    private final static int MAX_UNION_TRIES = 10;


    public static void setEditableFeature(GeoJsonFeature feature) {
        feature.getPolygonStyle().setStrokeColor(EDIT_COLOR);
        feature.getPolygonStyle().setStrokeWidth(EDIT_LINE_WIDTH);
        feature.getLineStringStyle().setColor(EDIT_COLOR);
        feature.getLineStringStyle().setWidth(EDIT_LINE_WIDTH);
        //TODO POINTS
    }

    public static void setNotEditableFeature(GeoJsonFeature feature) {
        feature.getPolygonStyle().setStrokeColor(feature.getPolygonStyle().getFillColor());
        feature.getPolygonStyle().setStrokeWidth(NORMAL_LINE_WIDTH);
        feature.getLineStringStyle().setColor(feature.getPolygonStyle().getFillColor());
        feature.getLineStringStyle().setWidth(NORMAL_LINE_WIDTH);
        //TODO POINTS
    }

    public static boolean intersects(GeometryCollection geometryCollection, Geometry intersectionGeometry) {
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            if (geometryCollection.getGeometryN(i).intersects(intersectionGeometry)) {
                return true;
            }
        }
        return false;
    }

    public static GeometryCollection union(GeometryCollection geometryCollection) {
        Geometry geometry;
        GeometryCollection collection = geometryCollection;
        int unionTries = 0;
        do {
            try {
                geometry = collection.union();
            } catch (TopologyException e) {
                return collection;
            }
            if (geometry instanceof Polygonal) {
                List<Polygon> polygons = null;
                if (geometry instanceof Polygon) {
                    polygons = PolygonRepairerService.repair((Polygon) geometry);
                } else if (geometry instanceof MultiPolygon) {
                    polygons = PolygonRepairerService.repair((MultiPolygon) geometry);
                }
                collection = new GeometryFactory().createGeometryCollection(polygons.toArray(new Polygon[polygons.size()]));
            } else if (geometry instanceof GeometryCollection) {
                collection = repairGeometryCollection(((GeometryCollection) geometry));
            }
            collection = removeHoles(collection);
            unionTries++;
        } while (selfIntersection(collection) && unionTries < MAX_UNION_TRIES);
        return collection;

    }


    private static boolean selfIntersection(GeometryCollection geometryCollection) {
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            for (int j = 0; j < i; j++) {
                if (!geometryCollection.getGeometryN(i).disjoint(geometryCollection.getGeometryN(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static GeometryCollection removeHoles(GeometryCollection geometryCollection) {
        ArrayList<Geometry> newGeometryList = new ArrayList<>();
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            if (geometryCollection.getGeometryN(i) instanceof Polygon && ((Polygon) geometryCollection.getGeometryN(i)).getNumInteriorRing() > 0) {
                Coordinate[] coordinates = ((Polygon) geometryCollection.getGeometryN(i)).getExteriorRing().getCoordinates();
                LinearRing linearRing = new GeometryFactory().createLinearRing(coordinates);
                newGeometryList.add(new GeometryFactory().createPolygon(linearRing));
            } else {
                newGeometryList.add(geometryCollection.getGeometryN(i));
            }
        }
        return new GeometryFactory().createGeometryCollection(newGeometryList.toArray(new Geometry[newGeometryList.size()]));
    }

    private static GeometryCollection repairGeometryCollection(GeometryCollection collection) {
        ArrayList<Geometry> geometries = new ArrayList<>();
        for (int i = 0; i < collection.getNumGeometries(); i++) {
            if (collection.getGeometryN(i) instanceof Polygon) {
                geometries.addAll(PolygonRepairerService.repair((Polygon) collection.getGeometryN(i)));
            } else if (collection.getGeometryN(i) instanceof MultiPolygon) {
                geometries.addAll(PolygonRepairerService.repair((MultiPolygon) collection.getGeometryN(i)));
            } else {
                geometries.add(collection.getGeometryN(i));
            }
        }
        return new GeometryFactory().createGeometryCollection(geometries.toArray(new Geometry[geometries.size()]));
    }


}
