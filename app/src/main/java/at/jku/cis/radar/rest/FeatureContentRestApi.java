package at.jku.cis.radar.rest;

import at.jku.cis.radar.model.GeoJsonContent;
import retrofit.http.GET;
import retrofit.http.Path;

public interface FeatureContentRestApi {

    @GET("/featureContent/{featureId}")
    GeoJsonContent getFeatureContent(@Path("featureId") long featureId);
}
