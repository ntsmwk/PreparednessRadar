package at.jku.cis.radar.service;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.TopologyException;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.convert.GeometryTransformator;
import at.jku.cis.radar.geometry.GeoJsonFeatureBuilder;

public class GeoJsonIntersectionRemover {

    Iterable<GeoJsonFeature> features;
    GeoJsonGeometry geoJsonEraseGeometry;
    List<GeoJsonFeature> addList = new ArrayList<>();
    List<GeoJsonFeature> removeList = new ArrayList<>();

    public GeoJsonIntersectionRemover(Iterable<GeoJsonFeature> features, GeoJsonGeometry geoJsonEraseGeometry) {
        this.features = features;
        this.geoJsonEraseGeometry = geoJsonEraseGeometry;
    }

    public void removeIntersectedGeometry() {
        Geometry eraser = GeometryTransformator.transformToGeometry(geoJsonEraseGeometry);
        GeoJsonFeatureBuilder geoJsonFeatureBuilder;
        for (GeoJsonFeature feature : features) {
            GeoJsonGeometryCollection geoJsonGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<GeoJsonGeometry>());
            for (GeoJsonGeometry geoJsonGeometry : ((GeoJsonGeometryCollection) feature.getGeometry()).getGeometries()) {
                Geometry geometry = GeometryTransformator.transformToGeometry(geoJsonGeometry);
                GeoJsonGeometry intersectionGeoJsonGeometry = null;
                if (geometry.intersects(eraser)) {
                    Geometry intersectionGeometry;
                    try {
                        intersectionGeometry = geometry.difference(eraser);
                    } catch (TopologyException e) {
                        continue;
                    }
                    if (intersectionGeometry instanceof Polygon) {
                        intersectionGeoJsonGeometry = createComplexPolygon((Polygon) intersectionGeometry);

                    } else if (intersectionGeometry instanceof MultiPolygon) {
                        intersectionGeoJsonGeometry = createComplexPolygon((MultiPolygon) intersectionGeometry);
                    }
                    if (intersectionGeometry instanceof Polygonal && !intersectionGeometry.isEmpty()) {
                        geoJsonGeometryCollection.getGeometries().add(intersectionGeoJsonGeometry);

                    }
                } else {
                    geoJsonGeometryCollection.getGeometries().add(geoJsonGeometry);
                }
            }
            geoJsonFeatureBuilder = new GeoJsonFeatureBuilder(geoJsonGeometryCollection);
            geoJsonFeatureBuilder.setColor(feature.getPolygonStyle().getFillColor());
            addList.add(geoJsonFeatureBuilder.build());
            removeList.add(feature);
            continue;
        }
    }

    private GeoJsonMultiPolygon createComplexPolygon(Polygonal polygon) {
        List<Polygon> polygons;
        if (polygon instanceof Polygon) {
            polygons = JTSUtils.repair((Polygon) polygon);
        } else {
            polygons = JTSUtils.repair((MultiPolygon) polygon);
        }
        MultiPolygon multiPolygon = new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
        GeoJsonMultiPolygon geoJsonPolygon = (GeoJsonMultiPolygon) GeometryTransformator.transformToGeoJsonGeometry(multiPolygon);
        return geoJsonPolygon;
    }

    public List<GeoJsonFeature> getAddList() {
        return addList;
    }

    public List<GeoJsonFeature> getRemoveList() {
        return removeList;
    }

}
