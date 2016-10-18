package at.jku.cis.radar.geometry;

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
    private final static int MAX_UNION_TRIES = 100;

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
            if(!selfIntersection(collection)){
                break;
            }
        } while (unionTries < MAX_UNION_TRIES);
        if(selfIntersection(collection)){
            collection = convexHull(collection);
        }
        return collection;

    }

    private static GeometryCollection convexHull(Geometry geometry){
        Geometry[] geometries = new Geometry[geometry.getNumGeometries()];
        for(int i = 0; i < geometry.getNumGeometries(); i++){
            Geometry geometry1 = geometry.getGeometryN(i);
            if(geometry1.getNumGeometries() > 1){
                convexHull(geometry1);
            } else{
                geometry1 = geometry1.convexHull();
            }
            geometries[i] = geometry1;
        }
        return new GeometryCollection(geometries, new GeometryFactory());
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
