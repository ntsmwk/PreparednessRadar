package at.jku.cis.radar.rest;

import java.util.List;

import at.jku.cis.radar.model.Event;
import retrofit.http.GET;

public interface EventsRestApi {

    @GET("/events")
    public List<Event> getEvents();
}
