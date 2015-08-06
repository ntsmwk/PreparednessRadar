package at.jku.cis.preparednessradar.contract;

import android.provider.BaseColumns;

import java.text.MessageFormat;

public class LocationContract {
    // To prevent someone from accidentally instantiating the contract class,
    public static final String SQL_CREATE_ENTRY = MessageFormat.format("CREATE TABLE {0} ( {1} INTEGER PRIMARY KEY, {2} TEXT )", LocationEntry.TABLE_NAME, LocationEntry._ID, LocationEntry.TITLE);
    public static final String SQL_DELETE_ENTRY = "DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME;

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /* Inner class that defines the table contents */
    public static abstract class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "LocationContract";

        public static final String TITLE = "title";
    }
}
