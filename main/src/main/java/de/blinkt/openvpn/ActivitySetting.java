/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;

import de.blinkt.openvpn.activities.BaseActivity;

//import com.mixpanel.android.mpmetrics.MixpanelAPI;


public class ActivitySetting extends BaseActivity {
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
        setContentView(R.layout.setting);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setTitle("Settings");


        setupSpinnerProto();
        
        m_prop = new Properties();
        try {
            FileInputStream fi = new FileInputStream(getFilesDir() + "/setting.xml");
            m_prop.loadFromXML(fi);
            
            EditText editPort = (EditText)findViewById(R.id.edit_setting_port);
            editPort.setText(m_prop.getProperty("PORT"));
            Spinner spinProto = (Spinner)findViewById(R.id.spinner_setting_prototype);
            spinProto.setSelection(m_prop.getProperty("PROTOCOL").equalsIgnoreCase("UDP") ? 0 : 1);
        } catch(Exception e) {
            System.out.println(e);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();

    }
     
    @Override
    protected void onStop() {
        super.onStop();     

    }
    
    protected void setupSpinnerProto()
    {
        Spinner spinProto = (Spinner)findViewById(R.id.spinner_setting_prototype);
        ArrayList<String> aProto = new ArrayList<String>();
        aProto.add("UDP");
        aProto.add("TCP");
        
        String[] arrayProto = new String[aProto.size()];
        arrayProto = aProto.toArray(arrayProto);
        ArrayAdapter<String> adapterProto = new ArrayAdapter<String>(this, R.layout.spinner_text, arrayProto);
        class ProtoSelectedListener implements OnItemSelectedListener {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Log.d("ibVPN", "Protocol Selected:" + String.valueOf(arg2));
            }  
            public void onNothingSelected(AdapterView<?> arg0) {
            }  
        }
        adapterProto.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);  
        spinProto.setAdapter(adapterProto);  
        spinProto.setOnItemSelectedListener(new ProtoSelectedListener());
    }
    
    public void onSave(View v) {
        EditText editPort = (EditText)findViewById(R.id.edit_setting_port);
        Spinner spinProto = (Spinner)findViewById(R.id.spinner_setting_prototype);
        String oldport = m_prop.getProperty("PORT");
        try {
            int port = Integer.parseInt(editPort.getText().toString());
            if(port != 1197 && port != 1196) {
                Toast toast = Toast.makeText(this, "Port should be 1195 or 1196.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
            	toast.show();
            	
                JSONObject props = new JSONObject();
                props.put("Error Type", "Port should be 1196 or 1197");
                //mixpanelTrack("Error Message at Adv. Settings", props);
                return;
            }
            m_prop.setProperty("PORT", editPort.getText().toString());
            m_prop.setProperty("PROTOCOL", spinProto.getSelectedItem().toString());
            
            FileOutputStream fo = new FileOutputStream(getFilesDir() + "/setting.xml");
            m_prop.storeToXML(fo, null);
        } catch(Exception e) {
            System.out.println(e);
        }
        
        JSONObject props = new JSONObject();
        mixpanelAdd(props, "Selected Protocol", spinProto.getSelectedItem().toString());
        mixpanelAdd(props, "Used Port", oldport);
        mixpanelAdd(props, "New Port", editPort.getText().toString());
        //mixpanelTrack("Save Advanced Settings", props);
        
        Toast toast = Toast.makeText(this, "Setting has saved.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
    	toast.show();
        finish();
    }
    
    public void onCancel(View v) {
        EditText editPort = (EditText)findViewById(R.id.edit_setting_port);
        Spinner spinProto = (Spinner)findViewById(R.id.spinner_setting_prototype);
        
        JSONObject props = new JSONObject();
        mixpanelAdd(props, "Selected Protocol", spinProto.getSelectedItem().toString());
        mixpanelAdd(props, "Used Port", editPort.getText().toString());
        mixpanelAdd(props, "New Port", editPort.getText().toString());
        //mixpanelTrack("Cancel Advanced Settings", props);
        
        finish();
    }
}
