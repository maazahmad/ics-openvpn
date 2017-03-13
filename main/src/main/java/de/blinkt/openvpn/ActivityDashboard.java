/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import de.blinkt.openvpn.activities.LogWindow;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.Preferences;

//import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class ActivityDashboard extends Activity {
    public static final int AlertDialogExitNotify = 0x90001;
    public static final int NetDisconnectedNotify = 0x90002;
    
    enum Status {
        Connecting,
        Connected,
        Disconnecting,
        Disconnected,
    }
    
    private static class ExtendHandler extends Handler {
        private ActivityDashboard m_activity;
        
        public ExtendHandler(ActivityDashboard activity) {
            setContext(activity);
        }
        
        public void setContext(ActivityDashboard activity) {
            m_activity = activity;
        }
        
        @Override
        public void handleMessage(Message msg) {
            m_activity.handleMessage(msg);
        }
    }
    
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

    private static ExtendHandler     m_handler;            // static handler to deal message.
//    private OpenVPN                  m_openvpn;            // handle for openvpn.
    private RemoteAPI                m_remote;

    private Status                   m_status;            // status of current connection.
    private String m_username;
    private String m_password;
    private String m_userid;
    private ProgressDialog m_waitdlg;
    private String m_package;
    private Timer m_timer;
    private long                     m_date;
    
    private class NetStateCheckTask extends TimerTask {
        private Context m_context;
        
        public NetStateCheckTask(Context context) {
            m_context = context;
        }
        
        public void run() {
            if(!isInternetAvailable(m_context) && m_status != Status.Disconnected) {
                m_handler.sendEmptyMessage(NetDisconnectedNotify);
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("ibVPN", "onCreate dashboard.");
        super.onCreate(savedInstanceState);
        
        // set theme by code, this will improve the speed.
//        setTheme(R.style.App_Theme);
        setContentView(R.layout.dashboard);
        
        // new the handler here, so it will not leak.
        if(m_handler == null)
            m_handler = new ExtendHandler(this);
        else
            m_handler.setContext(this);
        m_remote = new RemoteAPI(this, m_handler);
        m_timer = new Timer();
        
        setStatus(Status.Disconnected);
        
        // get data from intent.
        Intent intent = getIntent();
        m_username = intent.getStringExtra("username");
        m_password = intent.getStringExtra("password");
        m_userid = intent.getStringExtra("userid");

        // delete the vpn log.
        File file = new File(getCacheDir(), "vpnlog.txt");
        if(file.exists())
            file.delete();
        
        // get package and server name.
        m_remote.getUserService(m_userid, m_password);
        m_waitdlg = ProgressDialog.show(this, "Loading Servers", "Waiting for server reply...", true, false);
        
        // make text view link valid.
        TextView view1 = (TextView)findViewById(R.id.textview_checkip);
        view1.setMovementMethod(LinkMovementMethod.getInstance());
        TextView view2 = (TextView)findViewById(R.id.view_dashboard_status);
        view2.setMovementMethod(LinkMovementMethod.getInstance());
        TextView view3 = (TextView)findViewById(R.id.view_dashboard_setting);
        view3.setMovementMethod(LinkMovementMethod.getInstance());
        view3.setText(Html.fromHtml("<font color=#FFFFFF>Advanced Settings</font><b>  &gt;&gt;&gt;</b>"));
        
        // start internet checker timer, will not stop this.
        m_timer.schedule(new NetStateCheckTask(this), 3000, 3000);
        try {
            FileInputStream fi = new FileInputStream(getFilesDir() + "/setting.xml");
            Log.d( "d" , "get files directory : " + getFilesDir().toString());

            Properties xml = new Properties();
            xml.loadFromXML(fi);
            String first_login = xml.getProperty("FIRST_LOGIN");
            if(first_login == null) {
                xml.setProperty("FIRST_LOGIN", String.valueOf(new Date().getTime()));
                FileOutputStream fo = new FileOutputStream(getFilesDir() + "/setting.xml");
                xml.storeToXML(fo, null);
            }
        } catch(Exception e) {
            System.out.println(e);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
//        RateThisApp.onStart(this);
//        RateThisApp.showRateDialogIfNeeded(this);
    }
     
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    public void onPause() {
        Log.i("ibVPN", "onPause dashboard.");
        super.onPause();
    }
    
    @Override
    public void onResume() {
        Log.i("ibVPN", "onResume dashboard.");
        super.onResume();
    }
    
    @Override
    public void onDestroy() {
        Log.i("ibVPN", "onDestroy dashboard.");
        //TODO
//        if(m_openvpn != null) {
//            m_openvpn.disconnect();
//        }
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        if(m_status == Status.Connecting 
        || m_status == Status.Connected) {
            String message = "Do you want to keep VPN running in background?";
            handlerMessageBox(AlertDialogExitNotify, m_handler, "Exit", message, "Yes", "No", "Cancel");
            return;
        }
        super.onBackPressed();
    }
    
    class HandlerDialogClickListener implements OnClickListener {
        public static final int MessageType = 0x90000;
        private Handler m_handler;
        private int        m_id;
        public HandlerDialogClickListener(Handler handler, int id) {
            m_handler = handler;
            m_id = id;
        }
        public void onClick(DialogInterface dialog, int which) {
            Message msg = new Message();
            msg.what = MessageType;
            msg.arg1 = m_id;
            msg.arg2 = which;
            m_handler.sendMessage(msg);
            dialog.dismiss();
        }
    }

    public void handlerMessageBox(int id, Handler handler, String title, String message, String yes, String no, String cancel) {
        AlertDialog box = new AlertDialog.Builder(this).create();
        HandlerDialogClickListener listener = new HandlerDialogClickListener(handler, id);
        if(yes != null)
            box.setButton(AlertDialog.BUTTON_POSITIVE, yes, listener);
        if(no != null)
            box.setButton(AlertDialog.BUTTON_NEUTRAL, no, listener);
        if(cancel != null)
            box.setButton(AlertDialog.BUTTON_NEGATIVE, cancel, listener);
        box.setTitle(title);
        box.setMessage(message);
        box.show();
    }
    
    public void handleMessage(Message msg) {
        Bundle data = msg.getData();
        switch(msg.what) {        
        case RemoteAPI.MessageType: {
            String method = data.getString("method");
            String code = data.getString("code");
            String message = data.getString("message");

            if(method.equalsIgnoreCase("getUserServices")) {
                m_waitdlg.cancel();
                if(!(code.equalsIgnoreCase("0") || code.equalsIgnoreCase("4") || code.equalsIgnoreCase("5"))) {
                    Toast toast = Toast.makeText(this, "getUserServices failed, " + message, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                	toast.show();
                    return;
                }
                m_package = data.getString("packages");
                m_username = data.getString("username");
                m_password = data.getString("vpnpassword"); // update login password to vpnpassword.
                updatePackage(m_package.trim());
            }
            break; }

//            case OpenVPNService.MessageType: {
//                    String log = data.getString("log");
//                    if(log == null)
//                        return;
//                    // get openvpn state from the log.
//                    if(log.contains("AUTH_FAILED")) {
//                        Toast toast = Toast.makeText(this, "Username or password error.", Toast.LENGTH_SHORT);
//                        toast.setGravity(Gravity.CENTER, 0, 0);
//                        toast.show();
//                        setStatus(Status.Disconnected);
//                        return;
//                    }
//                    if(log.contains("SIGTERM") || log.contains("exiting")) {
//                        Toast toast = Toast.makeText(this, "Disconnected from server.", Toast.LENGTH_SHORT);
//                        toast.setGravity(Gravity.CENTER, 0, 0);
//                        toast.show();
//
//                        setStatus(Status.Disconnected);
//                        return;
//                    }
//                    if(log.contains("Initialization Sequence Completed")) {
//                        if(m_status == Status.Connecting)
//                            setStatus(Status.Connected);
//                        return;
//                    }
//                    break; }

        case HandlerDialogClickListener.MessageType: {
            switch(msg.arg1) {
            case AlertDialogExitNotify: {
                switch(msg.arg2) {
                case AlertDialog.BUTTON_POSITIVE: {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                    break; }
                case AlertDialog.BUTTON_NEGATIVE: {
                    break; }
                case AlertDialog.BUTTON_NEUTRAL: {
                    finish();
                    break; }
                }
                break; }
            }
            break; }
        
        case NetDisconnectedNotify: {
            Log.d("ibVPN", "Disconnected by internet lose.");
            Toast toast = Toast.makeText(this, "VPN connection was lost, please reconnect Wi-Fi/Mobile Data connection.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            
            Button button = (Button)findViewById(R.id.button_dashboard_connect);
//            if(button.getText().toString().equalsIgnoreCase(getString(R.string.text_cancel))) {
//                m_openvpn.cancel();
//                setStatus(Status.Disconnected);
//            } else
//            if(button.getText().toString().equalsIgnoreCase(getString(R.string.text_disconnect))) {
//                m_openvpn.disconnect();
//                setStatus(Status.Disconnected);
//            }
            break; }
        
        default: {
            Log.d("ibVPN", "Message From Unknown Thread. :)");
            break; }
        }
    }
    
    protected void onActivityResult(int code, int result, Intent data) {

        if(result == Activity.RESULT_OK) {
            String server = getCurrentServer();
            if(server.isEmpty()) {
                Toast toast = Toast.makeText(this, "Get server failed.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
            	toast.show();
                setStatus(Status.Disconnected);
                return;
            }
                
            // get session name.
            Spinner spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
            String session = spinServer.getSelectedItem().toString();

            // connecting to server.
            String port = getProperty("PORT");
            String proto = getProperty("PROTOCOL");
//TODO Seting login and user pass for opnvpn and start open vpn
//            m_openvpn.setLogin(m_username, m_password);
//            m_openvpn.setRemote(server, port == null ? "1195" : port);
//            m_openvpn.setSession(session);
//            m_openvpn.setProtocol(proto == null ? "udp" : proto);
//            m_openvpn.connect();    // start the service, but it is not connected.

            JSONObject props = new JSONObject();
            mixpanelAdd(props, "Selected Plan", getCurrentPackage());
            mixpanelAdd(props, "Location", getCurrentServerName());
            mixpanelAdd(props, "Other Available Plans", getValidPackages());
            //mixpanelTrack("Connect", props);

            Log.d("ibVPN", "props: " + props);
        } else 
        if (result == Activity.RESULT_CANCELED) {
            // end this process if user deny.
            finish();
        }
    }
    
    public boolean permissionConnect() {
        // delete the vpn log.
        File file = new File(getCacheDir(), "vpnlog.txt");
        if(file.exists())
            file.delete();
                
        Intent intent = VpnService.prepare(this);
        if(intent != null) {
            try {
                startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        } else {


            onActivityResult(0, Activity.RESULT_OK, null);

        }
        return true;
    }
    
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm == null)
            return false;

        NetworkInfo[] info = cm.getAllNetworkInfo();
        if(info == null)
            return false;
        
        for(int i = 0; i < info.length; i++) {   
        if (info[i].getState() == NetworkInfo.State.CONNECTED)
            return true;   
        }
        
        return false;
    }
    
    private String loadLogFromFile() {
        String log = "";
        try {
            RandomAccessFile fp = new RandomAccessFile(getCacheDir() + "/vpnlog.txt", "r");
//        	RandomAccessFile fp = new RandomAccessFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/vpnlog.txt", "r");
            while(true) {
                byte[] buf = new byte[0x1000];
                int size = fp.read(buf);
                if(size <= 0)
                    break;
                log += new String(buf);
            }
            fp.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return log;
    }
    
    private String formatTime(long t) {
    	long h = t / 3600000;
    	long m = t % 3600000 / 60000;
    	long s = t % 60000 / 1000;
    	return String.valueOf(h) + " hours "
    		+ String.valueOf(m) + " minutes "
    		+ String.valueOf(s) + " seconds";
    }

    public void onConnect(View v) {
        Log.d("ibVPN", "getCurrentServer:" + getCurrentServer());
        
        if(!isInternetAvailable(this)) {
            Toast toast = Toast.makeText(this, "Network is unreachable.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            return;
        }
        
        if(((Button)v).getText().toString().equalsIgnoreCase(getString(R.string.text_connect))) {
            setStatus(Status.Connecting);
//            m_openvpn = new OpenVPN(m_handler, this);    // new it here, so cancel will not crash.
            permissionConnect();
            m_date = System.currentTimeMillis();
        } else
        if(((Button)v).getText().toString().equalsIgnoreCase(getString(R.string.text_cancel))) {
//            m_openvpn.cancel();
            setStatus(Status.Disconnected);
            //mixpanelTrack("Cancel Connection", null);
        } else
        if(((Button)v).getText().toString().equalsIgnoreCase(getString(R.string.text_disconnect))) {
//            m_openvpn.disconnect();
            setStatus(Status.Disconnected); 

            String dura = formatTime(System.currentTimeMillis() - m_date);
            Log.d("ibVPN", "Session Duration: " + dura);
            
            JSONObject props = new JSONObject();
            mixpanelAdd(props, "Session Duration", dura);
            //mixpanelTrack("Disconnect", props);
            Log.d("ibVPN", "props: " + props);

            try {
                FileInputStream fi = new FileInputStream(getFilesDir() + "/setting.xml");
                Log.d( "d" , "get files directory : " + getFilesDir().toString());
                Properties xml = new Properties();
                xml.loadFromXML(fi);
                
                String first_login = xml.getProperty("FIRST_LOGIN");
                String first_conn = xml.getProperty("FIRST_CONNECTED");
                if(first_conn == null) {
                    first_conn = String.valueOf(new Date().getTime());
                    xml.setProperty("FIRST_CONNECTED", first_conn);
                    FileOutputStream fo = new FileOutputStream(getFilesDir() + "/setting.xml");
                    xml.storeToXML(fo, null);

                    JSONObject json = new JSONObject();
                    mixpanelAdd(json, "First Login Time", first_login);
                    mixpanelAdd(json, "First Connected Time", first_conn);
                    mixpanelAdd(json, "Accomodation Time", 
                        String.valueOf(Long.parseLong(first_conn) - Long.parseLong(first_login)));
                    //mixpanelTrack("Application 1st Time Connected", json);
                    Log.d("ibVPN", "props: " + json);
                }
            } catch(Exception e) {
                System.out.println(e);
            }

            JSONObject logs = new JSONObject();
            mixpanelAdd(logs, "Data", loadLogFromFile());
            //mixpanelTrack("Connection Log", logs);
        }
    }

    public void onStatus(View v) {
        Intent intent = new Intent(this, LogWindow.class);
        startActivity(intent);
    }
    
    public void onSetting(View v) {
        Intent intent = new Intent(this, Preferences.class);
        startActivity(intent);
        //mixpanelTrack("Advanced Settings", null);
    }
    
    public void setStatus(Status status) {
        Button btnConnect = (Button)findViewById(R.id.button_dashboard_connect);
        TextView textStatus = (TextView)findViewById(R.id.view_dashboard_status);
        Spinner spinPackage = (Spinner)findViewById(R.id.spinner_dashboard_package);
        Spinner spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
        
        if(btnConnect == null || textStatus == null 
        || spinPackage == null || spinServer == null) {
            Log.e("ibVPN", "at least one item do not exist.");
            return;
        }
            
        m_status = status;
        switch(m_status) {
        case Connecting: {
            spinPackage.setEnabled(false);
            spinServer.setEnabled(false);
            btnConnect.setEnabled(true);
            btnConnect.setText(R.string.text_cancel);
            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#fdbb2f>CONNECTING...</font><b>  &gt;&gt;&gt;</b>"));
            break; }
            
        case Connected: {
            spinPackage.setEnabled(false);
            spinServer.setEnabled(false);
            btnConnect.setEnabled(true);
            btnConnect.setText("Disconnect");
            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#00FF20>CONNECTED</font><b>  &gt;&gt;&gt;</b>"));
            break; }
            
        case Disconnecting: {
            spinPackage.setEnabled(false);
            spinServer.setEnabled(false);
            btnConnect.setEnabled(false);
            btnConnect.setText("Disconnect");
            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#FF0000>DISCONNECTING...</font><b>  &gt;&gt;&gt;</b>"));
            break; }
        
        case Disconnected: {
            spinPackage.setEnabled(true);
            spinServer.setEnabled(true);
            btnConnect.setEnabled(true);
            btnConnect.setText("Connect");
            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#FF0000>NOT CONNECTED</font><b>  &gt;&gt;&gt;</b>"));
            break; }
        
        default:
            Log.e("ibVPN", "no such status.");
            return;
        }
    }
    
    public String getCurrentServerName() {
    	Spinner spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
        return spinServer.getSelectedItem().toString();
    }
    
    public String getCurrentServer() {
        Spinner spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
        String[] packitem = m_package.split("\n");
        if(packitem == null || packitem.length < 2)
            return "";
        
        String[] tempServer = packitem[1].trim().split("\\\"");
        Object selected = spinServer.getSelectedItem();
        String server = selected.toString();

        int skipper = 3, stopper = -1;
        for(String item : tempServer) {
            if(stopper == skipper)
                return item;
            if(skipper++ % 4 != 0)
                continue;
            if(item.equals(server))
                stopper = skipper + 1;
        }
        return "";
    }
    
    public String getValidPackages() {
        Spinner spinPackage = (Spinner)findViewById(R.id.spinner_dashboard_package);
        String packs = "";
        for(int i = 0; i < spinPackage.getAdapter().getCount(); i++)
            packs += spinPackage.getItemAtPosition(i).toString().trim() + ",";
        return packs;
    }
    
    public String getCurrentPackage() {
        Spinner spinPackage = (Spinner)findViewById(R.id.spinner_dashboard_package);
        return spinPackage.getSelectedItem().toString().trim();
    }
    
    void updatePackage(String data) {
        if(data.length() <= 0) {
            // show order page
            //TODO check new order
//            Intent intent = new Intent(this, ActivityNewOrder.class);
//            startActivity(intent);
            // now we must close dashboard, and only show the order page.
            finish();
            return;     // end, this page is useless now.
        }
        
        Spinner spinPackage, spinServer;
        spinPackage = (Spinner)findViewById(R.id.spinner_dashboard_package);
        spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
        
        String[] raw = data.split("\n");
        if(raw.length < 2)
            return; // not enough array items.
        String[] tempPackage = raw[0].trim().split("\\\"");
        String[] tempServer = raw[1].trim().split("\\\"");
        ArrayList<String> aPackage = new ArrayList<String>();
        ArrayList<String> aServer = new ArrayList<String>();
        int skipper;
        
        skipper = 1;
        for(String item : tempPackage) {
            if(skipper++ % 4 != 0)
                continue;
            aPackage.add(item);
        }
        skipper = 3;
        for(String item : tempServer) {
            if(skipper++ % 4 != 0)
                continue;
            aServer.add(item);
        }
        
        String[] arrayPackage = new String[aPackage.size()];
        arrayPackage = aPackage.toArray(arrayPackage);
        String[] arrayServer = new String[aServer.size()];
        arrayServer = aServer.toArray(arrayServer);

        ArrayAdapter<String> adapterPackage = new ArrayAdapter<String>(this, R.layout.spinner_text, arrayPackage);
        ArrayAdapter<String> adapterServer = new ArrayAdapter<String>(this, R.layout.spinner_text, arrayServer);

        class PackageSelectedListener implements OnItemSelectedListener {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Log.d("ibVPN", "Package Selected:" + String.valueOf(arg2));
            }  
            public void onNothingSelected(AdapterView<?> arg0) {
            }  
        }
        adapterPackage.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);  
        spinPackage.setAdapter(adapterPackage);  
        spinPackage.setOnItemSelectedListener(new PackageSelectedListener());
        
        class ServerSelectedListener implements OnItemSelectedListener {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Log.d("ibVPN", "Server Selected:" + String.valueOf(arg2));
            }  
            public void onNothingSelected(AdapterView<?> arg0) {
            }  
        }
        adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);  
        spinServer.setAdapter(adapterServer);  
        spinServer.setOnItemSelectedListener(new ServerSelectedListener());  
    }
    
    public String getProperty(String key) {
        Properties prop = new Properties();
        String value = null;
        try {
            FileInputStream fi = new FileInputStream(getFilesDir() + "/setting.xml");
            prop.loadFromXML(fi);
            
            value = prop.getProperty(key);
        } catch(Exception e) {
            System.out.println(e);
        }
        return value;
    }
}
