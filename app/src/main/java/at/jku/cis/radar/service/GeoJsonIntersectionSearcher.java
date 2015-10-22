package at.jku.cis.radar.service;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;

public class GeoJsonIntersectionSearcher {

    private GeoJsonGeometry2GeometryTransformer geoJsonGeometry2GeometryTransformer = new GeoJsonGeometry2GeometryTransformer();

    public List<GeoJsonFeature> searchForIntersection(List<GeoJsonFeature> geoJsonFeatures, GeoJsonGeometry geoJsonGeometry) {
        List<GeoJsonFeature> featureList = new ArrayList<>();
        Geometry point = transformGeometry(geoJsonGeometry);
        for (GeoJsonFeature geoJsonFeature : geoJsonFeatures) {
            GeometryCollection geometryCollection = transformGeometryCollection(geoJsonFeature.getGeometry());
            if (intersects(geometryCollection, point)) {
                featureList.add(geoJsonFeature);
            }
        }
        return featureList;
    }

    private boolean intersects(GeometryCollection geometryCollection, Geometry intersectionGeometry) {
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            if (geometryCollection.getGeometryN(i).intersects(intersectionGeometry)) {
                return true;
            }
        }
        return false;
    }


    private GeometryCollection transformGeometryCollection(GeoJsonGeometry geoJsonGeometry) {
        return (GeometryCollection) geoJsonGeometry2GeometryTransformer.transform(geoJsonGeometry);
    }

    private Geometry transformGeometry(GeoJsonGeometry geoJsonGeometry) {
        return geoJsonGeometry2GeometryTransformer.transform(geoJsonGeometry);
    }
}
