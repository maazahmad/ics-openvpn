/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import de.blinkt.openvpn.activities.BaseActivity;

//import com.mixpanel.android.mpmetrics.MixpanelAPI;


public class ActivityEntrance extends BaseActivity {
    protected static final String MIXPANEL_TOKEN = "807d7275a563b23cd31b0aad50e63f4f";

    protected void mixpanelAdd(JSONObject props, String name, String value) {
        try {
            props.put(name, value);
        } catch(Exception e) {
            e.printStackTrace();
            Log.d("ibVPN", "Error: " + e.toString());
        }
    }

    /*protected void mixpanelTrack(String name, JSONObject props) {
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, MIXPANEL_TOKEN);
        mixpanel.track(name, props);
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // set theme by code, this will improve the speed.
//        setTheme(R.style.App_Theme);
        setContentView(R.layout.entrance);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
    }
     
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    public void onLogin(View v) {
        Intent intent = new Intent(this, ActivityLogin.class);
        startActivity(intent);
        //mixpanelTrack("Login onHome", null);
    }
    
    public void onRegister(View v) {
        // goto register page.
        Intent intent = new Intent(this, ActivityRegister.class);
        startActivity(intent);
        //mixpanelTrack("Register onHome", null);
    }
}
