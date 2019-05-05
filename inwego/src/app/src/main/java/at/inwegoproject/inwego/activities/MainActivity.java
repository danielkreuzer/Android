package at.inwegoproject.inwego.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import at.inwegoproject.inwego.R;
import at.inwegoproject.inwego.fragments.AllRoutesFragment;
import at.inwegoproject.inwego.fragments.MapFragment;

/**
 * Activity where all the magic happens.
 * All children of the activity are fragments.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DrawerLayout.DrawerListener {

    private DrawerLayout mDrawer;
    private NavigationView mNavigationView;

    private Fragment currentFragment;
    private Fragment nextFragment;

    /**
     * Lifecycle method of the activity.
     * Layout and listeners are bound here.
     * Current fragment is set to {@link MapFragment}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationView = findViewById(R.id.nav_view);
        mDrawer = findViewById(R.id.drawer_layout);

        mNavigationView.setNavigationItemSelectedListener(this);
        mDrawer.addDrawerListener(this);

        // init fragment
        nextFragment = new MapFragment();
        setNextFragment();
    }

    /**
     * Handles clicks in the navigation drawer.
     * Instantiates the next fragment and closes the drawer.
     * @param menuItem the clicked menu item
     * @return boolean, if the navigation was successful
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        // save nextFragment in variable to remove lag when navigating
        switch (menuItem.getItemId()) {
            case R.id.nav_item_map: {
                nextFragment = new MapFragment();
                break;
            }
            case R.id.nav_item_routes: {
                nextFragment = new AllRoutesFragment();
                break;
            }
            case R.id.nav_item_settings: {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            }
            default: {
                return false;
            }
        }
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Sets the next fragment of the drawer content which has be set beforehand.
     * Fragment is only replaces if it's not loaded already
     */
    protected void setNextFragment() {
        if (currentFragment == null ||
                !currentFragment.getClass().getName().equals(nextFragment.getClass().getName())) {
            currentFragment = nextFragment;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.drawer_content, nextFragment)
                    .commit();
        }
    }

    @Override
    public void onDrawerSlide(@NonNull View view, float v) {
    }

    @Override
    public void onDrawerOpened(@NonNull View view) {
    }

    /**
     * Handles the event when the drawer was closed
     * Calls the setNextFragment() method.
     * This is done here to prevent the lag of closing the navigation drawer.
     */
    @Override
    public void onDrawerClosed(@NonNull View view) {
        setNextFragment();
    }

    @Override
    public void onDrawerStateChanged(int i) {
    }
}
