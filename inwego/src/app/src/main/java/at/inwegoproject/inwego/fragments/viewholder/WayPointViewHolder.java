package at.inwegoproject.inwego.fragments.viewholder;

import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import at.inwegoproject.inwego.R;

/**
 * ViewHolder which stores a single waypoint in a {@link RecyclerView}
 */
public class WayPointViewHolder extends RecyclerView.ViewHolder {
    public TextView mLocationName;
    public AppCompatImageButton mDeleteWaypoint;

    /**
     * Creates a new WayPointViewHolder and binds the layout objects for given view.
     * @param v View which contains the layout
     */
    public WayPointViewHolder(View v) {
        super(v);
        mLocationName = v.findViewById(R.id.txt_address);
        mDeleteWaypoint = v.findViewById(R.id.btn_delete_waypoint);
    }
}
