package org.getlantern.lantern.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ApplicationInfo; 
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.MenuItem; 
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getlantern.lantern.config.LanternConfig;
import org.getlantern.lantern.model.UI;
import org.getlantern.lantern.R;
import org.getlantern.lantern.service.LanternVpn;


public class LanternMainActivity extends ActionBarActivity implements Handler.Callback {

    private static final String TAG = "LanternMainActivity";
    private static final String PREFS_NAME = "LanternPrefs";

    private SharedPreferences mPrefs = null;

    private UI UI;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }

        mPrefs = getSharedPrefs(getApplicationContext());

        setContentView(R.layout.activity_lantern_main);

        // setup our UI
        try { 
            UI = new UI(this, mPrefs);
            UI.setupSideMenu();
            UI.setupStatusToast();
            // configure actions to be taken whenever slider changes state
            UI.setupLanternSwitch();
            PromptVpnActivity.UI = UI;
        } catch (Exception e) {
            Log.d(TAG, "Got an exception " + e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // we check if mPrefs has been initialized before
        // since onCreate and onResume are always both called
        if (mPrefs != null) {
            UI.setBtnStatus();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (mPrefs != null) {
                mPrefs.edit().remove(LanternConfig.PREF_USE_VPN);
                mPrefs.edit().clear().commit();
            }
            stopLantern();
            // give Lantern a second to stop
            Thread.sleep(1000);
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    public void quitLantern() {
        try {
            stopLantern();
            if (mPrefs != null) {
                mPrefs.edit().remove(LanternConfig.PREF_USE_VPN).commit();
            }
            Log.d(TAG, "About to exit Lantern...");
            // sleep for a few ms before exiting
            Thread.sleep(200);

            finish();
            moveTaskToBack(true);

        } catch (Exception e) {

        }
    }


    // update START/STOP power Lantern button
    // according to our stored preference

    public SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            //Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    // Prompt the user to enable full-device VPN mode
    public void enableVPN() {
        Log.d(TAG, "Load VPN configuration");
        Thread thread = new Thread() {
            public void run() { 
                Intent intent = new Intent(LanternMainActivity.this, PromptVpnActivity.class);
                if (intent != null) {
                    startActivity(intent);
                }
            }
        };
        thread.start();
    }

    public void stopLantern() {
        Log.d(TAG, "Stopping Lantern...");
        try {
            Thread thread = new Thread() {
                public void run() { 

                    Intent service = new Intent(LanternMainActivity.this, LanternVpn.class);
                    if (service != null) {
                        service.setAction(LanternConfig.DISABLE_VPN);
                        startService(service);
                        UI.toggleSwitch(false);
                    }
                }
            };
            thread.start();
        } catch (Exception e) {
            Log.d(TAG, "Got an exception trying to stop Lantern: " + e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle
        // If it returns true, then it has handled
        // the nav drawer indicator touch event
        if (UI.optionSelected(item)) {
            return true;
        }

        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        UI.syncState();
    }
}
