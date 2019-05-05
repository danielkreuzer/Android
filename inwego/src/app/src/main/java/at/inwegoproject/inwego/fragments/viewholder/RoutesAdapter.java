package at.inwegoproject.inwego.fragments.viewholder;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import at.inwegoproject.inwego.InwegoApplication;
import at.inwegoproject.inwego.R;
import at.inwegoproject.inwego.db.DBHelper;
import at.inwegoproject.inwego.domain.Route;

/**
 * List adapter for handling saved routes in an {@link RecyclerView}
 */
public class RoutesAdapter extends RecyclerView.Adapter<RoutesViewHolder> {
    private List<Route> mRoutesList;
    private OnCardClickListener onCardClickListener;
    private Context context;

    private final DBHelper dbHelper = InwegoApplication.getInstance().getDatabaseManager();

    /**
     * Creates a new RoutesAdapter from a collection of routes
     * @param routes A {@link Collection} of routes
     * @param context Context for building dialogs
     */
    public RoutesAdapter(Collection<Route> routes, Context context) {
        mRoutesList = new ArrayList<>(routes);
        this.context = context;
    }

    /**
     * Inflates the layout when a new view holder is created
     */
    @NonNull
    @Override
    public RoutesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.all_routes_list_item, viewGroup, false);
        RoutesViewHolder vh = new RoutesViewHolder(v);
        return vh;
    }

    /**
     * Binds the data of a given index in the list to the view holder.
     * Adds a OnClickListener which opens a new dialog if the user tries do delete a waypoint.
     * @param routesViewHolder the ViewHolder to be bound to
     * @param i index the data comes from
     */
    @Override
    public void onBindViewHolder(@NonNull final RoutesViewHolder routesViewHolder, int i) {
        Route route = mRoutesList.get(i);
        routesViewHolder.mRouteName.setText(route.getName());
        routesViewHolder.mDestinationName.setText(route.getAddress());
        routesViewHolder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onCardClickListener != null) {
                    onCardClickListener.OnCardClicked(v, routesViewHolder.getAdapterPosition());
                }
            }
        });
        routesViewHolder.mDeleteRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.delete_route_title))
                        .setMessage(context.getString(R.string.delete_route_message))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                RoutesAdapter.this.removeRoute(routesViewHolder.getAdapterPosition());
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
            }
        });
    }

    /**
     * Returns the number of items in the adapter
     */
    @Override
    public int getItemCount() {
        return mRoutesList.size();
    }

    /**
     * Removes a route from the adapter by a given index
     * @param i the index to be removed
     */
    public void removeRoute(int i) {
        Route deletedRoute = this.mRoutesList.remove(i);
        dbHelper.deleteRouteWithWaypoints(deletedRoute.getId());
        notifyItemRemoved(i);
    }

    /**
     * The {@link RecyclerView} does not provide a OnClickListener for items.
     * So use this interface.
     */
    public interface OnCardClickListener {
        void OnCardClicked(View view, int position);
    }

    /**
     * Binds a the subscriber for the clicks on cards
     */
    public void setOnCardClickListener(OnCardClickListener onCardClickListener) {
        this.onCardClickListener = onCardClickListener;
    }
}
