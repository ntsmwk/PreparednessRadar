package at.jku.cis.radar.service;

import java.util.List;

import at.jku.cis.radar.model.Event;
import retrofit.http.GET;

public interface RestService {

    @GET("/events")
    List<Event> getEvents();
}