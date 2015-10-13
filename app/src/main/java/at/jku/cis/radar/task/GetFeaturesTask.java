package at.jku.cis.radar.task;

import android.os.AsyncTask;

import org.json.JSONObject;

import at.jku.cis.radar.rest.FeaturesRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;

public class GetFeaturesTask extends AsyncTask<Long, Void, JSONObject> {

    @Override
    protected JSONObject doInBackground(Long... params) {
        return RestServiceGenerator.createService(FeaturesRestApi.class).getFeatures(params[0]);
    }
}