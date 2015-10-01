package at.jku.cis.radar.geometry;

import android.graphics.Color;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;

import java.util.ArrayList;

import at.jku.cis.radar.service.PolygonRepairerService;


public class GeometryUtils {
    private final static float EDIT_LINE_WIDTH = 10;
    private final static float NORMAL_LINE_WIDTH = 10;
    private final static int EDIT_COLOR = Color.WHITE;


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
        ArrayList<Geometry> unionList = unionRec(geometryCollection);
        if (unionList == null) {
            return geometryCollection;
        }
        GeometryCollection collection = new GeometryFactory().createGeometryCollection(unionList.toArray(new Geometry[unionList.size()]));
        while (selfIntersection(unionList)) {
            unionList = unionRec(collection);
        }
        collection = new GeometryFactory().createGeometryCollection(unionList.toArray(new Geometry[unionList.size()]));
        collection = repairGeometryCollection(collection);
        return collection;
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

    private static ArrayList<Geometry> unionRec(GeometryCollection geometryCollection) {
        ArrayList<Geometry> geometries = new ArrayList<>();
        if (geometryCollection.getNumGeometries() == 1) {
            return null;
        }
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            boolean intersected = false;
            for (int j = 0; j < geometryCollection.getNumGeometries(); j++) {
                if (i != j) {
                    try {
                        if (geometryCollection.getGeometryN(i).intersects(geometryCollection.getGeometryN(j))) {
                            Geometry unionGeometry = geometryCollection.getGeometryN(i).union(geometryCollection.getGeometryN(j));
                            if (!inList(geometries, unionGeometry)) {
                                geometries.add(geometryCollection.getGeometryN(i).union(geometryCollection.getGeometryN(j)));
                            }
                            intersected = true;
                        }
                    } catch (TopologyException e) {
                    }
                }
            }
            if (!intersected) {
                if (!inList(geometries, geometryCollection.getGeometryN(i))) {
                    geometries.add(geometryCollection.getGeometryN(i));
                }
            }
        }
        return geometries;
    }

    private static boolean inList(ArrayList<Geometry> geometries, Geometry geometry) {
        for (int i = 0; i < geometries.size(); i++) {
            if (geometries.get(i).equalsTopo(geometry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean selfIntersection(ArrayList<Geometry> geometries) {
        for (int i = 0; i < geometries.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (!geometries.get(i).disjoint(geometries.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }
}
