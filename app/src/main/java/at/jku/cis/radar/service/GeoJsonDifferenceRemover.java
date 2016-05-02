package at.jku.cis.radar.service;

import android.graphics.Color;
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
import java.util.HashMap;
import java.util.List;

import at.jku.cis.radar.transformer.GeoJsonGeometry2GeometryTransformer;
import at.jku.cis.radar.transformer.Geometry2GeoJsonGeometryTransformer;
import at.jku.cis.radar.view.GoogleView;

public class GeoJsonDifferenceRemover {
    private static final String TAG = "DifferenceRemover";

    private Iterable<GeoJsonFeature> features;
    private GeoJsonGeometry geoJsonRemoveGeometry;

    private List<GeoJsonFeature> addList = new ArrayList<>();
    private List<GeoJsonFeature> removeList = new ArrayList<>();
    private List<GeoJsonFeature> prevList = new ArrayList<>();
    private List<GeoJsonFeature> intersectionList = new ArrayList<>();

    public GeoJsonDifferenceRemover(Iterable<GeoJsonFeature> features, GeoJsonGeometry geoJsonRemoveGeometry) {
        this.features = features;
        this.geoJsonRemoveGeometry = geoJsonRemoveGeometry;
    }

    public GeoJsonDifferenceRemover(GeoJsonFeature feature, GeoJsonGeometry geoJsonRemoveGeometry) {
        this(Collections.singletonList(feature), geoJsonRemoveGeometry);
    }


    public void removeDifference() {
        HashMap<String, String> addProperties = new HashMap<>();
        HashMap<String, String> removeProperties = new HashMap<>();
        removeProperties.put(GoogleView.STATUS_PROPERTY_NAME, GoogleView.STATUS_ERASED);
        addProperties.put(GoogleView.STATUS_PROPERTY_NAME, GoogleView.STATUS_CREATED);

        Geometry removeGeometry = new GeoJsonGeometry2GeometryTransformer().transform(geoJsonRemoveGeometry);

        for (int i = 0; i < removeGeometry.getNumGeometries(); i++) {
            for (GeoJsonFeature feature : features) {

                Collection<Geometry> geometries = transformToGeometries(feature);
                Collection<Geometry> addGeometries = differenceGeometry(removeGeometry.getGeometryN(i), geometries);
                Collection<Geometry> intersectionGeometries = intersectionGeometry(removeGeometry.getGeometryN(i), geometries);
                if (!addGeometries.isEmpty()) {
                    addList.add(transformToGeoJsonFeature(addGeometries, feature.getPolygonStyle().getFillColor(), feature.getId(), addProperties));
                }
                if(!intersectionGeometries.isEmpty()){
                    intersectionList.add(transformToGeoJsonFeature(intersectionGeometries, Color.GRAY, feature.getId(), removeProperties));
                }
                removeList.add(buildFeature(new GeoJsonGeometryCollection(Collections.singletonList(geoJsonRemoveGeometry)), removeProperties, Color.TRANSPARENT, feature.getId()));
                prevList.add(feature);
            }
        }
    }

    private List<Geometry> differenceGeometry(Geometry removeGeometry, Collection<Geometry> geometries) {
        List<Geometry> geometryList = new ArrayList<>();

        for (Geometry geometry : geometries) {
            try {
                if (geometry.intersects(removeGeometry)) {
                    Geometry differenceGeometry = geometry.difference(removeGeometry);
                    if (differenceGeometry instanceof Polygon && !differenceGeometry.isEmpty()) {
                        geometryList.add(createMultiPolygon(PolygonRepairerService.repair((Polygon) differenceGeometry)));
                    } else if (differenceGeometry instanceof MultiPolygon && !differenceGeometry.isEmpty()) {
                        geometryList.add(createMultiPolygon(PolygonRepairerService.repair((MultiPolygon) differenceGeometry)));
                    }
                } else {
                    geometryList.add(geometry);
                }
            } catch (TopologyException e) {
                Log.e(TAG, "Could not calculate difference geometry[" + geometry.toString() + "]", e);
            }
        }
        return geometryList;
    }

    private List<Geometry> intersectionGeometry(Geometry removeGeometry, Collection<Geometry> geometries){
        List<Geometry> geometryList = new ArrayList<>();

        for(Geometry geometry : geometries){
            Geometry intersectionGeometry = geometry.intersection(removeGeometry);
            if (intersectionGeometry instanceof Polygon && !intersectionGeometry.isEmpty()) {
                geometryList.add(createMultiPolygon(PolygonRepairerService.repair((Polygon) intersectionGeometry)));
            } else if (intersectionGeometry instanceof MultiPolygon && !intersectionGeometry.isEmpty()) {
                geometryList.add(createMultiPolygon(PolygonRepairerService.repair((MultiPolygon) intersectionGeometry)));
            }
        }
        return geometryList;
    }

    private Collection<Geometry> transformToGeometries(GeoJsonFeature feature) {
        GeoJsonGeometryCollection geometry = (GeoJsonGeometryCollection) feature.getGeometry();
        return CollectionUtils.collect(geometry.getGeometries(), new GeoJsonGeometry2GeometryTransformer());
    }

    private GeoJsonFeature transformToGeoJsonFeature(Collection<Geometry> geometries, int color, String id, HashMap<String, String> properties) {
        Collection<GeoJsonGeometry> geoJsonGeometries = CollectionUtils.collect(geometries, new Geometry2GeoJsonGeometryTransformer());
        GeoJsonGeometryCollection geoJsonGeometryCollection = new GeoJsonGeometryCollection(new ArrayList<>(geoJsonGeometries));
        return new GeoJsonFeatureBuilder(geoJsonGeometryCollection).setColor(color).setProperties(properties).build(id);
    }

    private MultiPolygon createMultiPolygon(List<Polygon> polygons) {
        return new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
    }

    private GeoJsonFeature buildFeature(GeoJsonGeometryCollection geometry, HashMap<String, String> properties, int color, String id) {
        return new GeoJsonFeatureBuilder(geometry).setColor(color).setProperties(properties).build(id);
    }

    public List<GeoJsonFeature> getAddList() {
        return addList;
    }

    public List<GeoJsonFeature> getPrevList() {
        return prevList;
    }

    public List<GeoJsonFeature> getRemoveList() {
        return removeList;
    }

    public List<GeoJsonFeature> getIntersectionList(){
        return intersectionList;
    }

}
