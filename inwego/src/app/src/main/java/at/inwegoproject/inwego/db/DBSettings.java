package at.inwegoproject.inwego.db;

public class DBSettings {
    // Database Version
    public static final int DATABASE_VERSION = 1;

    // Database Name
    public static final String DATABASE_NAME = "Inwego.db";

    //------------------------------------------------------------------
    //----------------------------- TABLES -----------------------------
    //------------------------------------------------------------------

    // |||||||||||||||||||||||||||| Route ||||||||||||||||||||||||||||||

    // Route table name
    public static final String TABLE_ROUTE = "route";

    // Route Table Columns names
    public static final String COLUMN_ROUTE_ID = "route_id";
    public static final String COLUMN_ROUTE_NAME = "route_name";
    public static final String COLUMN_ROUTE_DESTINATION_LATITUDE = "dest_latitude";
    public static final String COLUMN_ROUTE_DESTINATION_LONGITUDE = "dest_longitude";
    public static final String COLUMN_ROUTE_DESTINATION_ADDRESS = "dest_address";

    // |||||||||||||||||||||||||||| WAYPOINT ||||||||||||||||||||||||||||||

    // Route table name
    public static final String TABLE_WAYPOINT = "waypoint";

    // Route Table Columns names
    public static final String COLUMN_WAYPOINT_ID = "waypoint_id";
    public static final String COLUMN_WAYPOINT_FIRST_NAME = "waypoint_first_name";
    public static final String COLUMN_WAYPOINT_LAST_NAME = "waypoint_last_name";
    public static final String COLUMN_WAYPOINT_NUMBER = "waypoint_number";
    public static final String COLUMN_WAYPOINT_LATITUDE = "waypoint_latitude";
    public static final String COLUMN_WAYPOINT_LONGITUDE = "waypoint_longitude";
    public static final String COLUMN_WAYPOINT_ADDRESS = "waypoint_address";
    public static final String COLUMN_WAYPOINT_ROUTE_ID = "waypoint_route_id";



}
