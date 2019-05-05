package at.inwegoproject.inwego.helper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Class which aggregates SMS functionality
 */
public class SmsHelper {
    private static final String TAG = SmsHelper.class.getSimpleName();

    /**
     *  Sends a given message to a given phone number.
     * @param activity Activity, which requests the permission for sending
     * @param permissionCode Code to be returned to the activity
     * @param phoneNumber Phone number with area code to be sent to
     * @param message Message which should be sent
     */
    public static void sendSMStoPhoneNumber(Activity activity, int permissionCode, String phoneNumber, String message) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsMgrVar = SmsManager.getDefault();
                smsMgrVar.sendTextMessage(phoneNumber, null, message, null, null);
            } catch (Exception e) {
                // sending SMS failed
                Log.e(TAG, e.getMessage());
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.SEND_SMS},
                        permissionCode);
            }
        }
    }
}
