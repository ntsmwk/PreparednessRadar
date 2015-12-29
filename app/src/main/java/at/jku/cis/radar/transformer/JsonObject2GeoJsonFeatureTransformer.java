package at.jku.cis.radar.transformer;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonMultiLineString;
import com.google.maps.android.geojson.GeoJsonMultiPoint;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPolygon;

import org.apache.commons.collections4.Transformer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class JsonObject2GeoJsonFeatureTransformer implements Transformer<JSONObject, List<GeoJsonFeature>> {
    private static final String TAG = JsonObject2GeoJsonFeatureTransformer.class.getName();

    private static final String FEATURE = "Feature";
    private static final String FEATURE_GEOMETRY = "geometry";
    private static final String FEATURE_ID = "id";
    private static final String FEATURE_COLLECTION = "FeatureCollection";
    private static final String FEATURE_COLLECTION_ARRAY = "features";
    private static final String GEOMETRY_COORDINATES_ARRAY = "coordinates";
    private static final String GEOMETRY_COLLECTION = "GeometryCollection";
    private static final String GEOMETRY_COLLECTION_ARRAY = "geometries";
    private static final String BOUNDING_BOX = "bbox";
    private static final String PROPERTIES = "properties";
    private static final String POINT = "Point";
    private static final String MULTIPOINT = "MultiPoint";
    private static final String LINESTRING = "LineString";
    private static final String MULTILINESTRING = "MultiLineString";
    private static final String POLYGON = "Polygon";
    private static final String MULTIPOLYGON = "MultiPolygon";

    @Override
    public List<GeoJsonFeature> transform(JSONObject input) {
        List<GeoJsonFeature> geoJsonFeatures = new ArrayList<>();
        try {
            String type = input.getString("type");
            GeoJsonFeature geoJsonFeature;
            if (type.equals(FEATURE)) {
                geoJsonFeature = parseFeature(input);
                if (geoJsonFeature != null) {
                    geoJsonFeatures.add(geoJsonFeature);
                }
            } else if (type.equals(FEATURE_COLLECTION)) {
                geoJsonFeatures.addAll(parseFeatureCollection(input));
            } else if (isGeometry(type)) {
                geoJsonFeature = parseGeometryToFeature(input);
                if (geoJsonFeature != null) {
                    geoJsonFeatures.add(geoJsonFeature);
                }
            } else {
                Log.w(TAG, "GeoJson file could not be parsed.");
            }
        } catch (JSONException var3) {
            Log.w(TAG, "GeoJson file could not be parsed.");
        }
        return geoJsonFeatures;
    }

    private GeoJsonFeature parseGeometryToFeature(JSONObject geoJsonGeometry) {
        GeoJsonGeometry geometry = parseGeometry(geoJsonGeometry);
        if (geometry != null) {
            return new GeoJsonFeature(geometry, (String) null, new HashMap(), (LatLngBounds) null);
        } else {
            Log.w("GeoJsonParser", "Geometry could not be parsed");
            return null;
        }
    }

    private List<GeoJsonFeature> parseFeatureCollection(JSONObject geoJsonFeatureCollection) {
        ArrayList features = new ArrayList();

        try {
            JSONArray geoJsonFeatures = geoJsonFeatureCollection.getJSONArray("features");
            for (int i = 0; i < geoJsonFeatures.length(); ++i) {
                try {
                    JSONObject e = geoJsonFeatures.getJSONObject(i);
                    if (e.getString("type").equals(FEATURE)) {
                        GeoJsonFeature parsedFeature = parseFeature(e);
                        if (parsedFeature != null) {
                            features.add(parsedFeature);
                        } else {
                            Log.w(TAG, "Index of Feature in Feature Collection that could not be created: " + i);
                        }
                    }
                } catch (JSONException ex) {
                    Log.w(TAG, "Index of Feature in Feature Collection that could not be created: " + i);
                }
            }
        } catch (JSONException ex) {
            Log.w(TAG, "Feature Collection could not be created.");
        }
        return features;
    }

    private GeoJsonFeature parseFeature(JSONObject geoJsonFeature) {
        String id = null;
        GeoJsonGeometry geometry = null;
        HashMap<String, String> properties = new HashMap();

        try {
            if (geoJsonFeature.has("id")) {
                id = geoJsonFeature.getString("id");
            }

            if (geoJsonFeature.has("geometry") && !geoJsonFeature.isNull("geometry")) {
                geometry = parseGeometry(geoJsonFeature.getJSONObject("geometry"));
            }

            if (geoJsonFeature.has("properties") && !geoJsonFeature.isNull("properties")) {
                properties = parseProperties(geoJsonFeature.getJSONObject("properties"));
            }
        } catch (JSONException var6) {
            Log.w("GeoJsonParser", "Feature could not be successfully parsed " + geoJsonFeature.toString());
            return null;
        }

        return new GeoJsonFeature(geometry, id, properties, null);
    }

    private HashMap<String, String> parseProperties(JSONObject properties) throws JSONException {
        HashMap<String, String> propertiesMap = new HashMap();
        Iterator propertyKeys = properties.keys();

        while (propertyKeys.hasNext()) {
            String key = (String) propertyKeys.next();
            propertiesMap.put(key, properties.getString(key));
        }

        return propertiesMap;
    }

    private GeoJsonGeometry parseGeometry(JSONObject geoJsonGeometry) {
        try {
            String e = geoJsonGeometry.getString("type");
            JSONArray geometryArray;
            if (e.equals("GeometryCollection")) {
                geometryArray = geoJsonGeometry.getJSONArray("geometries");
            } else {
                if (!isGeometry(e)) {
                    return null;
                }

                geometryArray = geoJsonGeometry.getJSONArray("coordinates");
            }

            return createGeometry(e, geometryArray);
        } catch (JSONException var3) {
            return null;
        }
    }

    private GeoJsonGeometry createGeometry(String geometryType, JSONArray geometryArray) throws JSONException {
        return (GeoJsonGeometry) (geometryType.equals("Point") ? createPoint(geometryArray) : (geometryType.equals("MultiPoint") ? createMultiPoint(geometryArray) : (geometryType.equals("LineString") ? createLineString(geometryArray) : (geometryType.equals("MultiLineString") ? createMultiLineString(geometryArray) : (geometryType.equals("Polygon") ? createPolygon(geometryArray) : (geometryType.equals("MultiPolygon") ? createMultiPolygon(geometryArray) : (geometryType.equals("GeometryCollection") ? createGeometryCollection(geometryArray) : null)))))));
    }

    private GeoJsonPoint createPoint(JSONArray coordinates) throws JSONException {
        return new GeoJsonPoint(parseCoordinate(coordinates));
    }

    private GeoJsonMultiPoint createMultiPoint(JSONArray coordinates) throws JSONException {
        ArrayList geoJsonPoints = new ArrayList();

        for (int i = 0; i < coordinates.length(); ++i) {
            geoJsonPoints.add(createPoint(coordinates.getJSONArray(i)));
        }

        return new GeoJsonMultiPoint(geoJsonPoints);
    }

    private GeoJsonLineString createLineString(JSONArray coordinates) throws JSONException {
        return new GeoJsonLineString(parseCoordinatesArray(coordinates));
    }

    private GeoJsonMultiLineString createMultiLineString(JSONArray coordinates) throws JSONException {
        ArrayList geoJsonLineStrings = new ArrayList();

        for (int i = 0; i < coordinates.length(); ++i) {
            geoJsonLineStrings.add(createLineString(coordinates.getJSONArray(i)));
        }

        return new GeoJsonMultiLineString(geoJsonLineStrings);
    }

    private GeoJsonPolygon createPolygon(JSONArray coordinates) throws JSONException {
        return new GeoJsonPolygon(parseCoordinatesArrays(coordinates));
    }

    private GeoJsonMultiPolygon createMultiPolygon(JSONArray coordinates) throws JSONException {
        ArrayList geoJsonPolygons = new ArrayList();

        for (int i = 0; i < coordinates.length(); ++i) {
            geoJsonPolygons.add(createPolygon(coordinates.getJSONArray(i)));
        }

        return new GeoJsonMultiPolygon(geoJsonPolygons);
    }

    private GeoJsonGeometryCollection createGeometryCollection(JSONArray geometries) throws JSONException {
        ArrayList geometryCollectionElements = new ArrayList();

        for (int i = 0; i < geometries.length(); ++i) {
            JSONObject geometryElement = geometries.getJSONObject(i);
            GeoJsonGeometry geometry = parseGeometry(geometryElement);
            if (geometry != null) {
                geometryCollectionElements.add(geometry);
            }
        }

        return new GeoJsonGeometryCollection(geometryCollectionElements);
    }

    private LatLng parseCoordinate(JSONArray coordinates) throws JSONException {
        return new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
    }

    private ArrayList<LatLng> parseCoordinatesArray(JSONArray coordinates) throws JSONException {
        ArrayList coordinatesArray = new ArrayList();

        for (int i = 0; i < coordinates.length(); ++i) {
            coordinatesArray.add(parseCoordinate(coordinates.getJSONArray(i)));
        }

        return coordinatesArray;
    }

    private ArrayList<ArrayList<LatLng>> parseCoordinatesArrays(JSONArray coordinates) throws JSONException {
        ArrayList coordinatesArray = new ArrayList();

        for (int i = 0; i < coordinates.length(); ++i) {
            coordinatesArray.add(parseCoordinatesArray(coordinates.getJSONArray(i)));
        }

        return coordinatesArray;
    }


    private boolean isGeometry(String type) {
        return type.matches(POINT + "|" + MULTIPOINT + "|" + LINESTRING + "|" + MULTILINESTRING + "|" + POLYGON + "|" + MULTIPOLYGON + "|" + GEOMETRY_COLLECTION);
    }
}


