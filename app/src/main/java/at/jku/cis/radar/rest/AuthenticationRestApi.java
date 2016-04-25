package at.jku.cis.radar.rest;

import at.jku.cis.radar.model.AuthenticationToken;
import at.jku.cis.radar.model.Credentials;
import retrofit.http.Body;
import retrofit.http.POST;

public interface AuthenticationRestApi {

    @POST("/authentication")
    public AuthenticationToken authenticate(@Body Credentials credentials);
}
