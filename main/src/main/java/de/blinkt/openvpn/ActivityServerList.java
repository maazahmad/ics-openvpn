/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4n.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

import de.blinkt.openvpn.activities.BaseActivity;
import de.blinkt.openvpn.activities.FileSelect;
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

    private LinearLayoutManager mLinearLayoutManager;

    private LinearLayout linearSelectedServer, linearFavorites, linearAvailableServers;
    String[] countryList;
    private ArrayList<String> mServerList;
    public  SharedPreferences spGlobal;
    public  SharedPreferences.Editor edGlobal;
    EditText edt_search_key;




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

        linearSelectedServer = (LinearLayout)findViewById(R.id.linearSelectedServer);
        linearFavorites = (LinearLayout)findViewById(R.id.linearFavorites);
        linearAvailableServers = (LinearLayout)findViewById(R.id.linearAvailableServer);
        edt_search_key = (EditText)findViewById(R.id.edt_search_key);

        mLinearLayoutManager = new LinearLayoutManager(this);



        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("ServerList");

//        myToolbar.setNavigationIcon();
        myToolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Toast.makeText(ActivityServerList.this, "Back clicked!",     Toast.LENGTH_SHORT).show();
                Log.d("Clicked", "drawer open");
                finish();
            }
        });


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


        mServerList = new ArrayList<>();
        mServerList.addAll(ActivityDashboard.myServer);
        spGlobal = getSharedPreferences("user_info", 0);
        edGlobal = spGlobal.edit();
        makeServerList();

        edt_search_key.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                searchServer(edt_search_key.getText().toString());
            }
        });


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

    private void getCountryList(){
        countryList = getResources().getStringArray(R.array.countries_array);
        Collections.sort(Arrays.asList(countryList), new Comparator<String>(){
            public int compare(String obj1, String obj2) {
                if( obj1.length() > obj2.length() )
                    return -1;
                else if( obj1.length() < obj2.length())
                    return 1;
                else
                    return 0;
            }
        });
    }
    private void makeServerList(){


        getCountryList();

        makeSelectedServer();
        makeFavoriteServer();
        makeAvaiableServer();
    }
    private void refreshFavoriteAvailable(){
        makeFavoriteServer();
        makeAvaiableServer();
    }
    private void makeSelectedServer(){
        linearSelectedServer.removeAllViews();
        View viewSelectedServer = LayoutInflater.from(this).inflate(R.layout.item_selected_server, linearSelectedServer, false);
        ImageView imgViewFlag = (ImageView)viewSelectedServer.findViewById(R.id.imgViewFlag);
        TextView txtCountry = (TextView)viewSelectedServer.findViewById(R.id.txtViewCountryName);


        imgViewFlag.setVisibility(View.GONE);
        for(int i = 0; i < countryList.length; i++){
            String country = countryList[i];
            if( ActivityDashboard.lolstring.toLowerCase().contains(country.toLowerCase()) ){
                String resourceName = country.toLowerCase().replace(" ", "_");

                int checkExistence = getResources().getIdentifier(resourceName, "drawable", getPackageName());
                if ( checkExistence != 0 ) {  // the resouce exists...
                    imgViewFlag.setVisibility(View.VISIBLE);
                    imgViewFlag.setImageResource(getResources().getIdentifier("drawable/" + resourceName, null, getPackageName()));
                }
                break;
            }
        }
        txtCountry.setText(ActivityDashboard.lolstring);
        linearSelectedServer.addView(viewSelectedServer);
    }
    private void makeFavoriteServer(){
        linearFavorites.removeAllViews();

        for(int i = 0; i < mServerList.size(); i++){
            final String server = mServerList.get(i);

            if( spGlobal.getBoolean(server, false) ) {
                final View viewItem = LayoutInflater.from(this).inflate(R.layout.itemserver, linearSelectedServer, false);
                ImageView imgViewFlag = (ImageView) viewItem.findViewById(R.id.imgViewFlag);
                TextView txtCountry = (TextView) viewItem.findViewById(R.id.txtViewCountryName);
                ImageView imgFavorite = (ImageView) viewItem.findViewById(R.id.imgFavorite);

                imgFavorite.setImageResource(getResources().getIdentifier("drawable/icon_favorite", null, getPackageName()));
                imgViewFlag.setVisibility(View.GONE);
                for (int j = 0; j < countryList.length; j++) {
                    String country = countryList[j];
                    if (server.toLowerCase().contains(country.toLowerCase())) {
                        String resourceName = country.toLowerCase().replace(" ", "_");

                        int checkExistence = getResources().getIdentifier(resourceName, "drawable", getPackageName());
                        if (checkExistence != 0) {  // the resouce exists...
                            imgViewFlag.setVisibility(View.VISIBLE);
                            imgViewFlag.setImageResource(getResources().getIdentifier("drawable/" + resourceName, null, getPackageName()));
                        }
                        break;
                    }
                }
                txtCountry.setText(server);

                imgFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        edGlobal.putBoolean(server, false);
                        edGlobal.commit();
                        linearFavorites.removeView(viewItem);
                        makeAvaiableServer();
                    }
                });

                txtCountry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(VpnStatus.isVPNActive() && ActivityDashboard.m_status.equals(Status.Connected) ) {
                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which){
                                        case DialogInterface.BUTTON_POSITIVE:
                                            dialog.dismiss();
                                            ActivityDashboard.lolstring = server;
                                            finish();
                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            dialog.dismiss();
                                            break;
                                    }
                                }
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityServerList.this);
                            builder.setMessage("Currently connected to another VPN server. Are you sure you want to change the server?").setPositiveButton("Yes", dialogClickListener)
                                    .setNegativeButton("No", dialogClickListener).show();
                        }else{
                            ActivityDashboard.lolstring = server;
                            finish();
                        }
                    }
                });
                linearFavorites.addView(viewItem);
            }
        }
    }
    private void makeAvaiableServer(){
        linearAvailableServers.removeAllViews();

        for(int i = 0; i < mServerList.size(); i++){
            final String server = mServerList.get(i);


            View viewItem = LayoutInflater.from(this).inflate(R.layout.itemserver, linearSelectedServer, false);
            ImageView imgViewFlag = (ImageView) viewItem.findViewById(R.id.imgViewFlag);
            TextView txtCountry = (TextView) viewItem.findViewById(R.id.txtViewCountryName);
            final ImageView imgFavorite = (ImageView) viewItem.findViewById(R.id.imgFavorite);

            if( !spGlobal.getBoolean(server, false) ) {
                imgFavorite.setImageResource(getResources().getIdentifier("drawable/icon_unfavorite", null, getPackageName()));
                imgFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        edGlobal.putBoolean(server, true);
                        edGlobal.commit();
                        imgFavorite.setImageResource(getResources().getIdentifier("drawable/icon_favorite", null, getPackageName()));
                        makeFavoriteServer();
                    }
                });
            }else{
                imgFavorite.setImageResource(getResources().getIdentifier("drawable/icon_favorite", null, getPackageName()));
                imgFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        edGlobal.putBoolean(server, false);
                        edGlobal.commit();
                        imgFavorite.setImageResource(getResources().getIdentifier("drawable/icon_unfavorite", null, getPackageName()));
                        makeFavoriteServer();
                    }
                });
            }
            imgViewFlag.setVisibility(View.GONE);
            for (int j = 0; j < countryList.length; j++) {
                String country = countryList[j];
                if (server.toLowerCase().contains(country.toLowerCase())) {
                    String resourceName = country.toLowerCase().replace(" ", "_");

                    int checkExistence = getResources().getIdentifier(resourceName, "drawable", getPackageName());
                    if (checkExistence != 0) {  // the resouce exists...
                        imgViewFlag.setVisibility(View.VISIBLE);
                        imgViewFlag.setImageResource(getResources().getIdentifier("drawable/" + resourceName, null, getPackageName()));
                    }
                    break;
                }
            }
            txtCountry.setText(server);

            txtCountry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(VpnStatus.isVPNActive() && ActivityDashboard.m_status.equals(Status.Connected) ) {
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        dialog.dismiss();
                                        ActivityDashboard.lolstring = server;
                                        finish();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        dialog.dismiss();
                                        break;
                                }
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityServerList.this);
                        builder.setMessage("Currently connected to another VPN server. Are you sure you want to change the server?").setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("No", dialogClickListener).show();
                    }else{
                        ActivityDashboard.lolstring = server;
                        finish();
                    }
                }
            });
            linearAvailableServers.addView(viewItem);

        }
    }

    private void searchServer(String key){
        mServerList.clear();
        if( key.length() == 0 ){
            mServerList.addAll(ActivityDashboard.myServer);
        }else {
            for (int i = 0; i < ActivityDashboard.myServer.size(); i++) {
                if( ActivityDashboard.myServer.get(i).toLowerCase().contains(key.toLowerCase()) ){
                    mServerList.add(ActivityDashboard.myServer.get(i));
                }
            }
        }
        makeFavoriteServer();
        makeAvaiableServer();
    }


}