package at.jku.cis.radar.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.GeoJsonContent;
import at.jku.cis.radar.rest.FeatureContentRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;

public class FeatureDetailActivity extends AppCompatActivity {


    private TextView titleTextView;
    private TextView descriptionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_detail);
        titleTextView = (TextView) findViewById(R.id.title);
        descriptionTextView = (TextView) findViewById(R.id.description);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feature_detail, menu);
        MenuItem.OnMenuItemClickListener menuItemClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.back:
                        finish();
                }
                return true;
            }
        };
        menu.findItem(R.id.back).setOnMenuItemClickListener(menuItemClickListener);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContent(getIntent().getExtras());
    }

    private void loadContent(Bundle bundle) {
        try {
            long featureId = Long.valueOf(bundle.getString("featureId"));
            FeatureContentTask featureContentTask = new FeatureContentTask(featureId);
            bindGeoJsonContent(featureContentTask.execute().get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindGeoJsonContent(GeoJsonContent geoJsonContent) {
        titleTextView.setText(geoJsonContent.getTitle());
        descriptionTextView.setText(geoJsonContent.getDescription());
    }

    private class FeatureContentTask extends AsyncTask<Void, Void, GeoJsonContent> {
        private long featureId;

        FeatureContentTask(long featureId) {
            this.featureId = featureId;
        }

        @Override
        protected GeoJsonContent doInBackground(Void... params) {
            return createFeatureContentService().getFeatureContent(featureId);
        }
    }

    private FeatureContentRestApi createFeatureContentService() {
        return RestServiceGenerator.createService(FeatureContentRestApi.class, getIntent().getStringExtra("token"));
    }
}
