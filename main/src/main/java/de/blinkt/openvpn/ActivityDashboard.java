/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.widget.DrawerLayout;
import android.support.v4n.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import de.blinkt.openvpn.activities.BaseActivity;
import de.blinkt.openvpn.activities.FileSelect;
import de.blinkt.openvpn.activities.LogWindow;
import de.blinkt.openvpn.activities.MainActivity;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter;


public class ActivityDashboard extends BaseActivity  implements VpnStatus.StateListener   {
    public static final int AlertDialogExitNotify = 0x90001;
    public static final int NetDisconnectedNotify = 0x90002;


    public final static int RESULT_VPN_DELETED = Activity.RESULT_FIRST_USER;
    public final static int RESULT_VPN_DUPLICATE = Activity.RESULT_FIRST_USER + 1;

    private static final int MENU_ADD_PROFILE = Menu.FIRST;

    private static final int START_VPN_CONFIG = 92;
    private static final int SELECT_PROFILE = 43;
    private static final int IMPORT_PROFILE = 231;
    private static final int FILE_PICKER_RESULT_KITKAT = 392;

    private static final int MENU_IMPORT_PROFILE = Menu.FIRST + 1;
    private static final int MENU_CHANGE_SORTING = Menu.FIRST + 2;
    private static final String PREF_SORT_BY_LRU = "sortProfilesByLRU";
//    private String mLastStatusMessage;
    private  boolean dicojugar =true;
    private IOpenVPNServiceInternal mService;




    private static ExtendHandler     m_handler;            // static handler to deal message.
    //    private OpenVPN                  m_openvpn;            // handle for openvpn.
    private RemoteAPI                m_remote;

    public static Status                   m_status;            // status of current connection.
    private String m_username;
    private String m_password;
    private String m_userid;
    private ProgressDialog m_waitdlg;
    private String m_package;
    private Timer m_timer;
    private long                     m_date;



    public static ArrayList<String> myServer;
    public static String lolstring;



    private Context m_context;
    public String extra;
    private String m_server;
    private  String m_port;
    private String m_extra;
    private String m_proto;
    private String m_session;

    private ScreenSlidePagerAdapter mPagerAdapter;

    public VpnProfile m_vpnprofile;
    private ProfileManager m_manager;
    private ViewPager mPager;


    private ArrayList<NavDrawerItem> mDrawerTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;


    private boolean mCmfixed = false;



    private String m_inlineConfig;
    private String mLastStatusMessage;

    Toolbar myToolbar;

    //added <code></code>


    @Override
    public void updateState(final String state, String logmessage, final int localizedResId, ConnectionStatus level) {
        ActivityDashboard.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mLastStatusMessage = VpnStatus.getLastCleanLogMessage(getParent());

//                mArrayadapter.notifyDataSetChanged();
//                setStatus(Status.Connected);
               Log.d("state", state.toString());
            }
        });
    }
    @Override
    public void setConnectedVPN(String uuid) {
        Log.d("state", "setConnectedVPB");


    }
    private ServiceConnection mConnection = new ServiceConnection() {



        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };


    // added code


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
    protected void onCreate(android.os.Bundle savedInstanceState) {
        Log.i("ibVPN", "onCreate dashboard.");
        super.onCreate(savedInstanceState);
        // set theme by code, this will improve the speed.
        setTheme(R.style.blinkt_lolTheme);
        setContentView(R.layout.mydash);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        myToolbar.setNavigationIcon(R.drawable.ic_action_menu);
        myToolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
              //  Toast.makeText(ActivityDashboard.this, "Back clicked!",     Toast.LENGTH_SHORT).show();
                Log.d("Clicked", "drawer open");
            }
        });


        // Set the adapter for the list view
         //   mDrawerList.setAdapter(new ArrayAdapter<String>(this,
         //           R.layout.drawer_list_item, mDrawerTitles));
        mDrawerTitles = new ArrayList<>();
        mDrawerTitles.add(new NavDrawerItem(R.drawable.ic_action_menu, "Setting"));
        mDrawerTitles.add(new NavDrawerItem(R.drawable.ic_action_menu, "Connection Status"));
        mDrawerTitles.add(new NavDrawerItem(R.drawable.ic_action_menu, "Purchase"));
        mDrawerTitles.add(new NavDrawerItem(R.drawable.ic_action_menu, "Log out"));


        mDrawerList.setAdapter(new NavDrawerAdapter (this, R.layout.drawer_list_item, mDrawerTitles));
//         Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


//        Log.d("toolbartitle",myToolbar.getTitle().toString());

        m_manager=ProfileManager.getInstance(this);

