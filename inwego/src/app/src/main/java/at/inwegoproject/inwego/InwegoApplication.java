package at.inwegoproject.inwego;

import android.app.Application;

import at.inwegoproject.inwego.db.DBHelper;

/**
 * Custom instance of application class, which holds Singleton services (e.g. the database manager)
 */
public class InwegoApplication extends Application {
    private static InwegoApplication mInstance;
    private DBHelper mDatabaseManager;

    /**
     * Part of the lifecycle of an Application class, gets called implicitly.
     * Initializes the service instances.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mDatabaseManager = new DBHelper(mInstance);
    }

    /**
     *  Returns the singleton instance of the {@link DBHelper} class
     */
    public DBHelper getDatabaseManager() {
        return mDatabaseManager;
    }

    /**
     * Returns the singleton instance of the Application instance
     */
    public static synchronized InwegoApplication getInstance() {
        return mInstance;
    }
}