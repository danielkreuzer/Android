package at.inwegoproject.inwego.fragments.viewholder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import at.inwegoproject.inwego.R;
import at.inwegoproject.inwego.domain.Waypoint;

/**
 * List adapter for handling waypoints in an {@link RecyclerView}
 */
public class WayPointAdapter extends RecyclerView.Adapter<WayPointViewHolder> {
    private List<Waypoint> mWayPointList;

    /**
     * Creates a new WayPointAdapter
     */
    public WayPointAdapter() {
        mWayPointList = new ArrayList<>();
    }

    /**
     * Inflates the layout when a new view holder is created
     */
    @NonNull
    @Override
    public WayPointViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.route_list_item, viewGroup, false);
        WayPointViewHolder vh = new WayPointViewHolder(v);
        return vh;
    }

    /**
     * Binds the data of a given index in the list to the view holder
     * @param wayPointViewHolder the ViewHolder to be bound to
     * @param i index the data comes from
     */
    @Override
    public void onBindViewHolder(@NonNull final WayPointViewHolder wayPointViewHolder, int i) {
        // get element from the dataset at position i
        // replace the contents of the view with that element
        wayPointViewHolder.mLocationName.setText(mWayPointList.get(i).getAddress());
        wayPointViewHolder.mDeleteWaypoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get adapter position to get correct index if user has deleted an item before
                WayPointAdapter.this.removeWaypoint(wayPointViewHolder.getAdapterPosition());
            }
        });
    }

    /**
     * Returns the number of items in the adapter
     */
    @Override
    public int getItemCount() {
        return mWayPointList.size();
    }

    /**
     * Returns all waypoints which are stored in the adapter
     */
    public List<Waypoint> getAllWayPoints() {
        return mWayPointList;
    }

    /**
     * Adds a new waypoint to the adapter
     * @param wp to waypoint to be added
     */
    public void addWaypoint(Waypoint wp) {
        this.mWayPointList.add(wp);
        notifyDataSetChanged();
    }

    /**
     * Removes a waypoint from the adapter by a given index
     * @param i the index to be removed
     */
    public void removeWaypoint(int i) {
        this.mWayPointList.remove(i);
        notifyItemRemoved(i);
    }
}