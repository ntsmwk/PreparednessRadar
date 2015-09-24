package at.jku.cis.radar.rest;


import org.json.JSONObject;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface LayerRestService {

    @GET("/layers/{eventId}")
    public JSONObject getLayer(@Path("eventId") long eventId);

    @POST("/layers/{eventId}")
    public void saveLayer(@Path("eventId") long eventId, @Body JSONObject jsonObject);
}
