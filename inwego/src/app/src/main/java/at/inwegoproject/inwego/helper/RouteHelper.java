package at.inwegoproject.inwego.helper;

import android.content.Context;
import android.util.Log;
import at.inwegoproject.inwego.R;
import at.inwegoproject.inwego.domain.RouteDetails;
import at.inwegoproject.inwego.domain.Waypoint;

import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.*;

import java.util.ArrayList;
import java.util.List;

import static android.support.constraint.Constraints.TAG;


/**
 * This class helps by requesting a route from Google Directions API
 */
public class RouteHelper {

    private Context mContext;

    public RouteHelper(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Get Route Details from the Google Directions API
     * @param waypoints List of waypoints
     * @param startPoint Start position of the route
     * @param destination Destination of the route
     * @return List of Route Details for every waypoint
     */
    public List<RouteDetails> getRouteDetails(List<Waypoint> waypoints, LatLng startPoint, LatLng destination) {
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(mContext.getString(R.string.google_maps_key))
                .build();


        DirectionsApiRequest request = DirectionsApi.newRequest(context)
                .destination(destination)
                .origin(startPoint);

        List<RouteDetails> routeDetails = new ArrayList<>();

        LatLng[] LatLngWaypoints = new LatLng[waypoints.size()];

        int x = 0;
        for (Waypoint w :
                waypoints) {
            LatLngWaypoints[x] = new LatLng(w.getLatitude(), w.getLongitude());
            x++;
        }

        request.waypoints(LatLngWaypoints);

        try {
            DirectionsResult res = request.await();

            //Loop through legs and steps to get encoded polylines of each step
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];

                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        routeDetails.add(new RouteDetails(leg.distance.humanReadable,
                                leg.distance.inMeters,
                                leg.duration.humanReadable,
                                leg.duration.inSeconds));
                    }
                }
            }
        } catch(Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage());
        }

        return routeDetails;
    }

    /**
     * Get route points to draw a route on the map
     * @param waypoints List of waypoints on the route
     * @param startPoint Start point of the route
     * @param destination Destination of the route
     * @return List of route points to draw the route in the google map
     */
    public List<com.google.android.gms.maps.model.LatLng> getRoute(List<Waypoint> waypoints, LatLng startPoint, LatLng destination) {
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(mContext.getString(R.string.google_maps_key))
                .build();


        DirectionsApiRequest request = DirectionsApi.newRequest(context)
                .destination(destination)
                .origin(startPoint);

        List<com.google.android.gms.maps.model.LatLng> path = new ArrayList<>();
        LatLng[] LatLngWaypoints = new LatLng[waypoints.size()];

        int x = 0;
        for (Waypoint w :
                waypoints) {
            LatLngWaypoints[x] = new LatLng(w.getLatitude(), w.getLongitude());
            x++;
        }

        request.waypoints(LatLngWaypoints);

        try {
            DirectionsResult res = request.await();

            //Loop through legs and steps to get encoded polylines of each step
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];

                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];

                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++){
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        if (points1 != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                            for (com.google.maps.model.LatLng coord1 : coords1) {
                                                path.add(new com.google.android.gms.maps.model.LatLng(coord1.lat, coord1.lng));
                                            }
                                        }
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    if (points != null) {
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            path.add(new com.google.android.gms.maps.model.LatLng(coord.lat, coord.lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception ex) {
             Log.e(TAG, ex.getLocalizedMessage());
        }

        return path;
    }
}
