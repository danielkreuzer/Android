package at.inwegoproject.inwego.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import at.inwegoproject.inwego.domain.Route;
import at.inwegoproject.inwego.domain.Waypoint;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private String CREATE_ROUTE_TABLE = "CREATE TABLE IF NOT EXISTS " + DBSettings.TABLE_ROUTE + "("
            + DBSettings.COLUMN_ROUTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + DBSettings.COLUMN_ROUTE_NAME + " TEXT,"
            + DBSettings.COLUMN_ROUTE_DESTINATION_LATITUDE + " REAL,"
            + DBSettings.COLUMN_ROUTE_DESTINATION_LONGITUDE + " REAL,"
            + DBSettings.COLUMN_ROUTE_DESTINATION_ADDRESS + " TEXT)";

    private String CREATE_WAYPOINT_TABLE = "CREATE TABLE IF NOT EXISTS " + DBSettings.TABLE_WAYPOINT + "("
            + DBSettings.COLUMN_WAYPOINT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + DBSettings.COLUMN_WAYPOINT_FIRST_NAME + " TEXT,"
            + DBSettings.COLUMN_WAYPOINT_LAST_NAME + " TEXT,"
            + DBSettings.COLUMN_WAYPOINT_NUMBER + " TEXT,"
            + DBSettings.COLUMN_WAYPOINT_LATITUDE + " REAL,"
            + DBSettings.COLUMN_WAYPOINT_LONGITUDE + " REAL,"
            + DBSettings.COLUMN_WAYPOINT_ADDRESS + " TEXT,"
            + DBSettings.COLUMN_WAYPOINT_ROUTE_ID + " INTEGER,"
            + "FOREIGN KEY(" + DBSettings.COLUMN_WAYPOINT_ROUTE_ID
            +") REFERENCES " + DBSettings.TABLE_ROUTE + "(" + DBSettings.COLUMN_ROUTE_ID + ")"
            + ")";

    private String DROP_ROUTE_TABLE = "DROP TABLE IF EXISTS " + DBSettings.TABLE_ROUTE;
    private String DROP_WAYPOINT_TABLE = "DROP TABLE IF EXISTS " + DBSettings.TABLE_WAYPOINT;


    /**
     * Constructor
     */
    public DBHelper(Context context) {
        super(context, DBSettings.DATABASE_NAME, null, DBSettings.DATABASE_VERSION);
    }

    /**
     * Creates Database
     * @param db actual Database
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ROUTE_TABLE);
        db.execSQL(CREATE_WAYPOINT_TABLE);
    }

    /**
     * Handles update of database
     * @param db actual Database
     * @param oldVersion version number of old database
     * @param newVersion version number of new database
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //Drop User Table if exist
        db.execSQL(DROP_ROUTE_TABLE);
        db.execSQL(DROP_WAYPOINT_TABLE);

        // Create tables again
        onCreate(db);
    }

    /**
     * This method is to create route record
     *
     * @param route Route to add
     */
    public long addRoute(Route route) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBSettings.COLUMN_ROUTE_NAME, route.getName());
        values.put(DBSettings.COLUMN_ROUTE_DESTINATION_LATITUDE, route.getLatitude());
        values.put(DBSettings.COLUMN_ROUTE_DESTINATION_LONGITUDE, route.getLongitude());
        values.put(DBSettings.COLUMN_ROUTE_DESTINATION_ADDRESS, route.getAddress());

        // Inserting Row
        long insertedId = db.insert(DBSettings.TABLE_ROUTE, null, values);
        db.close();
        return insertedId;
    }

    /**
     * This method is to create route record
     *
     * @param waypoint Waypoint to add
     */
    public long addWaypoint(Waypoint waypoint) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBSettings.COLUMN_WAYPOINT_FIRST_NAME, waypoint.getFirstName());
        values.put(DBSettings.COLUMN_WAYPOINT_LAST_NAME, waypoint.getLastName());
        values.put(DBSettings.COLUMN_WAYPOINT_NUMBER, waypoint.getNumber());
        values.put(DBSettings.COLUMN_WAYPOINT_LATITUDE, waypoint.getLatitude());
        values.put(DBSettings.COLUMN_WAYPOINT_LONGITUDE, waypoint.getLongitude());
        values.put(DBSettings.COLUMN_WAYPOINT_ADDRESS, waypoint.getAddress());
        values.put(DBSettings.COLUMN_WAYPOINT_ROUTE_ID, waypoint.getRouteId());

        // Inserting Row
        long insertedId = db.insert(DBSettings.TABLE_WAYPOINT, null, values);
        db.close();
        return insertedId;
    }

    /**
     * Get all Routes
     * @return List of Routes
     */
    public List<Route> getAllRoutes() {
        // array of columns to fetch
        String[] columns = {
                DBSettings.COLUMN_ROUTE_ID,
                DBSettings.COLUMN_ROUTE_NAME,
                DBSettings.COLUMN_ROUTE_DESTINATION_LATITUDE,
                DBSettings.COLUMN_ROUTE_DESTINATION_LONGITUDE,
                DBSettings.COLUMN_ROUTE_DESTINATION_ADDRESS
        };
        // sorting orders
        String sortOrder =
                DBSettings.COLUMN_ROUTE_NAME + " ASC";
        List<Route> userList = new ArrayList<Route>();

        SQLiteDatabase db = this.getReadableDatabase();

        // query the user table
        /*
         * Here query function is used to fetch records from user table this function works like we use sql query.
         * SQL query equivalent to this query function is
         * SELECT user_id,user_name,user_email,user_password FROM user ORDER BY user_name;
         */
        Cursor cursor = db.query(DBSettings.TABLE_ROUTE, //Table to query
                columns,    //columns to return
                null,        //columns for the WHERE clause
                null,        //The values for the WHERE clause
                null,       //group the rows
                null,       //filter by row groups
                sortOrder); //The sort order


        // Traversing through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Route route = new Route();
                route.setId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_ROUTE_ID))));
                route.setName(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_ROUTE_NAME)));
                route.setLatitude(Double.parseDouble(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_ROUTE_DESTINATION_LATITUDE))));
                route.setLongitude(Double.parseDouble(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_ROUTE_DESTINATION_LONGITUDE))));
                route.setAddress(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_ROUTE_DESTINATION_ADDRESS)));
                // Adding user record to list
                userList.add(route);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // return user list
        return userList;
    }

    /**
     * Get Waypoints for a route by its id
     * @param routeId Id of the route
     * @return List of waypoints
     */
    public List<Waypoint> getWaypointsForRoute(int routeId) {
        // array of columns to fetch
        String[] columns = {
                DBSettings.COLUMN_WAYPOINT_ID,
                DBSettings.COLUMN_WAYPOINT_FIRST_NAME,
                DBSettings.COLUMN_WAYPOINT_LAST_NAME,
                DBSettings.COLUMN_WAYPOINT_NUMBER,
                DBSettings.COLUMN_WAYPOINT_LATITUDE,
                DBSettings.COLUMN_WAYPOINT_LONGITUDE,
                DBSettings.COLUMN_WAYPOINT_ADDRESS,
                DBSettings.COLUMN_WAYPOINT_ROUTE_ID,
        };
        String whereClause = DBSettings.COLUMN_WAYPOINT_ROUTE_ID + " = ?";
        String[] whereArg = new String[]{String.valueOf(routeId)};

        List<Waypoint> userList = new ArrayList<Waypoint>();

        SQLiteDatabase db = this.getReadableDatabase();

        // query the user table
        /**
         * Here query function is used to fetch records from user table this function works like we use sql query.
         * SQL query equivalent to this query function is
         * SELECT user_id,user_name,user_email,user_password FROM user ORDER BY user_name;
         */
        Cursor cursor = db.query(DBSettings.TABLE_WAYPOINT, //Table to query
                columns,    //columns to return
                whereClause,        //columns for the WHERE clause
                whereArg,        //The values for the WHERE clause
                null,       //group the rows
                null,       //filter by row groups
                null); //The sort order


        // Traversing through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Waypoint waypoint = new Waypoint();
                waypoint.setId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_ID))));
                waypoint.setFirstName(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_FIRST_NAME)));
                waypoint.setLastName(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_LAST_NAME)));
                waypoint.setNumber(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_NUMBER)));
                waypoint.setLatitude(Double.parseDouble(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_LATITUDE))));
                waypoint.setLongitude(Double.parseDouble(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_LONGITUDE))));
                waypoint.setAddress(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_ADDRESS)));
                waypoint.setRouteId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(DBSettings.COLUMN_WAYPOINT_ROUTE_ID))));
                // Adding user record to list
                userList.add(waypoint);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // return user list
        return userList;
    }

    /**
     * Deletes the route and waypoints what are combined with this route
     * @param routeId Id of the route what should be deleted
     */
    public void deleteRouteWithWaypoints(int routeId) {
        List<Waypoint> waypoints;
        waypoints = getWaypointsForRoute(routeId);
        for (Waypoint wp: waypoints) {
            deleteWaypoint(wp.getId());
        }
        deleteRoute(routeId);
    }

    /**
     * Deletes a waypoint
     * @param waypointId Id of the waypoint what should be deleted
     */
    private void deleteWaypoint(int waypointId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // delete user record by id
        db.delete(DBSettings.TABLE_WAYPOINT, DBSettings.COLUMN_WAYPOINT_ID + " = ?",
                new String[]{String.valueOf(waypointId)});
        db.close();
    }

    /**
     * Deletes only the route without the waypoints
     * @param routeId Id of the route what should be deleted
     */
    private void deleteRoute(int routeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // delete user record by id
        db.delete(DBSettings.TABLE_ROUTE, DBSettings.COLUMN_ROUTE_ID + " = ?",
                new String[]{String.valueOf(routeId)});
        db.close();
    }

}
