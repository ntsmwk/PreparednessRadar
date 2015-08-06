package at.jku.cis.preparednessradar.contract;

import android.provider.BaseColumns;

public class LocationContract {
    // To prevent someone from accidentally instantiating the contract class,
    public static final String SQL_CREATE_ENTRY =
            "CREATE TABLE " + LocationEntry.TABLE_NAME + " (" +
                    LocationEntry._ID + " INTEGER PRIMARY KEY," +
                    LocationEntry.COLUMN_NAME_TITLE + " TEXT )";

    public static final String SQL_DELETE_ENTRY =
            "DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME;

    public LocationContract() {
    }

    /* Inner class that defines the table contents */
    public static abstract class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "LocationContract";

        public static final String COLUMN_NAME_TITLE = "title";
    }
}
