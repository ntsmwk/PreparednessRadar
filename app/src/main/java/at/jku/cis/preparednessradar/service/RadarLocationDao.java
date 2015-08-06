package at.jku.cis.preparednessradar.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.preparednessradar.model.RadarLocation;
import at.jku.cis.preparednessradar.database.SqlDatabaseHelper;

import static at.jku.cis.preparednessradar.model.RadarLocation.TABLE_NAME;
import static at.jku.cis.preparednessradar.model.RadarLocation.TITLE;
import static at.jku.cis.preparednessradar.model.RadarLocation._ID;

public class RadarLocationDao {

    private SqlDatabaseHelper sqlDatabaseHelper;

    public RadarLocationDao(Context context) {
        sqlDatabaseHelper = new SqlDatabaseHelper(context);
    }

    public void insert(RadarLocation radarLocation) {
        ContentValues values = new ContentValues();
        values.put(TITLE, radarLocation.getTitle());
        sqlDatabaseHelper.getReadableDatabase().insert(TABLE_NAME, null, values);
    }

    public List<RadarLocation> findAll() {
        SQLiteDatabase database = sqlDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, new String[]{_ID, TITLE}, null,
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
        radarLocation.setId(cursor.getInt(cursor.getColumnIndex(_ID)));
        radarLocation.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
        return radarLocation;
    }
}
