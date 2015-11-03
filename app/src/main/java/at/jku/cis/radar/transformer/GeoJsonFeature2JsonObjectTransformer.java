package at.jku.cis.radar.transformer;

import com.google.android.gms.maps.model.LatLng;
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

import java.util.List;

public class GeoJsonFeature2JsonObjectTransformer implements Transformer<GeoJsonFeature, JSONObject> {
    private static final String TYPE = "type";
    private static final String FEATURE = "Feature";

    private static final String FEATURE_ID = "id";
    private static final String FEATURE_GEOMETRY = "geometry";
    private static final String FEATURE_PROPERTIES = "properties";

    private static final String GEOMETRY_COORDINATES_ARRAY = "coordinates";
    private static final String GEOMETRY_COLLECTION_ARRAY = "geometries";

    @Override
    public JSONObject transform(GeoJsonFeature geoJsonFeature) {
        try {
            return transformFeature(geoJsonFeature);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONObject transformFeature(GeoJsonFeature geoJsonFeature) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TYPE, FEATURE);
        jsonObject.put(FEATURE_ID, geoJsonFeature.getId());
        jsonObject.put(FEATURE_GEOMETRY, transformGeometry(geoJsonFeature.getGeometry()));
        jsonObject.put(FEATURE_PROPERTIES, transformProperties(geoJsonFeature));
        return jsonObject;
    }

    private JSONObject  transformGeometry(GeoJsonGeometry geoJsonGeometry) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TYPE, geoJsonGeometry.getType());
        if (geoJsonGeometry instanceof GeoJsonPoint) {
            jsonObject.put(GEOMETRY_COORDINATES_ARRAY, transformPoint((GeoJsonPoint) geoJsonGeometry));
        } else if (geoJsonGeometry instanceof GeoJsonMultiPoint) {
            jsonObject.put(GEOMETRY_COORDINATES_ARRAY, transformMultiPoint((GeoJsonMultiPoint) geoJsonGeometry));
        } else if (geoJsonGeometry instanceof GeoJsonLineString) {
            jsonObject.put(GEOMETRY_COORDINATES_ARRAY, transformLineString((GeoJsonLineString) geoJsonGeometry));
        } else if (geoJsonGeometry instanceof GeoJsonMultiLineString) {
            jsonObject.put(GEOMETRY_COORDINATES_ARRAY, transformMultiLineString((GeoJsonMultiLineString) geoJsonGeometry));
        } else if (geoJsonGeometry instanceof GeoJsonPolygon) {
            jsonObject.put(GEOMETRY_COORDINATES_ARRAY, transformPolygon((GeoJsonPolygon) geoJsonGeometry));
        } else if (geoJsonGeometry instanceof GeoJsonMultiPolygon) {
            jsonObject.put(GEOMETRY_COORDINATES_ARRAY, transformMultiPolygon((GeoJsonMultiPolygon) geoJsonGeometry));
        } else if (geoJsonGeometry instanceof GeoJsonGeometryCollection) {
            jsonObject.put(GEOMETRY_COLLECTION_ARRAY, transformGeometryCollection((GeoJsonGeometryCollection) geoJsonGeometry));
        }
        return jsonObject;
    }

    private JSONObject transformProperties(GeoJsonFeature geoJsonFeature) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (String key : geoJsonFeature.getPropertyKeys()) {
            jsonObject.put(key, geoJsonFeature.getProperty(key));
        }
        return jsonObject;
    }

    private JSONArray transformPoint(GeoJsonPoint geoJsonPoint) throws JSONException {
        return transformCoordinate(geoJsonPoint.getCoordinates());
    }


    private JSONArray transformMultiPoint(GeoJsonMultiPoint geoJsonMultiPoint) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (GeoJsonPoint geoJsonPoint : geoJsonMultiPoint.getPoints()) {
            jsonArray.put(transformPoint(geoJsonPoint));
        }
        return jsonArray;
    }

    private JSONArray transformLineString(GeoJsonLineString geoJsonLineString) throws JSONException {
        return transformCoordinates(geoJsonLineString.getCoordinates());
    }


    private JSONArray transformMultiLineString(GeoJsonMultiLineString geoJsonMultiLineString) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (GeoJsonLineString geoJsonLineString : geoJsonMultiLineString.getLineStrings()) {
            jsonArray.put(transformLineString(geoJsonLineString));
        }
        return jsonArray;
    }

    private JSONArray transformPolygon(GeoJsonPolygon geoJsonPolygon) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (List<LatLng> latLngs : geoJsonPolygon.getCoordinates()) {
            jsonArray.put(transformCoordinates(latLngs));
        }
        return jsonArray;
    }

    private JSONArray transformMultiPolygon(GeoJsonMultiPolygon geoJsonMultiPolygon) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (GeoJsonPolygon geoJsonPolygon : geoJsonMultiPolygon.getPolygons()) {
            jsonArray.put(transformPolygon(geoJsonPolygon));
        }
        return jsonArray;
    }


    private JSONArray transformGeometryCollection(GeoJsonGeometryCollection geoJsonGeometryCollection) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (GeoJsonGeometry geoJsonGeometry : geoJsonGeometryCollection.getGeometries()) {
            jsonArray.put(transformGeometry(geoJsonGeometry));
        }
        return jsonArray;
    }

    private JSONArray transformCoordinates(List<LatLng> coordinates) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (LatLng latLng : coordinates) {
            jsonArray.put(transformCoordinate(latLng));
        }
        return jsonArray;
    }


    private JSONArray transformCoordinate(LatLng latLng) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(latLng.longitude);
        jsonArray.put(latLng.latitude);
        return jsonArray;
    }
}
