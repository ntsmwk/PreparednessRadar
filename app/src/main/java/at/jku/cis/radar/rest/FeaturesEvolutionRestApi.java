package at.jku.cis.radar.rest;

import org.json.JSONObject;

import retrofit.http.GET;
import retrofit.http.Path;

public interface FeaturesEvolutionRestApi {

    @GET("/featureEvolution/{eventId}/{featureId}")
    JSONObject getFeaturesEvolution(@Path("eventId") long eventId, @Path("featureId") long featureId);
}
