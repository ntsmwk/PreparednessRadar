package at.jku.cis.radar.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.database.SqlDatabaseHelper;
import at.jku.cis.radar.entry.RadarLocationEntry;
import at.jku.cis.radar.model.RadarLocation;

public class RadarLocationDao {

    private SqlDatabaseHelper sqlDatabaseHelper;

    public RadarLocationDao(Context context) {
        sqlDatabaseHelper = new SqlDatabaseHelper(context);
    }

    public void insert(RadarLocation radarLocation) {
        ContentValues values = new ContentValues();
        values.put(RadarLocationEntry.TITLE, radarLocation.getTitle());
        sqlDatabaseHelper.getReadableDatabase().insert(RadarLocationEntry.TABLE_NAME, null, values);
    }

    public List<RadarLocation> findAll() {
        SQLiteDatabase database = sqlDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.query(RadarLocationEntry.TABLE_NAME, new String[]{BaseColumns._ID, RadarLocationEntry.TITLE}, null,
                null, null, null, null);
        return buildRadarLocations(cursor);
    }

    private List<RadarLocation> buildRadarLocations(Cursor cursor) {
        List<RadarLocation> radarLocations = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                radarLocations.add(buildRadarLocation(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return radarLocations;
    }

    private RadarLocation buildRadarLocation(Cursor cursor) {
        RadarLocation radarLocation = new RadarLocation();
        radarLocation.setId(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)));
        radarLocation.setTitle(cursor.getString(cursor.getColumnIndex(RadarLocationEntry.TITLE)));
        return radarLocation;
    }
}
