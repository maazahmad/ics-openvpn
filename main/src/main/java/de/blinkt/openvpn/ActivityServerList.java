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
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4n.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
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
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter;

//import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class ActivityServerList extends BaseActivity  {
    public static final int AlertDialogExitNotify = 0x90001;
    public static final int NetDisconnectedNotify = 0x90002;

    private static final int START_VPN_CONFIG = 92;
    private static final int SELECT_PROFILE = 43;
    private static final int IMPORT_PROFILE = 231;
    private static final int FILE_PICKER_RESULT_KITKAT = 392;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerAdapter mAdapter;

    private ArrayList<String> mServerList;



    private static ExtendHandler     m_handler;            // static handler to deal message.
    private RemoteAPI                m_remote;

    private Status                   m_status;            // status of current connection.
    private String m_username;
    private String m_password;
    private String m_userid;
    private ProgressDialog m_waitdlg;
    private String m_package;
    private Timer m_timer;
    private String m_data;
    private long                     m_date;





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


    private boolean mCmfixed = false;



    private String m_inlineConfig;
    private String mLastStatusMessage;


    enum Status {
        Connecting,
        Connected,
        Disconnecting,
        Disconnected,
    }

    private static class ExtendHandler extends Handler {
        private ActivityServerList m_activity;

        public ExtendHandler(ActivityServerList activity) {
            setContext(activity);
        }

        public void setContext(ActivityServerList activity) {
            m_activity = activity;
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
        Log.i("ibVPN", "onCreate serverList.");
        super.onCreate(savedInstanceState);
        setTheme(R.style.blinkt_lolTheme);
        setContentView(R.layout.servers);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mAdapter = new RecyclerAdapter(mServerList,this);

        Log.d("Recycle Adapter",  "Count"+mAdapter.getItemCount()+"");

        mRecyclerView.setAdapter(mAdapter);

        m_manager=ProfileManager.getInstance(this);
        // set theme by code, this will improve the speed.
       setTheme(R.style.blinkt_lolTheme);
//        mPager = (ViewPager) findViewById(R.id.pager);

        // new the handler here, so it will not leak.
        if(m_handler == null)
            m_handler = new ExtendHandler(this);
        else
            m_handler.setContext(this);
        m_remote = new RemoteAPI(this, m_handler);
        m_timer = new Timer();
        
        setStatus(Status.Disconnected);
        


    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Start", ActivityDashboard.myServer.toString());
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.i("ibVPN", "onPause ServerList.");
        super.onPause();
    }
    
    @Override
    public void onResume() {
        Log.i("ibVPN", "onResume ServerList.");
        super.onResume();
    }
    
    @Override
    public void onDestroy() {
        Log.i("ibVPN", "onDestroy ServerList.");
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        if(m_status == Status.Connecting 
        || m_status == Status.Connected) {
            return;
        }
        super.onBackPressed();
    }
    



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode != Activity.RESULT_OK)
            return;

        if (requestCode == START_VPN_CONFIG) {
            String configuredVPN = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);


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



}