package se.johan.wendler.activity;

import android.os.Bundle;

import se.johan.wendler.activity.base.BaseActivity;
import se.johan.wendler.fragment.SettingsFragment;

/**
 * The activity for displaying our settings.
 */
public class SettingsActivity extends BaseActivity {

    /**
     * Called when the Activity is created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment.newInstance(), SettingsFragment.TAG)
                .commit();
    }
}
