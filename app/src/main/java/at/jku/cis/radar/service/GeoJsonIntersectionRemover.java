package at.jku.cis.radar.service;

import android.util.Log;

import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;
import at.jku.cis.radar.transformer.Geometry2GeoJsonGeometryTransformer;

public class GeoJsonIntersectionRemover {
    private static final String TAG = "IntersectionRemover";

    private Iterable<GeoJsonFeature> features;
    private GeoJsonGeometry geoJsonIntersectionGeometry;

    private List<GeoJsonFeature> addList = new ArrayList<>();
    private List<GeoJsonFeature> removeList = new ArrayList<>();

    public GeoJsonIntersectionRemover(Iterable<GeoJsonFeature> features, GeoJsonGeometry geoJsonIntersectionGeometry) {
        this.features = features;
        this.geoJsonIntersectionGeometry = geoJsonIntersectionGeometry;
    }
    

    public void intersectGeoJsonFeatures() {
        List<Geometry> geometriesToIntersection = transformToGeometries(geoJsonIntersectionGeometry);

        for (GeoJsonFeature feature : features) {
            Collection<Geometry> geometries = transformToGeometries(feature);
            for (Geometry geometryToIntersection : geometriesToIntersection) {
                geometries = intersectionGeometries(geometryToIntersection, geometries);
            }
            if (!geometries.isEmpty()) {
                addList.add(transformToGeoJsonFeature(geometries, feature.getPolygonStyle().getFillColor(), feature.getId()));
            }
            removeList.add(feature);
        }
    }

    private List<Geometry> intersectionGeometries(Geometry geometryToIntersection, Collection<Geometry> geometries) {
        List<Geometry> geometryList = new ArrayList<>();

        for (Geometry geometry : geometries) {
            try {
                if (geometry.intersects(geometryToIntersection)) {
                    Geometry intersectionGeometry = geometry.difference(geometryToIntersection);
                    if (intersectionGeometry instanceof Polygon && !intersectionGeometry.isEmpty()) {
                        geometryList.add(createMultiPolygon(PolygonRepairerService.repair((Polygon) intersectionGeometry)));
                    } else if (intersectionGeometry instanceof MultiPolygon && !intersectionGeometry.isEmpty()) {
                        geometryList.add(createMultiPolygon(PolygonRepairerService.repair((MultiPolygon) intersectionGeometry)));
                    }
                } else {
                    geometryList.add(geometry);
                }
            } catch (TopologyException e) {
                Log.e(TAG, "Could not intersect geometry[" + geometry.toString() + "]", e);
            }
        }
        return geometryList;
    }

    private Collection<Geometry> transformToGeometries(GeoJsonFeature feature) {
        GeoJsonGeometryCollection geometry = (GeoJsonGeometryCollection) feature.getGeometry();
        return CollectionUtils.collect(geometry.getGeometries(), new GeoJsonGeometry2GeometryTransformer());
    }

    private GeoJsonFeature transformToGeoJsonFeature(Collection<Geometry> geometries, int color, String id) {
        Collection<GeoJsonGeometry> geoJsonGeometries = CollectionUtils.collect(geometries, new Geometry2GeoJsonGeometryTransformer());
        GeoJsonGeometryCollection geoJsonGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<>(geoJsonGeometries));
        return new GeoJsonFeatureBuilder(geoJsonGeometryCollection).setId(id).setColor(color).build();
    }

    private List<Geometry> transformToGeometries(GeoJsonGeometry geoJsonEraseGeometry) {
        Geometry geometry = new GeoJsonGeometry2GeometryTransformer().transform(geoJsonEraseGeometry);
        if (geometry instanceof Polygon) {
            return Collections.singletonList(geometry);
        }
        return transformToGeometries((MultiPolygon) geometry);
    }

    private List<Geometry> transformToGeometries(MultiPolygon geometry) {
        List<Geometry> geometries = new ArrayList<>();
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            geometries.add(geometry.getGeometryN(i));
        }
        return geometries;
    }

    private MultiPolygon createMultiPolygon(List<Polygon> polygons) {
        return new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
    }

    public List<GeoJsonFeature> getAddList() {
        return addList;
    }

    public List<GeoJsonFeature> getRemoveList() {
        return removeList;
    }
}
