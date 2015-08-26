package at.jku.cis.radar.entry;

import android.provider.BaseColumns;

import java.text.MessageFormat;

public interface RadarLocationEntry extends BaseColumns {
    public static final String TABLE_NAME = "RadarLocation";
    public static final String TITLE = "title";

    public static final String SQL_CREATE_ENTRY = MessageFormat.format("CREATE TABLE {0} ( {1} INTEGER PRIMARY KEY, {2} TEXT )", TABLE_NAME, _ID, TITLE);
    public static final String SQL_DELETE_ENTRY = "DROP TABLE IF EXISTS " + TABLE_NAME;
}