//        mPager = (ViewPager) findViewById(R.id.pager);

        // new the handler here, so it will not leak.
        if(m_handler == null)
            m_handler = new ExtendHandler(this);
        else
            m_handler.setContext(this);
        m_remote = new RemoteAPI(this, m_handler);
        m_timer = new Timer();
        
        setStatus(Status.Disconnected);
        
        // get data from intent.
        final Intent intent = getIntent();
        m_username = intent.getStringExtra("username");
        m_password = intent.getStringExtra("password");
        m_userid = intent.getStringExtra("userid");

        // delete the vpn log.
        File file = new File(getCacheDir(), "vpnlog.txt");
        if(file.exists())
            file.delete();
        //TODO fix On Register
        // get package and server name.
        Log.d("m_password" , m_password);
        m_remote.getUserService(m_userid, m_password);
        m_waitdlg = ProgressDialog.show(this, "Loading Servers", "Waiting for server reply...", true, false);
        
        TextView view2 = (TextView)findViewById(R.id.textview_serverlist);

        // start internet checker timer, will not stop this.

        view2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ActivityDashboard.this,ActivityServerList.class);
                intent.putExtra("userid", m_userid);
                intent.putExtra("username", m_userid);
                intent.putExtra("password", m_password);

                startActivity(i);

            }
        });
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
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);


        TextView locationServer = (TextView)findViewById(R.id.view_location);
        if (lolstring!= "" && myServer != null) {
            locationServer.setText(lolstring);
        }
        if(VpnStatus.isVPNActive() && dicojugar==false){
            setStatus(Status.Connected);
            dicojugar=true;
        }
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
            if(button.getText().toString().equalsIgnoreCase(getString(R.string.text_cancel))) {
//                m_openvpn.cancel();
                setStatus(Status.Disconnected);
            } else
            if(button.getText().toString().equalsIgnoreCase(getString(R.string.text_disconnect))) {
//                m_openvpn.disconnect();
                setStatus(Status.Disconnected);
            }
            break; }
        
        default: {
            Log.d("ibVPN", "Message From Unknown Thread. :)");
            break; }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        if (resultCode == RESULT_VPN_DELETED) {
//            if (mArrayadapter != null && mEditProfile != null)
//                mArrayadapter.remove(mEditProfile);
//        } else if (resultCode == RESULT_VPN_DUPLICATE && data != null) {
//            String profileUUID = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);
//            VpnProfile profile = ProfileManager.get(getActivity(), profileUUID);
//            if (profile != null)
//                onAddOrDuplicateProfile(profile);
//        }


        if (resultCode != Activity.RESULT_OK)
            return;

        if (requestCode == START_VPN_CONFIG) {
            String configuredVPN = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);

            VpnProfile profile = ProfileManager.get(this, configuredVPN);
            m_manager.saveProfile(this, profile);
            // Name could be modified, reset List adapter
//            setListAdapter();

        } else if (requestCode == SELECT_PROFILE) {
            String fileData = data.getStringExtra(FileSelect.RESULT_DATA);
            Uri uri = new Uri.Builder().path(fileData).scheme("file").build();

//            startConfigImport(uri);
        } else if (requestCode == IMPORT_PROFILE) {
            String profileUUID = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);
//            mArrayadapter.add(ProfileManager.get(getActivity(), profileUUID));
        } else if (requestCode == FILE_PICKER_RESULT_KITKAT) {
            if (data != null) {
                Uri uri = data.getData();
//                startConfigImport(uri);
            }
        }

    }


//    protected void onActivityResult(int code, int result, Intent data) {
//
//        if(result == Activity.RESULT_OK) {
//            String server = getCurrentServer();
//            if(server.isEmpty()) {
//                Toast toast = Toast.makeText(this, "Get server failed.", Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.CENTER, 0, 0);
//            	toast.show();
//                setStatus(Status.Disconnected);
//                return;
//            }
//
//            // get session name.
//            Spinner spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
//            String session = spinServer.getSelectedItem().toString();
//
//            // connecting to server.
//            String port = getProperty("PORT");
//            String proto = getProperty("PROTOCOL");
//
//            setLogin(m_username, m_password);
//            setRemote(server, port == null ? "1195" : port);
//            setSession(session);
//            setProtocol(proto == null ? "udp" : proto);
////            m_openvpn.connect();    // start the service, but it is not connected.
//
//            updateOvpnConfigFromAssets(m_server, m_port, m_proto, m_extra);
//
////            Log.d("TADA" ,  createVPNProfile().toString());
//            m_vpnprofile = createVPNProfile();
//            m_vpnprofile.mUsername = m_username;
//            m_vpnprofile.mPassword = m_password;
//            startVPN(m_vpnprofile);
//
//
//
//
//
//
////TODO Seting login and user pass for opnvpn and start open vpn
////            m_openvpn.setLogin(m_username, m_password);
////            m_openvpn.setRemote(server, port == null ? "1195" : port);
////            m_openvpn.setSession(session);
////            m_openvpn.setProtocol(proto == null ? "udp" : proto);
////            m_openvpn.connect();    // start the service, but it is not connected.
//
//            JSONObject props = new JSONObject();
//            mixpanelAdd(props, "Selected Plan", getCurrentPackage());
//            mixpanelAdd(props, "Location", getCurrentServerName());
//            mixpanelAdd(props, "Other Available Plans", getValidPackages());
//            //mixpanelTrack("Connect", props);
//
////            Log.d("ibVPN", "props: " + props);
//        } else
//        if (result == Activity.RESULT_CANCELED) {
//            // end this process if user deny.
//            finish();
//        }
//    }
    
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
            dicojugar=false;
