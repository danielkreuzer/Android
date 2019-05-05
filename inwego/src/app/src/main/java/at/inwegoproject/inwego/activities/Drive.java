package at.inwegoproject.inwego.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import at.inwegoproject.inwego.R;
import at.inwegoproject.inwego.domain.Route;
import at.inwegoproject.inwego.domain.RouteDetails;
import at.inwegoproject.inwego.domain.Waypoint;
import at.inwegoproject.inwego.fragments.MapFragment;
import at.inwegoproject.inwego.helper.LocationHelper;
import at.inwegoproject.inwego.helper.RouteHelper;
import at.inwegoproject.inwego.helper.SmsHelper;

public class Drive extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 33;
    private static final int PERMISSIONS_REQUEST_SEND_SMS = 77;
    private static final String TAG = MapFragment.class.getSimpleName();
    private static final int DEFAULT_ZOOM = 13;
    private static final int INITIAL_STROKE_WIDTH_PX = 5;

    private FloatingActionButton mFabCancelRoute;
    private TextView mTxtNextWaypointMinutes;
    private TextView mTxtNextWaypointName;
    private TextView mTxtDestinationMinutes;

    private boolean mAllowReturn;

    private Route mActualRoute;
    private ArrayList<Waypoint> mActualWaypoints;

    private LinearLayout mLinearLayout;

    // Drive Control

    private RouteHelper mRouteHelper;

    private List<RouteDetails> mRouteDetails;

    private boolean mSendStartSms;
    private int mMinSendSms;

    // Members GMaps
    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private AlertDialog mGPSDialog;

    private List<Marker> mMarkers = new ArrayList<>();

    private boolean mLocationPermissionGranted;

    /**
     * BroadcastReceiver for receiving changes of the GPS state
     */
    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
                if(!LocationHelper.isLocationEnabled(Drive.this)) {
                    showGPSDisabledDialog();
                }
            }
        }
    };

    // save last SMS infos for sending it after permission request
    private String lastSMSMessage;
    private String lastSMSReceiver;

    //region Lifecycle

    /**
     * On Create for the drive mode activity
     * Initialises member variables and reads the pushed information from the activity before from the
     * intent extra.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (mMapFragment == null) {
            mMapFragment = SupportMapFragment.newInstance();
            mMapFragment.getMapAsync(this);
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.drive_map, mMapFragment).commit();

        mFabCancelRoute = findViewById(R.id.fab_cancel_drive);
        mFabCancelRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDrive();
            }
        });
        mTxtNextWaypointMinutes = findViewById(R.id.txt_next_waypoint_minutes);
        mTxtNextWaypointName = findViewById(R.id.txt_next_waypoint_name);
        mTxtDestinationMinutes = findViewById(R.id.txt_destination_minutes);
        mAllowReturn = false;
        mLinearLayout = findViewById(R.id.drive_linear_layout);

        mRouteHelper = new RouteHelper(this);

        Intent intent = getIntent();
        mActualRoute = intent.getParcelableExtra(MapFragment.INTENT_KEY_ROUTE);
        mActualWaypoints = intent.getParcelableArrayListExtra(MapFragment.INTENT_KEY_WAYPOINTS);
        getSettings();
    }

    /**
     * Restarts GPS-Receiver
     */
    @Override
    protected void onResume() {
        mAllowReturn = false;
        super.onResume();
        this.registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        if(!LocationHelper.isLocationEnabled(Drive.this)) {
            showGPSDisabledDialog();
        }
    }

    /**
     * Pauses GPS-Receiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mGpsSwitchStateReceiver);
    }

    /**
     * Block back button
     * Back only allowed by pressing stop Drive-Mode
     */
    @Override
    public void onBackPressed() {
        if (mAllowReturn) {
            super.onBackPressed();
        } else {
            cancelDrive();
        }
        mAllowReturn = false;
    }

    /**
     * Get permission for SMS sending
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // send last sms after permission request was successful
                    SmsHelper.sendSMStoPhoneNumber(this, PERMISSIONS_REQUEST_SEND_SMS, lastSMSReceiver, lastSMSMessage);
                }
            }
        }
    }

    //endregion

    //region DriveControl

    /**
     * Starts drive control. Gets route information from the google direction API and
     * initialises screen. After initialising optional "on my way" sms is send and
     * the further drive control is started.
     */
    private void startDriveControl() {
        mRouteDetails = mRouteHelper.getRouteDetails(mActualWaypoints,
                new com.google.maps.model.LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()),
                new com.google.maps.model.LatLng(mActualRoute.getLatitude(), mActualRoute.getLongitude()));


        mTxtNextWaypointMinutes.setText(mRouteDetails.get(0).readableDuration);
        mTxtNextWaypointName.setText(mActualWaypoints.get(0).getFirstName()
                + " " + mActualWaypoints.get(0).getLastName());
        mTxtDestinationMinutes.setText(DestinationTime());

        if (mSendStartSms) {
            SendStartSms();
            mSendStartSms = false;
        }

        if((mRouteDetails.get(0).durationSecounds / 60) <= mMinSendSms) {
            UpdateDrive();
        }

        DriveControl();
    }

    /**
     * Sends stat SMS "I'm on my way"
     */
    private void SendStartSms() {
        int x = 0;
        for (Waypoint w :
                mActualWaypoints) {
            lastSMSMessage = getString(R.string.DriveStartedStart) + " " + mRouteDetails.get(x).readableDuration +
            " " + getString(R.string.DriveStartedEnd) + " " + DestinationTime();
            lastSMSReceiver = w.getNumber();
            SmsHelper.sendSMStoPhoneNumber(this, PERMISSIONS_REQUEST_SEND_SMS, lastSMSReceiver, lastSMSMessage);
        }
        ShowSmsSent(getString(R.string.StartSmsSent));
    }

    /**
     * Starts a runnable that updates device location every 30 seconds and checks
     * inwego drive logic for reminding the next person on the next waypoint.
     */
    private void DriveControl() {
        Runnable driveRunnable = new Runnable() {
            @Override
            public void run() {
                getDeviceLocation(true);
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(driveRunnable, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Updates drive logic for reminding the next person on the next saved waypoint.
     * Also updates estimated time to the next waypoint and destination.
     */
    private void UpdateDrive() {
        mRouteDetails = mRouteHelper.getRouteDetails(mActualWaypoints,
                new com.google.maps.model.LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()),
                new com.google.maps.model.LatLng(mActualRoute.getLatitude(), mActualRoute.getLongitude()));

            if (mActualWaypoints.isEmpty()) {
                TriggerNoWaypointsLeft();
            } else {
                if ((mRouteDetails.get(0).durationSecounds / 60) <= mMinSendSms) {
                    lastSMSReceiver = mActualWaypoints.get(0).getNumber();
                    lastSMSMessage = getString(R.string.InwegoReminder)
                            + " " + mRouteDetails.get(0).getReadableDuration();
                    sendSms(mActualWaypoints.get(0).getFirstName());
                    mActualWaypoints.remove(0);
                    if (!mActualWaypoints.isEmpty()) {
                        UpdateText();
                    }
                    // if next waypoint is very near, send sms
                    if (!mActualWaypoints.isEmpty() && (mRouteDetails.get(0).durationSecounds / 60) <= mMinSendSms) {
                        UpdateDrive();
                    } else if (mActualWaypoints.isEmpty()) {
                        TriggerNoWaypointsLeft();
                    }
                } else {
                    UpdateText();
                }
            }
    }

    /**
     * Updates estimated time to next waypoint and estimated time to destination.
     */
    private void UpdateText() {
        mTxtNextWaypointMinutes.setText(mRouteDetails.get(0).readableDuration);
        mTxtNextWaypointName.setText(mActualWaypoints.get(0).getFirstName()
                + " " + mActualWaypoints.get(0).getLastName());
        mTxtDestinationMinutes.setText(DestinationTime());
    }

    /**
     * When no waypoint is left, only the estimated time to destination is updated.
     */
    private void TriggerNoWaypointsLeft() {
        mRouteDetails = mRouteHelper.getRouteDetails(mActualWaypoints,
                new com.google.maps.model.LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()),
                new com.google.maps.model.LatLng(mActualRoute.getLatitude(), mActualRoute.getLongitude()));
        mTxtNextWaypointMinutes.setText(R.string.NoDestinationLeft);
        mTxtNextWaypointName.setText(R.string.destination);
        mTxtDestinationMinutes.setText(DestinationTime());
    }

    /**
     * Sums up the estimated time from every waypoint.
     * @return string with the estimated time to the destination
     */
    private String DestinationTime() {
        long sum = 0;
        for (int x = 0; x <= mRouteDetails.size()-1; x++) {
            sum += mRouteDetails.get(x).durationSecounds;
        }

        Long sumMinutes = sum / 60;
        return sumMinutes.intValue() + " " + getString(R.string.minutesLabel);
    }

    /**
     * Handles send SMS to spezific contact. lastSMSReceiver and lastSMSMessage must be set before
     * @param name Name of the person which gets the SMS
     */
    private void sendSms(String name) {
        SmsHelper.sendSMStoPhoneNumber(this, PERMISSIONS_REQUEST_SEND_SMS, lastSMSReceiver, lastSMSMessage);
        ShowSmsSent(getString(R.string.SmsSent) + " " + name);
    }

    /**
     * Notification for the user in form of a snackbar
     * @param message Message what should be displayed on the snackbar
     */
    private void ShowSmsSent(String message) {
        Snackbar snackbar = Snackbar.make(mLinearLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    /**
     * Get choosen settings from shared preferences
     */
    private void getSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSendStartSms = sharedPreferences.getBoolean(getString(R.string.prefs_key_send_drive_off_sms), false);
        mMinSendSms = Integer.valueOf(sharedPreferences.getString(getString(R.string.prefs_key_min_before_sms), "2"));
        Log.e(TAG, "getSettings: " + mSendStartSms );
        Log.e(TAG, "getSettings: " + mMinSendSms );
    }
    //endregion

    //region Button Handlers

    /**
     * Cancels actual driving session
     */
    private void cancelDrive() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(android.R.string.cancel);
        alert.setMessage(R.string.QuitRoute);

        alert.setNegativeButton(android.R.string.cancel, null);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAllowReturn = true;
                onBackPressed();
            }
        });

        alert.show();
    }

    //endregion

    //region GMaps

    /**
     * After map initialisation is ready this method is called
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Redirect to GMaps disabled
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Turn on the my Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation(false);


    }

    /**
     * Gets actual route from google API and draws it into the map.
     * Also sets markers for every waypoint
     */
    private void showRouteOnMap() {
        for (Waypoint w: mActualWaypoints) {
            addWaypointMarkerToMap(new LatLng(w.getLatitude(), w.getLongitude()),
                    w.getFirstName() + " " + w.getLastName(), true);
        }
        addWaypointMarkerToMap(new LatLng(mActualRoute.getLatitude(),
                mActualRoute.getLongitude()), "Destination", false);

        List<LatLng> path = mRouteHelper.getRoute(mActualWaypoints,
                new com.google.maps.model.LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()),
                new com.google.maps.model.LatLng(mActualRoute.getLatitude(), mActualRoute.getLongitude()));

        mMap.addPolyline(new PolylineOptions()
                .addAll(path)
                .width(INITIAL_STROKE_WIDTH_PX)
                .color(Color.BLUE)
                .geodesic(true));

        updateMapCamera();
        startDriveControl();
    }

    /**
     * Draws Markers into the map.
     * @param latLng Position of the marker
     * @param title Name of the marker
     * @param isWaypoint If marker is a waypoint another symbol is used
     */
    private void addWaypointMarkerToMap(LatLng latLng, String title, boolean isWaypoint) {
        Marker newMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
        );
        if (isWaypoint) {
            newMarker.setIcon(bitmapDescriptorFromVector(this, R.drawable.ic_person_pin_red_40dp));
        }
        // newMarker.setTag(wp);
        mMarkers.add(newMarker);
        //updateMapCamera();
    }

    /**
     * Recenters map view
     */
    private void updateMapCamera() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
        for (Marker marker : mMarkers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        int padding = 100;  // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
    }

    /**
     * Updates actual position button on the map and asks for permissions if necessary
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Get the actual location of the device with gps
     * @param startDriveUpdate set true if a drive update should be triggered
     */
    private void getDeviceLocation(final boolean startDriveUpdate) {
        try {
            if (mLocationPermissionGranted) {
                if (LocationHelper.isLocationEnabled(this)) {
                    Task locationResult = mFusedLocationProviderClient.getLastLocation();
                    locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful()) {
                                // Set the map's camera position to the current location of the device.
                                mLastKnownLocation = task.getResult();
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                if (startDriveUpdate) {
                                    UpdateDrive();
                                } else {
                                    showRouteOnMap();
                                }

                            } else {
                                Log.d(TAG, "Current location is null.");
                                Log.e(TAG, "Exception: %s", task.getException());
                                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            }
                        }
                    });
                } else {
                    showGPSDisabledDialog();
                }
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Let user allow the use of the actual location
     */
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            updateLocationUI();
            getDeviceLocation(false);
        } else {
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Show Dialog that GPS is necessary
     */
    private void showGPSDisabledDialog() {
        if (mGPSDialog == null) {
            mGPSDialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.activate_location_title))
                    .setMessage(getString(R.string.active_location_message))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            openLocationSettings();
                            mGPSDialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    }).create();
        }
        mGPSDialog.show();
    }

    /**
     * Opens settings with location site. User can easily turn on GPS now.
     */
    private void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    /**
     * Needed for custom marker image on map.
     * @param context actual context
     * @param vectorResId picture resource id
     * @return BitmapDescriptor for the google maps marker image
     */
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    //endregion
}
