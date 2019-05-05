package at.inwegoproject.inwego.fragments.viewholder;

import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import at.inwegoproject.inwego.R;

/**
 * ViewHolder which stores a single route in a {@link RecyclerView}
 */
public class RoutesViewHolder extends RecyclerView.ViewHolder {
    public TextView mRouteName;
    public TextView mDestinationName;
    /**
     * The card layout surrounding the layout.
     * Enables bindings of click listeners.
     */
    public CardView mCardView;
    public AppCompatImageButton mDeleteRoute;

    /**
     * Creates a new RoutesViewHolder and binds the layout objects for given view.
     * @param v View which contains the layout
     */
    public RoutesViewHolder(View v) {
        super(v);
        mCardView = v.findViewById(R.id.card_route);
        mRouteName = v.findViewById(R.id.txt_route_name);
        mDestinationName = v.findViewById(R.id.txt_destination_address);
        mDeleteRoute = v.findViewById(R.id.btn_delete_route);
    }
}