//            m_openvpn = new OpenVPN(m_handler, this);    // new it here, so cancel will not crash.
            String server = getCurrentServer();
            // get session name.
            TextView locationServer = (TextView)findViewById(R.id.view_location);
            String session = locationServer.toString();
            Log.d("Seleceted item", session);

            // connecting to server.
            String port = getProperty("PORT");
            String proto = getProperty("PROTOCOL");

            setLogin(m_username, m_password);
            setRemote(server, port == null ? "1195" : port);
            setSession(session);
            setProtocol(proto == null ? "udp" : proto);

//            m_openvpn.connect();    // start the service, but it is not connected.


            updateOvpnConfigFromAssets(m_server, m_port, m_proto, m_extra);

            m_vpnprofile = createVPNProfile();
            m_vpnprofile.mUsername = m_username;
            m_vpnprofile.mPassword = m_password;
            m_manager.addProfile(m_vpnprofile);
            m_manager.saveProfile(this,m_vpnprofile);
            m_manager.saveProfileList(this);

//            gotoMainActivity();
            startVPN(m_vpnprofile);
//            permissionConnect();
            m_date = System.currentTimeMillis();
        } else
        if(((Button)v).getText().toString().equalsIgnoreCase(getString(R.string.text_cancel))) {
//            m_openvpn.cancel();
            setStatus(Status.Disconnected);
            //mixpanelTrack("Cancel Connection", null);
        } else
        if(((Button)v).getText().toString().equalsIgnoreCase(getString(R.string.text_disconnect))) {
//            .disconnect();

            if (VpnStatus.isVPNActive() ) {
                if (mService != null) {
                    try {
                        mService.stopVPN(false);
                    } catch (RemoteException e) {
                        VpnStatus.logException(e);
                    }
                }
            }
                setStatus(Status.Disconnected);
            dicojugar=true;

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
//        Spinner spinPackage = (Spinner)findViewById(R.id.spinner_dashboard_package);
//        Spinner spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
        TextView locationServer = (TextView)findViewById(R.id.view_location);
        ImageView ibVpnLogo = (ImageView)findViewById(R.id.ib_vpn_Con_ic);

        
        if(btnConnect == null || myToolbar.getTitle() == null || locationServer == null) {
            Log.e("ibVPN", "at least one item do not exist.");
            return;
        }
            
        m_status = status;
        switch(m_status) {
        case Connecting: {
//            spinPackage.setEnabled(false);
//            spinServer.setEnabled(false);
            btnConnect.setEnabled(true);
            btnConnect.setText(R.string.text_cancel);
            myToolbar.setTitle("Connecting");
//            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#fdbb2f>CONNECTING...</font><b>  &gt;&gt;&gt;</b>"));
            break; }
            
        case Connected: {
//            spinPackage.setEnabled(false);
//            spinServer.setEnabled(false);
            btnConnect.setEnabled(true);
            btnConnect.setText("Disconnect");
            myToolbar.setTitle("Connected");
            myToolbar.setBackgroundColor(Color.rgb(28,146,29));
            ibVpnLogo.setImageDrawable(getDrawable(R.drawable.icon_connected));

//            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#00FF20>CONNECTED</font><b>  &gt;&gt;&gt;</b>"));
            break; }
            
        case Disconnecting: {
//            spinPackage.setEnabled(false);
//            spinServer.setEnabled(false);
            btnConnect.setEnabled(false);
            btnConnect.setText("Disconnect");
            myToolbar.setTitle("Disconnecting");
            ibVpnLogo.setImageDrawable(getDrawable(R.drawable.icon_notconnected));

//            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#FF0000>DISCONNECTING...</font><b>  &gt;&gt;&gt;</b>"));
            break; }
        
        case Disconnected: {
//            spinPackage.setEnabled(true);
//            spinServer.setEnabled(true);
            btnConnect.setEnabled(true);
            btnConnect.setText("Connect");
            myToolbar.setTitle("NOT CONNECTED");
            myToolbar.setBackgroundColor(Color.rgb(255,0,64));
            ibVpnLogo.setImageDrawable(getDrawable(R.drawable.icon_notconnected));

//            textStatus.setText(Html.fromHtml("<font color=#FFFFFF>Status: </font><font color=#FF0000>NOT CONNECTED</font><b>  &gt;&gt;&gt;</b>"));
            break; }
        
        default:
            Log.e("ibVPN", "no such status.");
            return;
        }
    }
    
    public String getCurrentServerName() {
        TextView locationServer = (TextView)findViewById(R.id.view_location);
        return locationServer.toString();
    }
    
    public String getCurrentServer() {
        TextView locationServer = (TextView)findViewById(R.id.view_location);

        String[] packitem = m_package.split("\n");

        if(packitem == null || packitem.length < 2)
            return "";

        String[] tempServer = packitem[1].trim().split("\\\"");
        Object selected = locationServer.toString();
//        String server = selected.toString();
        String server = lolstring;
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
            Intent intent = new Intent(this, ActivityLogin.class);
            startActivity(intent);
            // now we must close dashboard, and only show the order page.
            finish();
            return;     // end, this page is useless now.
        }
        
        Spinner spinPackage, spinServer;
