package at.jku.cis.radar.timer;

import android.app.Activity;

import com.google.android.gms.maps.MapView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import at.jku.cis.radar.view.GoogleView;


public class CountDownRunner implements Runnable {

    private MapView mapView;
    private Activity activity;


    public CountDownRunner(Activity activity, MapView mapView) {
        this.mapView = mapView;
        this.activity = activity;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                doWork(mapView);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
            }
        }
    }

    public void doWork(final MapView mapView) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    Date date = new Date();
                    String mode = null;
                    if (mapView instanceof GoogleView) {
                        mode = ((GoogleView) mapView).getApplicationMode().getName();
                    } else {
                        mode = "Evolution Mode";
                    }
                    activity.setTitle(mode + "\t | \t" + dateFormat.format(date));
                } catch (Exception e) {
                }
            }
        });
    }

}
