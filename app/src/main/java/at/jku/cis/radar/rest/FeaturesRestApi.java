package at.jku.cis.radar.rest;


import org.json.JSONObject;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

public interface FeaturesRestApi {

    @GET("/features/{eventId}")
    JSONObject getFeatures(@Path("eventId") long eventId);

    @POST("/features/{eventId}")
    void saveFeature(@Path("eventId") long eventId, @Body JSONObject geoJsonFeature, Callback<String> callback);

    @PUT("/features/{eventId}")
    void updateFeature(@Path("eventId") long eventId, @Body JSONObject geoJsonFeature, Callback<String> callback);
}