//        spinPackage = (Spinner)findViewById(R.id.spinner_dashboard_package);
//        spinServer = (Spinner)findViewById(R.id.spinner_dashboard_location);
        TextView locationServer = (TextView)findViewById(R.id.view_location);
        String[] raw = data.split("\n");
        if(raw.length < 2)
            return; // not enough array items.
        String[] tempPackage = raw[0].trim().split("\\\"");
        String[] tempServer = raw[1].trim().split("\\\"");
        ArrayList<String> aPackage = new ArrayList<String>();
        ArrayList<String> aServer = new ArrayList<String>();
        myServer = new ArrayList<>();
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
        myServer = aServer;

        if (myServer == null){
            Log.d("Null","Null");
        }else
            Log.d("Null", "Not Null");
        String[] arrayPackage = new String[aPackage.size()];
        arrayPackage = aPackage.toArray(arrayPackage);
        String[] arrayServer = new String[aServer.size()];
        arrayServer = aServer.toArray(arrayServer);

        ArrayAdapter<String> adapterPackage = new ArrayAdapter<String>(this, R.layout.spinner_text, arrayPackage);
        ArrayAdapter<String> adapterServer = new ArrayAdapter<String>(this, R.layout.spinner_text, arrayServer);

//        class PackageSelectedListener implements OnItemSelectedListener {
//            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
//                Log.d("ibVPN", "Package Selected:" + String.valueOf(arg2));
//            }
//            public void onNothingSelected(AdapterView<?> arg0) {
//            }
//        }
//        adapterPackage.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        Log.d("adapter package",adapterPackage.toString());

//        spinPackage.setAdapter(adapterPackage);
//        spinPackage.setOnItemSelectedListener(new PackageSelectedListener());
        
//        class ServerSelectedListener implements OnItemSelectedListener {
//            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
//                Log.d("ibVPN", "Server Selected:" + String.valueOf(arg2));
//            }
//            public void onNothingSelected(AdapterView<?> arg0) {
//            }
//        }
//        adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
       locationServer.setText(myServer.get(0).toString());
        lolstring = myServer.get(0).toString();
