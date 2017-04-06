/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

//import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class ActivityLogin extends Activity {
    //TODO Change access of userid
    public String userid;

    private static class ExtendHandler extends Handler {

        private ActivityLogin m_activity;
        
        public ExtendHandler(ActivityLogin activity) {
            setContext(activity);
        }
        
        public void setContext(ActivityLogin activity) {
            m_activity = activity;
        }
        
        @Override
        public void handleMessage(Message msg) {
            m_activity.handleMessage(msg);
        }
    }
    private static ExtendHandler     m_handler;            // static handler to deal message.
    private ProgressDialog m_waitdlg;            // when we wait for server reply, we show it.
    private RemoteAPI                m_remote;             // remote api interface to communicate with server.
    
    String xor(String in, int key) {
        byte[] data = in.getBytes();
        for(int i = 0; i < in.length(); i++)
            data[i] ^= key;
        return new String(data);
    }
    
    String encrypt(String in) {
        if(in == null || in.isEmpty())
            return "";
        return xor(in, 0x4C);
    }
    
    String decrypt(String in) {
        if(in == null || in.isEmpty())
            return "";
        return xor(in, 0x4C);
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
    }

    protected void mixpanelSuperProperties(JSONObject props) {
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, MIXPANEL_TOKEN);
        mixpanel.registerSuperProperties(props);
    }*/
    
