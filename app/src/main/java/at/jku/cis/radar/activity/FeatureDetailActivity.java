package at.jku.cis.radar.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.GeoJsonContent;
import at.jku.cis.radar.rest.FeatureContentRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class FeatureDetailActivity extends AppCompatActivity {
    private TextView titleView;
    private TextView eventView;
    private TextView creatorView;
    private TextView modifierView;
    private TextView descriptionView;

    private boolean enabledEditing = false;

    private GeoJsonContent geoJsonContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_detail);
        titleView = (TextView) findViewById(R.id.title);
        eventView = (TextView) findViewById(R.id.event);
        creatorView = (TextView) findViewById(R.id.creator);
        modifierView = (TextView) findViewById(R.id.modifier);
        descriptionView = (TextView) findViewById(R.id.description);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feature_detail, menu);
        MenuItemClickListener menuItemClickListener = new MenuItemClickListener();
        menu.findItem(R.id.back).setOnMenuItemClickListener(menuItemClickListener);
        menu.findItem(R.id.edit).setOnMenuItemClickListener(menuItemClickListener);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContent();
    }

    private void loadContent() {
        try {
            long featureId = determineFeatureId();
            bindGeoJsonContent(new FeatureContentTask(featureId).execute().get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Long determineFeatureId() {
        return Long.valueOf(getIntent().getExtras().getString("featureId"));
    }

    private void bindGeoJsonContent(GeoJsonContent geoJsonContent) {
        titleView.setText(geoJsonContent.getTitle());
        eventView.setText(geoJsonContent.getEvent().getName());
        creatorView.setText(geoJsonContent.getCreator());
        modifierView.setText(geoJsonContent.getModifier());
        descriptionView.setText(geoJsonContent.getDescription());
    }

    private void saveFeatureContent() {
        GeoJsonContent geoJsonContent = unbindGeoJsonContent();
        createFeatureContentService().updateFeatureContent(geoJsonContent, new Callback<GeoJsonContent>() {
            @Override
            public void success(GeoJsonContent geoJsonContent, Response response) {
                Toast.makeText(getApplicationContext(), "Content is saved", Toast.LENGTH_LONG);
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getApplicationContext(), "Content is not saved", Toast.LENGTH_LONG);
            }
        });
    }

    private GeoJsonContent unbindGeoJsonContent() {
        GeoJsonContent geoJsonContent = new GeoJsonContent();
        geoJsonContent.setFeatureGroup(determineFeatureId());
        geoJsonContent.setTitle(titleView.getText().toString());
        geoJsonContent.setDescription(descriptionView.getText().toString());
        return geoJsonContent;
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

    private class MenuItemClickListener implements MenuItem.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.edit:
                    enabledEditing = !enabledEditing;
                    item.setTitle(!enabledEditing ? R.string.edit : R.string.save);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!enabledEditing) {
                                saveFeatureContent();
                            }
                            titleView.setEnabled(enabledEditing);
                            descriptionView.setEnabled(enabledEditing);
                        }
                    });
                    break;
            }
            return true;
        }
    }
}