//        spinServer.setAdapter(adapterServer);
//        spinServer.setOnItemSelectedListener(new ServerSelectedListener());
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


    //FIXME orignal function needs to be modified

    public void setLogin(String name, String pass) {
        m_username = name;
        m_password = pass;
    }

    public void setRemote(String remote, String port) {
        m_server = remote;
        m_port = port;
    }

    public void setProtocol(String proto) {
        m_proto = proto;
        if (proto.equalsIgnoreCase("udp"))
            m_extra = "fragment 1300\n";
        else
            m_extra = "";
    }

    public void setSession(String session) {
        m_session = session;
    }

    private boolean updateOvpnConfigFromAssets(String ip, String port,
                                               String proto, String extra) {
        Log.d("ibVPN", "make ovpn config file now.\n");
        try {
            byte buf[] = new byte[0x8000]; // max 32KB
            InputStream in = getApplicationContext().getAssets().open("config.module");
            int size = in.read(buf);

            String ovpn = new String(buf, 0, size, "UTF-8");
            ovpn = ovpn.replace("#REMOTE_ADDRESS#", ip);
            ovpn = ovpn.replace("#REMOTE_PORT#", port);
            ovpn = ovpn.replace("#CA_PATH#", getApplicationContext().getFilesDir()
                    + "/ca.crt");
            ovpn = ovpn
                    .replace("#PROTOCOL#", proto.toLowerCase(Locale.ENGLISH));


            //// FIXME: PUT ORIGNAL CODE FOR EXTRA
           ovpn = ovpn.replace("#EXTRA_CONFIG#", extra);
            Log.d("total  ovpn string", ovpn);
            // add management config.
            String attach = "";
            attach += "management " + getApplicationContext().getCacheDir().toString()
                    + "/socket unix\n";
            attach += "management-query-passwords\n";
            attach += "management-client\n";
            attach += "management-hold\n";
            attach += "\n";

            File fp = new File(getApplicationContext().getFilesDir(), "config");
            fp.delete();

            FileOutputStream out = new FileOutputStream(fp);
            String total = attach + ovpn;
//            Log.d("total", total);
            m_inlineConfig = total;
            out.write(total.getBytes());
            out.close();


        } catch (Exception e) {
            System.out.println(e);

            return false;
        }

        return true;
    }
    private VpnProfile createVPNProfile() {
        try {
            ConfigParser cp = new ConfigParser();

//            VpnConfigGenerator vpn_configuration_generator = new VpnConfigGenerator(general_configuration, secrets, gateway);
//            String configuration = vpn_configuration_generator.generate();

            cp.parseConfig(new StringReader(m_inlineConfig));

            return cp.convertProfile();
        } catch (ConfigParser.ConfigParseError e) {
            Log.d("Exception ", "We didn't get a VpnProfile");
            // FIXME We didn't get a VpnProfile!  Error handling! and log level
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // FIXME We didn't get a VpnProfile!  Error handling! and log level
            e.printStackTrace();
            return null;
        }
    }
    private void startVPN(VpnProfile profile) {
        //Profile Manger saves profile

        Intent intent = new Intent(this,LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        Log.d("extra",LaunchVPN.EXTRA_KEY + "profile UUID    " +profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }

    public void gotoMainActivity() {
//        EditText editUsername, editPassword;
//        editUsername = (EditText)findViewById(R.id.edit_login_email);
//        editPassword = (EditText)findViewById(R.id.edit_login_password);


        Toast.makeText(getApplicationContext(),"Going to Main Activity",Toast.LENGTH_LONG);

        Intent intent = new Intent(this, MainActivity.class);
//
        startActivity(intent);
    }
    public class NavDrawerItem
    {
        public int icon;
        public String name;

        public NavDrawerItem(int icon, String name)
        {
            this.icon = icon;
            this.name = name;
        }
    }
    public class NavDrawerAdapter extends ArrayAdapter<NavDrawerItem>
    {
        private final Context context;
        private final int layoutResourceId;
        private ArrayList<NavDrawerItem> data = null;

        public NavDrawerAdapter(Context context, int layoutResourceId, ArrayList<NavDrawerItem> data)
        {
            super(context, layoutResourceId, data);
            this.context = context;
            this.layoutResourceId = layoutResourceId;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();

            View v = inflater.inflate(layoutResourceId, parent, false);

            ImageView imageView = (ImageView) v.findViewById(R.id.navDrawerImageView);
            TextView textView = (TextView) v.findViewById(R.id.navDrawerTextView);

            NavDrawerItem choice = data.get(position);

            imageView.setImageResource(choice.icon);
            textView.setText(choice.name);

            return v;
        }
    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
            switch (position) {
                case 0:
                    Intent intentS= new Intent(ActivityDashboard.this,ActivitySetting.class);
                    startActivity(intentS);
                    break;
                case 1:
                    Intent intentL= new Intent(ActivityDashboard.this,LogWindow.class);
                    startActivity(intentL);
                    break;
                case 2:
                    Intent intent= new Intent(ActivityDashboard.this,ActivityInAppPurchase.class);
                    startActivity(intent);
                    break;
                case 3:
                    finish();
                    break;
                default:
                    break;
            }
        }
    }
    private void selectItem(int position) {
        Toast.makeText(this.getApplicationContext(), "clicked", Toast.LENGTH_SHORT).show();

        // update the main content by replacing fragments
//        Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();
        Log.d("args", position+"");

//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
//        fragment.setArguments(args);

//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
//        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

}