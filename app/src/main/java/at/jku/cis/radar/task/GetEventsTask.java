package at.jku.cis.radar.task;


import android.os.AsyncTask;

import java.util.List;

import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.rest.EventsRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;


public class GetEventsTask extends AsyncTask<Void, Void, List<Event>> {
    @Override
    protected List<Event> doInBackground(Void... params) {
        return RestServiceGenerator.createService(EventsRestApi.class).getEvents();
    }
}

