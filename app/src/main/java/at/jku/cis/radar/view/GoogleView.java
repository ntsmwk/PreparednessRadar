package at.jku.cis.radar.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometryCollection;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPoint;

import org.apache.commons.collections4.IteratorUtils;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import at.jku.cis.radar.command.AddFeatureCommand;
import at.jku.cis.radar.command.AddGeometryEditCommand;
import at.jku.cis.radar.command.RemoveFeatureCommand;
import at.jku.cis.radar.command.RemoveGeometryEditCommand;
import at.jku.cis.radar.model.ApplicationMode;
import at.jku.cis.radar.model.DrawType;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.model.MyGeoJsonFeature;
import at.jku.cis.radar.model.PenMode;
import at.jku.cis.radar.model.PenSetting;
import at.jku.cis.radar.rest.FeaturesRestApi;
import at.jku.cis.radar.service.GeoJsonFeatureBuilder;
import at.jku.cis.radar.service.GeoJsonGeometryBuilder;
import at.jku.cis.radar.service.GeoJsonIntersectionRemover;
import at.jku.cis.radar.service.GeoJsonIntersectionSearcher;
import at.jku.cis.radar.service.GeoJsonLayerBuilder;
import at.jku.cis.radar.task.GetFeaturesTask;
import at.jku.cis.radar.transformer.GeoJsonFeature2JsonObjectTransformer;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static at.jku.cis.radar.rest.RestServiceGenerator.createService;


public class GoogleView extends MapView implements OnMapReadyCallback, EventTreeFragment.EventClickListener {
    private final String TAG = "GoogleView";

    private GoogleMap googleMap;
    private boolean paintingEnabled = false;
    private PenSetting penSetting = new PenSetting();

    private ApplicationMode applicationMode = ApplicationMode.PAINT;

    private com.google.maps.android.geojson.GeoJsonFeature selectedFeature;

    private GeoJsonGeometryBuilder geoJsonGeometryBuilder;

    private Map<Event, GeoJsonLayer> event2GeoJsonLayer = new HashMap<>();

    public GoogleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    public ApplicationMode getApplicationMode() {
        return applicationMode;
    }

    public PenSetting getPenSetting() {
        return penSetting;
    }

    @Override
    public void handleEventVisibleChanged(Event event) {
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEvent(event);
        if (event.isVisible()) {
            geoJsonLayer.addLayerToMap();
        } else {
            geoJsonLayer.removeLayerFromMap();
        }
    }

    @Override
    public void handleEventSelectionChanged(Event event) {
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEvent(event);
        if (event.isSelected()) {
            geoJsonLayer.addLayerToMap();
        }
        paintingEnabled = event.isSelected();
        penSetting.setColor(event.getColor());
        penSetting.setEvent(event);
    }

    public void handleApplicationModeChanged(ApplicationMode applicationMode) {
        if (ApplicationMode.PAINT == applicationMode) {
            GeoJsonLayer geoJsonLayer = event2GeoJsonLayer.get(penSetting.getEvent());
            this.selectedFeature.setPolygonStyle(geoJsonLayer.getDefaultPolygonStyle());
            this.selectedFeature.setLineStringStyle(geoJsonLayer.getDefaultLineStringStyle());
            this.selectedFeature.setPointStyle(geoJsonLayer.getDefaultPointStyle());
            this.selectedFeature = null;
        }
        this.applicationMode = applicationMode;
    }

    private GeoJsonLayer findGeoJsonLayerByEvent(Event event) {
        GeoJsonLayer geoJsonLayer = event2GeoJsonLayer.get(event);
        if (geoJsonLayer == null) {
            geoJsonLayer = createGeoJsonLayer(event);
            event2GeoJsonLayer.put(event, geoJsonLayer);
        }
        return geoJsonLayer;
    }

    private GeoJsonLayer findCurrentGeoJsonLayer() {
        return findGeoJsonLayerByEvent(penSetting.getEvent());
    }

    private GeoJsonLayer createGeoJsonLayer(Event event) {
        GeoJsonLayerBuilder geoJsonLayerBuilder = new GeoJsonLayerBuilder();
        geoJsonLayerBuilder.setJsonObject(loadFeatures(event));
        geoJsonLayerBuilder.setColor(event.getColor());
        return geoJsonLayerBuilder.setGoogleMap(googleMap).build();
    }

