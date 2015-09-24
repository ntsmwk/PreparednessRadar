package at.jku.cis.radar.service;

import com.google.android.gms.maps.Projection;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.TopologyException;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.convert.GeometryTransformator;

public class GeoJsonIntersectionRemover {

    Iterable<GeoJsonFeature> features;
    GeoJsonGeometry geoJsonEraseGeometry;
    List<GeoJsonFeature> addList = new ArrayList<>();
    List<GeoJsonFeature> removeList = new ArrayList<>();

    public GeoJsonIntersectionRemover(Iterable<GeoJsonFeature> features, GeoJsonGeometry geoJsonEraseGeometry) {
        this.features = features;
        this.geoJsonEraseGeometry = geoJsonEraseGeometry;

    }

    public void removeIntersectedGeometry(Projection projection) {
        Geometry eraser = GeometryTransformator.transformToGeometry(geoJsonEraseGeometry);
        GeoJsonFeatureBuilder geoJsonFeatureBuilder;
        for (GeoJsonFeature feature : features) {
            Geometry line = GeometryTransformator.transformToGeometry(feature.getGeometry());
            GeoJsonGeometry intersectionGeoJsonGeometry = null;
            if (line.intersects(eraser)) {
                Geometry intersectionGeometry;
                try {
                    intersectionGeometry = line.difference(eraser);
                } catch (TopologyException e) {
                    continue;
                }
                if (intersectionGeometry instanceof Polygon) {
                    intersectionGeoJsonGeometry = createComplexPolygon((Polygon) intersectionGeometry, projection);

                } else if(intersectionGeometry instanceof  MultiPolygon){
                    intersectionGeoJsonGeometry = createComplexPolygon((MultiPolygon) intersectionGeometry, projection);
                }
                if (intersectionGeometry instanceof Polygonal && !intersectionGeometry.isEmpty()){
                    geoJsonFeatureBuilder = new GeoJsonFeatureBuilder(intersectionGeoJsonGeometry);
                    geoJsonFeatureBuilder.setColor(feature.getPolygonStyle().getFillColor());
                    addList.add(geoJsonFeatureBuilder.build());
                }
                removeList.add(feature);
                continue;
            }
        }
    }

    private GeoJsonMultiPolygon createComplexPolygon(Polygonal polygon, Projection projection) {
        List<Polygon> polygons = null;
        if(polygon instanceof Polygon) {
            polygons = JTSUtils.repair((Polygon)polygon);
        } else{
            polygons = JTSUtils.repair((MultiPolygon)polygon);
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
