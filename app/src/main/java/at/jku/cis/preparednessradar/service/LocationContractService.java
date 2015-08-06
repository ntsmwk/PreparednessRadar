package at.jku.cis.preparednessradar.service;

import static at.jku.cis.preparednessradar.contract.LocationContract.LocationEntry.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.preparednessradar.contract.LocationContract;
import at.jku.cis.preparednessradar.database.LocationContractDbHelper;

public class LocationContractService {

    private LocationContractDbHelper locationContractDbHelper;

    public LocationContractService(Context context) {
        locationContractDbHelper = new LocationContractDbHelper(context);
    }

    public void insert(LocationContract locationContract) {
        ContentValues values = new ContentValues();
        values.put(TITLE, locationContract.getTitle());
        locationContractDbHelper.getReadableDatabase().insert(TABLE_NAME, null, values);
    }

    public List<LocationContract> findAll() {
        SQLiteDatabase database = locationContractDbHelper.getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, new String[]{_ID, TITLE}, null,
                null, null, null, null);
        return buildLocationContracts(cursor);
    }

    private List<LocationContract> buildLocationContracts(Cursor cursor) {
        if (cursor.moveToFirst()) {
            return loopLocationContracts(cursor);
        }
        return new ArrayList<>();
    }

    private List<LocationContract> loopLocationContracts(Cursor cursor) {
        List<LocationContract> locationContracts = new ArrayList<>();

        do {
            LocationContract locationContract = new LocationContract();
            locationContract.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            locationContracts.add(locationContract);
        } while (cursor.moveToNext());

        return locationContracts;
    }
}
