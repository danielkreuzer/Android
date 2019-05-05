package at.inwegoproject.inwego.helper;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

/**
 * Class which aggregates location functionality
 */
public class LocationHelper {
    /**
     * Returns the current state of the location services
     * @param context The context for retrieving the location service
     * @return true, if location services are enabled, otherwise false
     */
    public static boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            // This is Deprecated in API 28
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (mode != Settings.Secure.LOCATION_MODE_OFF);

        }
    }
}
