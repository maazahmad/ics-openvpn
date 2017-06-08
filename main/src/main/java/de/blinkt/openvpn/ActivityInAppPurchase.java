/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.json.JSONObject;

import java.util.Properties;

import de.blinkt.openvpn.activities.BaseActivity;

//import com.mixpanel.android.mpmetrics.MixpanelAPI;


public class ActivityInAppPurchase extends BaseActivity {
    private Properties m_prop;
    
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
        setTheme(R.style.blinkt_lolTheme);
        setContentView(R.layout.activity_in_app_purchase);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setTitle("In App Purcahse");


    }
    
    @Override
    protected void onStart() {
        super.onStart();

    }
     
    @Override
    protected void onStop() {
        super.onStop();     

    }
    

}
