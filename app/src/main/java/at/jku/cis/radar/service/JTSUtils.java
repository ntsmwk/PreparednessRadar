package at.jku.cis.radar.service;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jts.operation.valid.TopologyValidationError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JTSUtils {

    public static List<Polygon> repair(Polygon polygon) {
        TopologyValidationError topologyValidationError = new IsValidOp(polygon).getValidationError();
        if (isSelfIntersectionErrorTyp(topologyValidationError)) {
            System.out.println(topologyValidationError);
            Geometry boundary = polygon.getBoundary();
            boundary = boundary.union(boundary);
            Polygonizer polygonizer = new Polygonizer();
            polygonizer.add(boundary);
            List<Polygon> newPolygons = new ArrayList<>();
            for (Object newPolygon : polygonizer.getPolygons()) {
                if (newPolygon != null) {
                    newPolygons.addAll(repair((Polygon) newPolygon));
                }
            }
            return newPolygons;
        }
        return Arrays.asList(polygon);
    }

    private static boolean isSelfIntersectionErrorTyp(TopologyValidationError topologyValidationError) {
        return topologyValidationError != null && (TopologyValidationError.SELF_INTERSECTION == topologyValidationError.getErrorType() ||
                topologyValidationError.getErrorType() == TopologyValidationError.RING_SELF_INTERSECTION ||
                topologyValidationError.getErrorType() == TopologyValidationError.DISCONNECTED_INTERIOR);
    }

    public static List<Polygon> repair(MultiPolygon multiPolygon) {
        ArrayList<Polygon> polygons = new ArrayList<>();
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            polygons.addAll(repair((Polygon) multiPolygon.getGeometryN(i)));
        }
        return polygons;
    }
}