//    static String getEmail(Context context) {
//    	AccountManager accountManager = AccountManager.get(context);
//    	Account account = getAccount(accountManager);
//
//    	if (account == null) {
//    		return null;
//    	} else {
//    		return account.name;
//    	}
//    }

  /*  private static Account getAccount(AccountManager accountManager) {
    	Account[] accounts = accountManager.getAccountsByType("com.google");
    	Account account;
    	if (accounts.length > 0) {
    		account = accounts[0];      
    	} else {
    		account = null;
    	}
    	return account;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // set theme by code, this will improve the speed.
        setTheme(R.style.App_Theme);
        setContentView(R.layout.login);

        Log.i("i", "i");
        
        Log.d("d", "d");
        
        Log.v("v", "v");
        
        // new the handler here, so it will not leak.
        Log.e("error",">>>>>");
        if(m_handler == null)
            m_handler = new ExtendHandler(this);
        else
            m_handler.setContext(this);
        m_remote = new RemoteAPI(this, m_handler);
        
        // make text view link valid.
        TextView textView = (TextView)findViewById(R.id.textview_forgetpass);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        TextView textView1 = (TextView)findViewById(R.id.textview_register);
        textView1.setMovementMethod(LinkMovementMethod.getInstance());


        // load saved login from file.
        File file = new File(getCacheDir() + "/login");
        if(file.exists() && file.length() == 0)
            file.delete();
        if(!file.exists()) {
        	EditText editUsername = (EditText)findViewById(R.id.edit_login_email);
            editUsername.setText("ibtoolbar@ibvpn.com");
        }
        if(file.exists() && file.length() > 0) {
            try {
                EditText editUsername, editPassword;
                editUsername = (EditText)findViewById(R.id.edit_login_email);
                editPassword = (EditText)findViewById(R.id.edit_login_password);
                CheckBox box = (CheckBox)findViewById(R.id.checkbox_login_remember);
                
                RandomAccessFile fp = new RandomAccessFile(getCacheDir() + "/login", "rw");
                editUsername.setText(decrypt(fp.readLine()));
                editPassword.setText(decrypt(fp.readLine()));
                fp.close();
                
                box.setChecked(true);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        onRemember(findViewById(R.id.checkbox_login_remember));
        super.onDestroy();    // must call this or crash.
    }
    
    @Override
    protected void onStart() {
        super.onStart();
    }
     
    @Override
    protected void onStop() {
        super.onStop();
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
    
    public void onLogin(View v) {
        if(!isInternetAvailable(this)) {
            Toast toast = Toast.makeText(this, "Please activate your internet connection first.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            return;
        }
        
        EditText editUsername, editPassword;
        editUsername = (EditText)findViewById(R.id.edit_login_email);
        editPassword = (EditText)findViewById(R.id.edit_login_password);
        
        if(editUsername.getText().toString().isEmpty()) {
            Toast toast = Toast.makeText(this, "Email can not be empty.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            editUsername.requestFocus();
            return;
        }
        if(editPassword.getText().toString().isEmpty()) {
            Toast toast = Toast.makeText(this, "Password can not be empty.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            editPassword.requestFocus();
            return;
        }
        
        if(m_waitdlg != null && m_waitdlg.isShowing())
            m_waitdlg.cancel();
        
        m_waitdlg = ProgressDialog.show(this, "Login", "Waiting for server reply...", true, false);
        
        Log.i("parola", editPassword.getText().toString());
        
        m_remote.login(editUsername.getText().toString(), editPassword.getText().toString());
    }

    public void onCheck (View v) {
        EditText editPassword = (EditText)findViewById(R.id.edit_login_password);
       m_remote.getUserService(userid , editPassword.getText().toString() );
    }
    
    public void onRemember(View v) {
        CheckBox box = (CheckBox)v;
        if(box.isChecked()) {
            // save username and password to local.
            File file = new File(getCacheDir() + "/login");
            if(file.exists())
                file.delete();
            
            EditText editUsername, editPassword;
            editUsername = (EditText)findViewById(R.id.edit_login_email);
            editPassword = (EditText)findViewById(R.id.edit_login_password);
            
            try {
                RandomAccessFile fp = new RandomAccessFile(getCacheDir() + "/login", "rw");
                fp.write((encrypt(editUsername.getText().toString()) + "\n").getBytes());
                fp.write((encrypt(editPassword.getText().toString()) + "\n").getBytes());
                fp.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        } else {
            // delete saved username and password.
            File file = new File(getCacheDir() + "/login");
            if(file.exists())
                file.delete();
        }
    }
    
    public void onCancel(View v) {
        finish();
    }
    
    public void handleMessage(Message msg) {
        Bundle data = msg.getData();
        switch(msg.what) {        
        case RemoteAPI.MessageType: {
            String method = data.getString("method");
            String code = data.getString("code");
            String message = data.getString("message");
            
            if(method.equalsIgnoreCase("login")) {
                if(!code.equalsIgnoreCase("0")) {
                	if(code.equalsIgnoreCase("2") || code.equalsIgnoreCase("3")) {
                		Toast toast = Toast.makeText(this, "Register failed, " + message, Toast.LENGTH_LONG);
                		toast.setGravity(Gravity.CENTER, 0, 0);
                    	toast.show();
                		return;
                	}
                    Log.d("ibVPN", "first time login failed, trying again...");
                    // can not connect to server, use login2.
                    EditText editUsername, editPassword;
                    editUsername = (EditText)findViewById(R.id.edit_login_email);
                    editPassword = (EditText)findViewById(R.id.edit_login_password);
                    m_remote.login2(editUsername.getText().toString(), editPassword.getText().toString());
                    return;
                }
                
                // cancel the wait dialog only when login success.
                if(m_waitdlg != null && m_waitdlg.isShowing())
                    m_waitdlg.cancel();
            }
            
            if(method.equalsIgnoreCase("login2")) {
                if(m_waitdlg != null && m_waitdlg.isShowing())
                    m_waitdlg.cancel();
                if(!code.equalsIgnoreCase("0")) {
                    if(message.contains("UnknownHostException")) {
                    	Toast toast = Toast.makeText(this, "You have no Internet connection! Please check your Network settings.", Toast.LENGTH_LONG);
                    	toast.setGravity(Gravity.CENTER, 0, 0);
                    	toast.show();
                        return;
                    }
                    // default output java error message.
                    
                    Toast toast = Toast.makeText(this, "Login failed, " + message, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                	toast.show();
                    return;
                }
            }
            
            EditText editUsername;
            editUsername = (EditText)findViewById(R.id.edit_login_email);
            
            String username = editUsername.getText().toString();
            userid = data.getString("userid");
            Log.d("userid ser" , "handleMessage: " + userid);

            Log.d("ibVPN", "user id: " + userid + ", user name: " + username);
            /*MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, MIXPANEL_TOKEN);
            mixpanel.identify(username);
            mixpanel.getPeople().identify(userid);
            mixpanel.getPeople().set("Plan", data.getString("usertype"));
            */
            JSONObject props = new JSONObject();
            String usertype = data.getString("usertype").equalsIgnoreCase("0") ? "PAID" : "TRIAL";
            mixpanelAdd(props, "Device Type", android.os.Build.MODEL);
            mixpanelAdd(props, "User Type", usertype);
            //mixpanelSuperProperties(props);
            Log.d("ibVPN", "User Type: " + usertype);
            
            // login/login2 will come to this code.
            gotoDashboard(userid);
            break; }
        
        default: {
            Log.d("ibVPN", "Message From Unknown Thread. :)");
            break; }
        }
    }
    public void  register (View V) {
        Intent intent = new Intent(this, ActivityRegister.class);
        startActivity(intent);
    }
    
    public void gotoDashboard(String userid) {
        EditText editUsername, editPassword;
        editUsername = (EditText)findViewById(R.id.edit_login_email);
        editPassword = (EditText)findViewById(R.id.edit_login_password);


        Toast.makeText(getApplicationContext(),"Going to Dashboard",Toast.LENGTH_LONG);
        
        Intent intent = new Intent(this, ActivityDashboard.class);
        intent.putExtra("userid", userid);
        intent.putExtra("username", editUsername.getText().toString());
        intent.putExtra("password", editPassword.getText().toString());
        startActivity(intent);
    }
}
