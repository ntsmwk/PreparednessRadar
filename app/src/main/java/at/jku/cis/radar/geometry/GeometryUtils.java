package at.jku.cis.radar.geometry;

import android.graphics.Color;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

public class GeometryUtils {
    private final static float EDIT_LINE_WIDTH = 20;
    private final static float NORMAL_LINE_WIDTH = 10;


    public static void setEditableFeature(GeoJsonFeature feature) {
        for (GeoJsonGeometry geometry : ((GeoJsonGeometryCollection) feature.getGeometry()).getGeometries()) {
            if (geometry instanceof GeoJsonLineString) {
                setLineStringStyle(feature, Color.CYAN, EDIT_LINE_WIDTH);
            }
            if (geometry instanceof GeoJsonPolygon || geometry instanceof GeoJsonMultiPolygon) {
                setPolygonStyle(feature, Color.CYAN, EDIT_LINE_WIDTH);
            }
            if (geometry instanceof GeoJsonPoint) {
                //TODO
            }
        }
    }

    public static void setNotEditableFeature(GeoJsonFeature feature) {
        for (GeoJsonGeometry geometry : ((GeoJsonGeometryCollection) feature.getGeometry()).getGeometries()) {
            if (geometry instanceof GeoJsonLineString) {
                setLineStringStyle(feature, Color.BLACK, NORMAL_LINE_WIDTH);
            }
            if (geometry instanceof GeoJsonPolygon || geometry instanceof GeoJsonMultiPolygon) {
                setPolygonStyle(feature, Color.BLACK, NORMAL_LINE_WIDTH);
            }
            if (geometry instanceof GeoJsonPoint) {
                //TODO
            }
        }
    }

    public static boolean intersects(GeometryCollection geometryCollection, Geometry intersectionGeometry) {
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            if (geometryCollection.getGeometryN(i).intersects(intersectionGeometry)) {
                return true;
            }
        }
        return false;
    }

    private static void setPolygonStyle(GeoJsonFeature feature, int color, float lineWidth) {
        feature.getPolygonStyle().setStrokeColor(color);
        feature.getPolygonStyle().setStrokeWidth(lineWidth);
    }

    private static void setLineStringStyle(GeoJsonFeature feature, int color, float lineWidth) {
        feature.getLineStringStyle().setColor(color);
        feature.getLineStringStyle().setWidth(lineWidth);
    }
}
