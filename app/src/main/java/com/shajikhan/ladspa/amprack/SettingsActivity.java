package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "Settings";
    public AudioManager audioManager ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                        }
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class MessagesFragment extends PreferenceFragmentCompat {
        AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput ;
        int defaultInputDevice = 0 ;
        int defaultOutputDevice = 0 ;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.messages_preferences, rootKey);
            ListPreference listPreference = findPreference("input");
            ListPreference listPreferenceOutput = findPreference("output");

            ArrayList<CharSequence> entries = new ArrayList<>();
            ArrayList<CharSequence> entryValues = new ArrayList<>();
            SettingsActivity settingsActivity = (SettingsActivity) getActivity();
            AudioManager audioManager = settingsActivity.audioManager;

            audioDevicesInput = audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
            audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

            entries.add("Default");
            entryValues.add("-1");

            for (int i = 0 ; i < audioDevicesInput.length ; i ++) {
                String name = MainActivity.typeToString(audioDevicesInput[i].getType());
                int deviceID = audioDevicesInput[i].getId();
//                name = (String) audioDevicesInput[i].getProductName();
                entries.add(name + " (" + (String) audioDevicesInput[i].getProductName() + ")");
//                entryValues.add(String.valueOf(audioDevicesInput[i]));
                entryValues.add(String.valueOf(deviceID));
                Log.d(TITLE_TAG, "onCreatePreferences: " + String.format ("%s: %s", deviceID, name));
            }

            listPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
            listPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    AudioEngine.setRecordingDeviceId(new Integer(newValue.toString()));
                    return true;
                }
            });

            entries.clear();
            entryValues.clear();

            entries.add("Default");
            entryValues.add("-1");

            for (int i = 0 ; i < audioDevicesOutput.length ; i ++) {
                String name = MainActivity.typeToString(audioDevicesOutput[i].getType());
                int deviceID = audioDevicesOutput[i].getId();
//                name = (String) audioDevicesOutput[i].getProductName();
                entries.add(name + " (" + (String) audioDevicesOutput[i].getProductName() + ")");
                entryValues.add(String.valueOf(deviceID));
                Log.d(TITLE_TAG, "onCreatePreferences: " + String.format ("%s: %s", deviceID, name));
            }

            listPreferenceOutput.setEntries(entries.toArray(new CharSequence[entries.size()]));
            listPreferenceOutput.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

            listPreferenceOutput.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(getClass().getSimpleName(), "onPreferenceChange: [playbackDeviceID] " + newValue.toString());
//                    AudioEngine.setPlaybackDeviceId(Integer.parseInt(newValue.toString()));
                    return true;
                }
            });
        }
    }

    public static class SyncFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sync_preferences, rootKey);
            if (MainActivity.proVersion == false) {
                ListPreference export = findPreference("export_format");
                export.setSummary("More export formats are available in the Pro Version");
                export.setShouldDisableView(true);
                export.setEnabled(false);
            }
        }
    }

    public static class ThemeFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.theme_settings, rootKey);
            Preference preference = findPreference("background_custom");
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent_upload = new Intent();
                    intent_upload.setType("image/*");
                    intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent_upload,1);
                    return true;
                }
            });
        }
    }

    public static class ProfileFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.account_pref, rootKey);
            FirebaseAuth auth = FirebaseAuth.getInstance() ;
            if (auth == null || auth.getUid() == null)
                return;

            String email = auth.getCurrentUser().getEmail();
            Preference preference = findPreference("email");
            preference.setTitle(email);
        }
    }

    public static class VersionFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.version_info, rootKey);
            findPreference("name_info").setTitle("AmpRack Pro Version");
            findPreference("build_number").setSummary(R.string.build_id);
            findPreference("build_number").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    HashCommands commands = new HashCommands(getContext());
                    commands.show();
                    return false;
                }
            });
            findPreference("build_name").setSummary(R.string.app_version);
//            int plugins = AudioEngine.getTotalPlugins() ;
            int plugins = MainActivity.totalPlugins;
            findPreference("plugins").setSummary(String.valueOf(plugins));

            if (MainActivity.proVersion == false) {
                findPreference("encoders_supported").setSummary("WAV 16 Bit");
            }
        }
    }

    public static class TracksFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.tracks_settings, rootKey);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (resultCode == RESULT_OK) {
            Uri selectedImage = imageReturnedIntent.getData();
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;
            Log.d(TITLE_TAG, "onActivityResult: " + selectedImage.getPath());

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPreferences.edit().putString("background", selectedImage.toString()).commit();
        }
    }
}