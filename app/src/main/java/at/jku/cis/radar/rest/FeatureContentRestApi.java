package at.jku.cis.radar.rest;

import at.jku.cis.radar.model.GeoJsonContent;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface FeatureContentRestApi {

    @GET("/featureContent/{featureId}")
    GeoJsonContent getFeatureContent(@Path("featureId") long featureId);

    @POST("/featureContent")
    void updateFeatureContent(@Body GeoJsonContent geoJsonContent, Callback<GeoJsonContent> callback);
}
