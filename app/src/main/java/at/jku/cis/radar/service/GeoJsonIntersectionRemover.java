package at.jku.cis.radar.service;

import android.util.Log;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.TopologyException;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;
import at.jku.cis.radar.transformer.Geometry2GeoJsonGeometryTransformer;

public class GeoJsonIntersectionRemover {
    private static final String TAG = "GeoJsonIntersectionRemover";

    private Iterable<GeoJsonFeature> features;
    private GeoJsonGeometry geoJsonIntersectionGeometry;

    private List<GeoJsonFeature> addList = new ArrayList<>();
    private List<GeoJsonFeature> removeList = new ArrayList<>();

    public GeoJsonIntersectionRemover(Iterable<GeoJsonFeature> features, GeoJsonGeometry geoJsonIntersectionGeometry) {
        this.features = features;
        this.geoJsonIntersectionGeometry = geoJsonIntersectionGeometry;
    }

    public List<GeoJsonFeature> getAddList() {
        return addList;
    }

    public List<GeoJsonFeature> getRemoveList() {
        return removeList;
    }

    public void intersectGeoJsonFeatures() {
        Geometry geometryToIntersection = transformToGeometries(geoJsonIntersectionGeometry);

        for (GeoJsonFeature feature : features) {
            Collection<Geometry> geometries = transformToGeometries(feature);
            Collection<Geometry> newGeometries = intersectionGeometries(geometryToIntersection, geometries);
            GeoJsonFeature newFeature = transformToGeoJsonFeature(newGeometries, feature.getPolygonStyle().getFillColor());
            addList.add(newFeature);
            removeList.add(feature);
        }
    }

    private List<Geometry> intersectionGeometries(Geometry geometryToIntersection, Collection<Geometry> geometries) {
        List<Geometry> geometryList = new ArrayList<>();

        for (Geometry geometry : geometries) {
            if (geometry.intersects(geometryToIntersection)) {
                Geometry intersectionGeometry = null;
                try {
                    intersectionGeometry = geometry.difference(geometryToIntersection);
                } catch (TopologyException e) {
                    Log.e(TAG, "Could not intersect geometry", e);
                }

                if (intersectionGeometry instanceof Polygon) {
                    List<Polygon> polygons = PolygonRepairerService.repair((Polygon) intersectionGeometry);
                    geometryList.add(createMultiPolygon(polygons));
                } else if (intersectionGeometry instanceof MultiPolygon) {
                    List<Polygon> polygons = PolygonRepairerService.repair((MultiPolygon) intersectionGeometry);
                    geometryList.add(createMultiPolygon(polygons));
                }

                if (intersectionGeometry instanceof Polygonal && !intersectionGeometry.isEmpty()) {
                    geometryList.add(intersectionGeometry);
                }
            } else {
                geometryList.add(geometry);
            }
        }
        return geometryList;
    }

    private Collection<Geometry> transformToGeometries(GeoJsonFeature feature) {
        GeoJsonGeometryCollection geometry = (GeoJsonGeometryCollection) feature.getGeometry();
        return CollectionUtils.collect(geometry.getGeometries(), new GeoJsonGeometry2GeometryTransformer());
    }

    private GeoJsonFeature transformToGeoJsonFeature(Collection<Geometry> geometries, int color) {
        Collection<GeoJsonGeometry> geoJsonGeometries = CollectionUtils.collect(geometries, new Geometry2GeoJsonGeometryTransformer());
        GeoJsonGeometryCollection geoJsonGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<>(geoJsonGeometries));
        GeoJsonFeatureBuilder geoJsonFeatureBuilder = new GeoJsonFeatureBuilder(geoJsonGeometryCollection);
        return geoJsonFeatureBuilder.setColor(color).build();
    }

    private MultiPolygon createMultiPolygon(List<Polygon> polygons) {
        return new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
    }

    private Geometry transformToGeometries(GeoJsonGeometry geoJsonEraseGeometry) {
        return new GeoJsonGeometry2GeometryTransformer().transform(geoJsonEraseGeometry);
    }
}
