package chongxuocmanhinh.sound_plusplus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by L on 29/12/2016.
 */
public class PreferencesActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener{
    /**
     * Initialize the activity, loading the preference specifications.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        ThemeHelper.setTheme(this, R.style.BackActionBar);
        super.onCreate(savedInstanceState);
        PlaybackService.getSettings(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlaybackService.getSettings(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target)
    {
        ArrayList<Header> tmp = new ArrayList<Header>();
        loadHeadersFromResource(R.xml.preference_headers, tmp);

        for(Header obj : tmp) {
            // Themes are 5.x only, so do not add PreferencesTheme on holo devices
            if (!ThemeHelper.usesHoloTheme() || !obj.fragment.equals(PreferencesTheme.class.getName()))
                target.add(obj);
        }
    }

    @Override
    public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
        if (PrefKeys.SELECTED_THEME.equals(key)) {
            // this gets called by all preference instances: we force them to redraw
            // themselfes if the theme changed
            recreate();
        }
    }


    //Audio Fragment
    public static class AudioFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_audio);
        }
    }

    public static class LibraryFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_library);
        }
    }

    public static class NotificationsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_notifications);
        }
    }

    public static class ShakeFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_shake);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
}
