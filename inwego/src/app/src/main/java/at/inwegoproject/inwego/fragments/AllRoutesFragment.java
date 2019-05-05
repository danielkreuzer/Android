package at.inwegoproject.inwego.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import at.inwegoproject.inwego.InwegoApplication;
import at.inwegoproject.inwego.R;
import at.inwegoproject.inwego.activities.Drive;
import at.inwegoproject.inwego.db.DBHelper;
import at.inwegoproject.inwego.domain.Route;
import at.inwegoproject.inwego.domain.Waypoint;
import at.inwegoproject.inwego.fragments.viewholder.RoutesAdapter;

/**
 * Fragment where all saved routes are shown
 */
public class AllRoutesFragment extends Fragment {
    private static final String TAG = MapFragment.class.getSimpleName();
    private final DBHelper dbHelper = InwegoApplication.getInstance().getDatabaseManager();

    private RecyclerView mRecyclerView;
    private TextView mRecyclerViewPlaceholder;
    private RoutesAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    /**
     * Lifecycle method of the fragment.
     * Inflates the fragment defined by the layout file.
     * Initializes references to layout objects.
     * Registers list adapters and OnClickListeners.
     * Loads all available routes from the db.
     * @return the inflated {@link View} object
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routes, container, false);
        mRecyclerView = view.findViewById(R.id.list_all_routes);
        mRecyclerViewPlaceholder = view.findViewById(R.id.routes_list_empty);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        final List<Route> allRoutes = dbHelper.getAllRoutes();
        if(allRoutes.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mRecyclerViewPlaceholder.setVisibility(View.VISIBLE);
        }
        else {
            mAdapter = new RoutesAdapter(allRoutes, getContext());
            mAdapter.setOnCardClickListener(new RoutesAdapter.OnCardClickListener() {
                @Override
                public void OnCardClicked(View view, int position) {
                    Route currentRoute = allRoutes.get(position);
                    // get waypoints from db
                    List<Waypoint> waypointsToRoute = dbHelper.getWaypointsForRoute(currentRoute.getId());
                    Intent intent = new Intent(getContext(), Drive.class);
                    intent.putExtra(MapFragment.INTENT_KEY_ROUTE, currentRoute);
                    ArrayList<Waypoint> waypoints = new ArrayList<>();
                    for (Waypoint x:
                            waypointsToRoute.toArray(new Waypoint[0])) {
                        waypoints.add(x);
                    }
                    intent.putParcelableArrayListExtra(MapFragment.INTENT_KEY_WAYPOINTS, waypoints);
                    startActivity(intent);
                }
            });
            mRecyclerView.setAdapter(mAdapter);
        }
        return view;
    }
}
