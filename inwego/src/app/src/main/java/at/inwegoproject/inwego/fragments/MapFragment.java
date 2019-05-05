package at.inwegoproject.inwego.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.rilixtech.materialfancybutton.MaterialFancyButton;

import java.util.ArrayList;
import java.util.List;

import at.inwegoproject.inwego.InwegoApplication;
import at.inwegoproject.inwego.R;
import at.inwegoproject.inwego.activities.Drive;
import at.inwegoproject.inwego.db.DBHelper;
import at.inwegoproject.inwego.domain.Route;
import at.inwegoproject.inwego.domain.Waypoint;
import at.inwegoproject.inwego.fragments.viewholder.WayPointAdapter;
import at.inwegoproject.inwego.helper.LocationHelper;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Fragment where waypoints and the destination can be set.
 * Includes the {@link MapFragment} of the Google Maps SDK
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = MapFragment.class.getSimpleName();

    /**
     * Key for the routes which are stored in an intent
     */
    public static final String INTENT_KEY_ROUTE = "INTENT_DATA_ROUTE";
    /**
     * Key for the waypoints which are stored in an intent
     */
    public static final String INTENT_KEY_WAYPOINTS = "INTENT_DATA_WAYPOINTS";

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 33;
    private static final int WAYPOINT_AUTOCOMPLETE_REQUEST_CODE = 111;
    private static final int DESTINATION_AUTOCOMPLETE_REQUEST_CODE = 222;
    private static final int RESULT_PICK_CONTACT = 333;
    private static final int DEFAULT_ZOOM = 13;

    private SupportMapFragment mMapFragment;
    private boolean mLocationPermissionGranted;
    private GoogleMap mMap;
    private List<Marker> mMarkers = new ArrayList<>();
    private Marker mMarkerLastClicked;

    private RecyclerView mRecyclerView;
    private TextView mRecyclerViewPlaceholder;
    private MaterialFancyButton mBtnAddStopOver;
    private EditText mTxtDestination;
    private FloatingActionButton mFabSaveRoute;
    private FloatingActionButton mFabStartNavigation;

    private WayPointAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private LatLng mDestination;
    private Marker mDestinationMarker;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private AlertDialog mGPSDialog;

    /**
     * BroadcastReceiver for receiving changes of the GPS state
     */
    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
                if(!LocationHelper.isLocationEnabled(getContext())) {
                    showGPSDisabledDialog();
                }
            }
        }
    };

    private final DBHelper dbHelper = InwegoApplication.getInstance().getDatabaseManager();

    /**
     * Lifecycle method of the fragment.
     * Inflates the fragment defined by the layout file.
     * Initializes references to layout objects.
     * Initializes the MapFragment and the required location client.
     * Registers list adapters and OnClickListeners.
     * @return the inflated {@link View} object
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        if (mMapFragment == null) {
            mMapFragment = SupportMapFragment.newInstance();
            mMapFragment.getMapAsync(this);
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.getActivity());
        }
        getChildFragmentManager().beginTransaction().replace(R.id.map_create_route, mMapFragment).commit();

        mRecyclerView = view.findViewById(R.id.waypoint_list);
        mRecyclerViewPlaceholder = view.findViewById(R.id.waypoint_list_empty);
        mBtnAddStopOver = view.findViewById(R.id.btn_add_waypoint);
        mTxtDestination = view.findViewById(R.id.txt_destination);
        mFabSaveRoute = view.findViewById(R.id.fab_save_route);
        mFabStartNavigation = view.findViewById(R.id.fab_start_navigation);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new WayPointAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                MapFragment.this.invalidateActionButtons();
                MapFragment.this.invalidateRecyclerViewVisibility();
                removeWaypointMarkerFromMap(positionStart);
            }
        });
        invalidateActionButtons();
        invalidateRecyclerViewVisibility();

        mBtnAddStopOver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPlaceAutocomplete(WAYPOINT_AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        mTxtDestination.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    openPlaceAutocomplete(DESTINATION_AUTOCOMPLETE_REQUEST_CODE);
                }
            }
        });
        mTxtDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPlaceAutocomplete(DESTINATION_AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        mFabSaveRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveRouteDialog();
            }
        });

        mFabStartNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create temporary route
                Route route = new Route();
                route.setLatitude(mDestination.latitude);
                route.setLongitude(mDestination.longitude);
                route.setAddress(mTxtDestination.getText().toString());

                Intent intent = new Intent(getContext(), Drive.class);
                intent.putExtra(INTENT_KEY_ROUTE, route);
                ArrayList<Waypoint> waypoints = new ArrayList<>();
                for (Waypoint x:
                        mAdapter.getAllWayPoints().toArray(new Waypoint[0])) {
                    waypoints.add(x);
                }
                intent.putParcelableArrayListExtra(MapFragment.INTENT_KEY_WAYPOINTS, waypoints);
                startActivity(intent);
            }
        });

        return view;
    }

    /**
     * Lifecycle method of the fragment.
     * Unregisters broadcast receivers.
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mGpsSwitchStateReceiver);
    }

    /**
     * Lifecycle method of the fragment.
     * Registers broadcast receivers.
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        if(!LocationHelper.isLocationEnabled(getContext())) {
            showGPSDisabledDialog();
        }
    }

    //region Permission Handling
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            updateLocationUI();
            getDeviceLocation();
        } else {
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the results of permission requests in newer android versions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                } else {
                    this.getActivity().finishAndRemoveTask();
                }
            }
        }
        updateLocationUI();
        getDeviceLocation();
    }
    //endregion

    //region Google Maps API Handling

    /**
     *  Callback when the MapFragment has been initialized successfully
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mDestination == null || !mDestination.equals(marker.getPosition())) {
                    mMarkerLastClicked = marker;
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
                }
            }
        });
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Turn on the my Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    /**
     * Enables or disables the current location layer in the MapFragment
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
     * Gets the current device location and saves it in {@code mLastKnownLocation}
     */
    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                if (LocationHelper.isLocationEnabled(getContext())) {
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
     * Shows a dialog, which forces the user to enable location services
     */
    private void showGPSDisabledDialog() {
        if (mGPSDialog == null) {
            mGPSDialog = new AlertDialog.Builder(getContext())
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
     * Sends an intent, which opens the settings page for the location services
     */
    private void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    /**
     * Sets a marker on the map for a given waypoint and saves it in the {@code mMarkers} list.
     * Invalidates the map camera.
     * @param wp the waypoint to be added
     */
    private void addWaypointMarkerToMap(Waypoint wp) {
        Marker newMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(wp.getLatitude(), wp.getLongitude()))
                .title(getString(R.string.specify_contact))
        );
        newMarker.setTag(wp);
        mMarkers.add(newMarker);
        updateMapCamera();
    }

    /**
     * Removes the marker with the given index from the map and the markers list
     * @param i index to be removed
     */
    private void removeWaypointMarkerFromMap(int i) {
        Marker deletedMarker = mMarkers.remove(i);
        deletedMarker.remove();
        updateMapCamera();
    }

    /**
     * Updates the map camera such that all markers, the current location and the destination location can be seen.
     */
    private void updateMapCamera() {
        if (mMarkers.isEmpty() && mDestination == null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
            if (mDestinationMarker != null) {
                builder.include(mDestinationMarker.getPosition());
            }
            for (Marker marker : mMarkers) {
                builder.include(marker.getPosition());
            }
            LatLngBounds bounds = builder.build();
            int padding = 100;  // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        }
    }

    /**
     * Converts a vector resource to an bitmap descriptor, needed for icons in the MapFragment
     * @return a {@link BitmapDescriptor} containing the given resource
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

    //region Google Places API Handling

    /**
     * Opens the Google Places API overlay
     * @param requestCode the request code to process the result of the request
     */
    private void openPlaceAutocomplete(int requestCode) {
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .build(getActivity());
            startActivityForResult(intent, requestCode);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Add selected place (of the Places API) overlay to the list adapter.
     * Calls 'addWaypointMarkerToMap' with the created waypoint.
     * @param place the selected place as an {@link Place} model object
     */
    private void addWaypointToAdapter(Place place) {
        LatLng placeLatLng = place.getLatLng();
        Waypoint wp = new Waypoint();
        wp.setAddress(place.getAddress().toString());
        wp.setLatitude(placeLatLng.latitude);
        wp.setLongitude(placeLatLng.longitude);
        mAdapter.addWaypoint(wp);
        addWaypointMarkerToMap(wp);
        invalidateActionButtons();
        invalidateRecyclerViewVisibility();
    }
    //endregion

    /**
     * Handles responses of called intents
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == WAYPOINT_AUTOCOMPLETE_REQUEST_CODE || requestCode == DESTINATION_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this.getContext(), data);
                Log.i(TAG, "Place: " + place.getName());
                if (requestCode == WAYPOINT_AUTOCOMPLETE_REQUEST_CODE) {
                    addWaypointToAdapter(place);
                } else { // DESTINATION_AUTOCOMPLETE_REQUEST_CODE
                    mDestination = place.getLatLng();
                    mTxtDestination.setText(place.getAddress());
                    if(mDestinationMarker != null) {
                        mDestinationMarker.remove();
                    }
                    mDestinationMarker = mMap.addMarker(new MarkerOptions()
                            .position(mDestination)
                            .title(getString(R.string.destination))
                            .snippet(place.getAddress().toString())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    updateMapCamera();
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this.getContext(), data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "User cancelled places search");
            }
        } else if (requestCode == RESULT_PICK_CONTACT) {
            if (resultCode == RESULT_OK) {
                Cursor cursor = null;
                try {
                    String phoneNo = null;
                    String name = null;
                    Uri uri = data.getData();
                    cursor = getContext()
                            .getContentResolver()
                            .query(uri, null, null, null, null);
                    cursor.moveToFirst();
                    int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    phoneNo = cursor.getString(phoneIndex);
                    name = cursor.getString(nameIndex);

                    if (mMarkerLastClicked != null && name != null) {
                        String[] splittedName = name.split(" ");
                        String firstName = splittedName[0];
                        String lastName = null;
                        if (splittedName.length > 1) {
                            lastName = splittedName[1];
                        }
                        Waypoint wp = (Waypoint) mMarkerLastClicked.getTag();
                        wp.setFirstName(firstName);
                        wp.setLastName(lastName);
                        wp.setNumber(phoneNo);
                        mMarkerLastClicked.setTitle(firstName + ' ' + lastName);
                        mMarkerLastClicked.setSnippet(phoneNo);
                        mMarkerLastClicked.setIcon(bitmapDescriptorFromVector(getActivity(), R.drawable.ic_person_pin_red_40dp));
                        mMarkerLastClicked.hideInfoWindow();
                        mMarkerLastClicked.showInfoWindow();
                        invalidateActionButtons();
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

            } else {
                Log.i("Failed", "User cancelled contact selection!");
            }
        }
    }

    //region Invalidate Views

    /**
     * Sets the content of the {@link RecyclerView} depending on the number of items in the adapter.
     * If the adapter contains no items, an empty view is loaded.
     */
    private void invalidateRecyclerViewVisibility() {
        if (mAdapter.getItemCount() == 0) {
            mRecyclerView.setVisibility(View.GONE);
            mRecyclerViewPlaceholder.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mRecyclerViewPlaceholder.setVisibility(View.GONE);
        }
    }

    /**
     * Set the state of the floating action buttons depending on the number of waypoints and the destination.
     */
    private void invalidateActionButtons() {
        boolean allContactsSet = true;
        for (Waypoint wp : mAdapter.getAllWayPoints()) {
            if (wp.getNumber() == null || wp.getNumber().isEmpty()) {
                allContactsSet = false;
                break;
            }
        }
        if (mAdapter.getItemCount() == 0 || mDestination == null || !allContactsSet) {
            mFabStartNavigation.setEnabled(false);
            mFabSaveRoute.setEnabled(false);
        } else {
            mFabStartNavigation.setEnabled(true);
            mFabSaveRoute.setEnabled(true);
        }
    }
    //endregion

    //region Dialog handling

    /**
     * Opens a new dialog which asks the user for the name for the new route.
     * Creates a new route in the db if the OK button was clicked.
     */
    private void showSaveRouteDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

        alert.setTitle(getString(R.string.save_route));
        alert.setMessage(R.string.enter_route_name);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_single_edittext, null);
        alert.setView(v);
        final EditText txtRouteName = v.findViewById(R.id.text_singleInput_dialog);
        txtRouteName.setHint(getString(R.string.route_name));

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String routeName = txtRouteName.getText().toString();
                Route newRoute = new Route();
                newRoute.setName(routeName);
                newRoute.setLatitude(mDestination.latitude);
                newRoute.setLongitude(mDestination.longitude);
                newRoute.setAddress(mTxtDestination.getText().toString());
                long routeId = dbHelper.addRoute(newRoute);
                for (Waypoint wp : mAdapter.getAllWayPoints()) {
                    wp.setRouteId((int) routeId);
                    dbHelper.addWaypoint(wp);
                }
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = alert.show();
        // disable OK button by default
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        txtRouteName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(txtRouteName.getText().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    //endregion
}