    private JSONObject loadFeatures(Event event) {
        try {
            return new GetFeaturesTask().execute(event.getId()).get();
        } catch (InterruptedException | ExecutionException e) {
            return new JSONObject();
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent motionEvent) {
        if (googleMap == null) {
            return false;
        }
        if (paintingEnabled && motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            dispatchStylusTouchEvent(motionEvent);
        } else if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            super.dispatchTouchEvent(motionEvent);
        }
        return true;
    }

    private void dispatchStylusTouchEvent(MotionEvent motionEvent) {
        Point currentPosition = new Point((int) motionEvent.getX(), (int) motionEvent.getY());
        LatLng currentLatLng = googleMap.getProjection().fromScreenLocation(currentPosition);
        if (ApplicationMode.PAINT == applicationMode) {
            if (PenMode.ERASING == penSetting.getPenMode()) {
                doErasing(motionEvent, currentLatLng);
            } else {
                doPainting(motionEvent, currentLatLng);
            }
        } else if (ApplicationMode.EDIT == applicationMode || applicationMode.EVOLE == applicationMode) {
            if (selectedFeature != null) {
                if (PenMode.ERASING == penSetting.getPenMode()) {
                    doErasing(motionEvent, currentLatLng);
                } else {
                    doPainting(motionEvent, currentLatLng);
                }
            } else if (MotionEvent.ACTION_UP == motionEvent.getAction()) {
                doSelectFeature(motionEvent, currentLatLng);
            }
        }
    }

    private void doSelectFeature(MotionEvent motionEvent, LatLng currentLatLng) {
        if (motionEvent.getAction() != MotionEvent.ACTION_UP) {
            return;
        }
        GeoJsonLayer geoJsonLayer = findGeoJsonLayerByEvent(penSetting.getEvent());
        List<GeoJsonFeature> geoJsonFeatures = IteratorUtils.toList(geoJsonLayer.getFeatures().iterator());
        List<GeoJsonFeature> featureList = new GeoJsonIntersectionSearcher().searchForIntersection(geoJsonFeatures, new GeoJsonPoint(currentLatLng));
        if (featureList.size() == 1) {
            this.selectedFeature = featureList.get(0);
            this.selectedFeature.getPolygonStyle().setStrokeWidth(10);
            this.selectedFeature.getPolygonStyle().setStrokeColor(Color.WHITE);
            this.selectedFeature.getLineStringStyle().setWidth(10);
            this.selectedFeature.getLineStringStyle().setColor(Color.WHITE);
        } else {
            Toast.makeText(getContext(), "Can only edit one Event. Please click on the specific area without other overlapping events.", Toast.LENGTH_SHORT).show();
        }
    }

