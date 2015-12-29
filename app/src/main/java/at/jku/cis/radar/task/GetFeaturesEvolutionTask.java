package at.jku.cis.radar.task;

import android.os.AsyncTask;

import com.google.maps.android.geojson.GeoJsonFeature;

import org.json.JSONObject;

import java.util.List;

import at.jku.cis.radar.rest.FeaturesEvolutionRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;
import at.jku.cis.radar.transformer.JsonObject2GeoJsonFeatureTransformer;

public class GetFeaturesEvolutionTask extends AsyncTask<Long, Void, List<GeoJsonFeature>> {
    @Override
    protected List<GeoJsonFeature> doInBackground(Long... params) {
        FeaturesEvolutionRestApi featuresEvolutionRest = RestServiceGenerator.createService(FeaturesEvolutionRestApi.class);
        JSONObject jsonObject = featuresEvolutionRest.getFeaturesEvolution(params[0], params[1]);
        return new JsonObject2GeoJsonFeatureTransformer().transform(jsonObject);
    }
}