    private void doPainting(MotionEvent motionEvent, LatLng latLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(penSetting.getDrawType());
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonGeometryCollection geoJsonGeometry = geoJsonGeometryBuilder.build();


            switch (applicationMode) {
                case PAINT:
                    doNewFeaturePainting(geoJsonGeometry);
                    break;
                case EDIT:
                    doEditPainting(geoJsonGeometry);
                    break;
                case EVOLE:
                    doEvolvePainting(geoJsonGeometry);
            }
        }
    }

    private void doEvolvePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(selectedFeature);
        createService(FeaturesRestApi.class).saveFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                Toast.makeText(getContext(), "Feature is edited", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not save feature", error);
                Toast.makeText(getContext(), "Feature is not edited", Toast.LENGTH_LONG).show();
            }
        });
        new AddGeometryEditCommand(geoJsonGeometry, event2GeoJsonLayer.get(penSetting.getEvent()), selectedFeature).doCommand();
    }

    private void doEditPainting(GeoJsonGeometryCollection geoJsonGeometry) {
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(selectedFeature);
        createService(FeaturesRestApi.class).updateFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                Toast.makeText(getContext(), "Feature is edited", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not save feature", error);
                Toast.makeText(getContext(), "Feature is not edited", Toast.LENGTH_LONG).show();
            }
        });
        new AddGeometryEditCommand(geoJsonGeometry, event2GeoJsonLayer.get(penSetting.getEvent()), selectedFeature).doCommand();
    }

    private void doNewFeaturePainting(GeoJsonGeometryCollection geoJsonGeometry) {
        final MyGeoJsonFeature myGeoJsonFeature = new GeoJsonFeatureBuilder(geoJsonGeometry).setColor(penSetting.getColor()).build();
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(myGeoJsonFeature);
        createService(FeaturesRestApi.class).saveFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                myGeoJsonFeature.setId(o.toString());
                Toast.makeText(getContext(), "Feature is saved", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not save feature", error);
                Toast.makeText(getContext(), "Feature is not saved", Toast.LENGTH_LONG).show();
            }
        });
        new AddFeatureCommand(myGeoJsonFeature, event2GeoJsonLayer.get(penSetting.getEvent())).doCommand();
    }

    private void doErasing(MotionEvent motionEvent, LatLng latLng) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            geoJsonGeometryBuilder = new GeoJsonGeometryBuilder(DrawType.POLYGON);
        }
        geoJsonGeometryBuilder.addCoordinate(latLng);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            GeoJsonLayer geoJsonLayer = event2GeoJsonLayer.get(penSetting.getEvent());
            GeoJsonGeometryCollection geoJsonGeometry = geoJsonGeometryBuilder.build();
            if (ApplicationMode.PAINT == applicationMode) {
                doCreateModeErasing(geoJsonLayer, geoJsonGeometry);
            } else if (ApplicationMode.EDIT == applicationMode) {
                doEditModeErasing(geoJsonGeometry);
            } else if (ApplicationMode.EVOLE == applicationMode) {
                doEvolveModeErasing(geoJsonGeometry);
            }
        }
    }

    private void doEvolveModeErasing(GeoJsonGeometryCollection geoJsonGeometryCollection) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Collections.singletonList(selectedFeature), geoJsonGeometryCollection.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        GeoJsonFeature newFeature = new GeoJsonFeatureBuilder().setId(selectedFeature.getId()).build();
        if (!geoJsonIntersectionRemover.getAddList().isEmpty()) {
            newFeature = geoJsonIntersectionRemover.getAddList().get(0);
        }
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(selectedFeature);
        createService(FeaturesRestApi.class).saveFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                Toast.makeText(getContext(), "Feature is evolved", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not save feature", error);
                Toast.makeText(getContext(), "Feature is not evolved", Toast.LENGTH_LONG).show();
            }
        });
        new RemoveGeometryEditCommand(findCurrentGeoJsonLayer(), selectedFeature, newFeature).doCommand();
    }

    private void doEditModeErasing(GeoJsonGeometryCollection geoJsonGeometryCollection) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(Collections.singletonList(selectedFeature), geoJsonGeometryCollection.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        GeoJsonFeature newFeature = new GeoJsonFeatureBuilder().setId(selectedFeature.getId()).build();
        if (!geoJsonIntersectionRemover.getAddList().isEmpty()) {
            newFeature = geoJsonIntersectionRemover.getAddList().get(0);
        }
        JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(newFeature);
        createService(FeaturesRestApi.class).updateFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

            @Override
            public void success(Object o, Response response) {
                Toast.makeText(getContext(), "Feature is edited", Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Could not save feature", error);
                Toast.makeText(getContext(), "Feature is not edited", Toast.LENGTH_LONG).show();
            }
        });
        new RemoveGeometryEditCommand(findCurrentGeoJsonLayer(), selectedFeature, newFeature).doCommand();
    }

    private void doCreateModeErasing(GeoJsonLayer geoJsonLayer, GeoJsonGeometryCollection geoJsonGeometry) {
        GeoJsonIntersectionRemover geoJsonIntersectionRemover = new GeoJsonIntersectionRemover(geoJsonLayer.getFeatures(), geoJsonGeometry.getGeometries().get(0));
        geoJsonIntersectionRemover.intersectGeoJsonFeatures();
        FeaturesRestApi featuresRestApi = createService(FeaturesRestApi.class);
        for (GeoJsonFeature geoJsonFeature : geoJsonIntersectionRemover.getAddList()) {
            JSONObject jsonFeature = new GeoJsonFeature2JsonObjectTransformer().transform(geoJsonFeature);
            featuresRestApi.updateFeature(penSetting.getEvent().getId(), jsonFeature, new Callback() {

                @Override
                public void success(Object o, Response response) {
                    Toast.makeText(getContext(), "Feature is edited", Toast.LENGTH_LONG).show();
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, "Could not save feature", error);
                    Toast.makeText(getContext(), "Feature is not edited", Toast.LENGTH_LONG).show();
                }
            });
            new RemoveFeatureCommand(geoJsonLayer, geoJsonIntersectionRemover.getAddList(), geoJsonIntersectionRemover.getRemoveList()).doCommand();
        }
    }

    public void handleNewLocation(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }
}
