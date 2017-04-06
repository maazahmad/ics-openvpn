/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import android.util.DisplayMetrics;
//import android.widget.TextView;

public class ActivityRegister extends Activity {
    

	
	
	private static class ExtendHandler extends Handler {
        private ActivityRegister m_activity;
        
        public ExtendHandler(ActivityRegister activity) {
            setContext(activity);
        }
        
        public void setContext(ActivityRegister activity) {
            m_activity = activity;
        }
        
        @Override
        public void handleMessage(Message msg) {
            m_activity.handleMessage(msg);
        }
    }
    private static ExtendHandler     m_handler;            // static handler to deal message.
    private RemoteAPI                m_remote;            // remote api interface to communicate with server.
    private ProgressDialog m_waitdlg;            // when we wait for server reply, we show it.
    

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        
        // set theme by code, this will improve the speed.
        setTheme(R.style.blinkt_lolTheme);
        setContentView(R.layout.register);
        
        // new the handler here, so it will not leak.
        if(m_handler == null)
            m_handler = new ExtendHandler(this);
        else
            m_handler.setContext(this);
        m_remote = new RemoteAPI(this, m_handler);
        
        // if the view is too short <= 800, we hide the two line.
        /*DisplayMetrics dm = new DisplayMetrics();   
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        if(dm.heightPixels <= 800) {
            TextView view1 = (TextView)findViewById(R.id.textView_Register2);
            TextView view2 = (TextView)findViewById(R.id.textView_Register3);
            view1.setVisibility(View.GONE);
            view2.setVisibility(View.GONE);
        }*/
        
        EditText editEmail = (EditText)findViewById(R.id.edit_register_email);
        editEmail.setText(getEmail(this));
        
        
        
        
    }
    @Override
    protected void onStart() {
        super.onStart();

    }
     
    @Override
    protected void onStop() {
        super.onStop();     

    }
    
    static String getEmail(Context context) {
	AccountManager accountManager = AccountManager.get(context);
	Account account = getAccount(accountManager);

	if (account == null) {
		return "-";
	} else {
		return account.name;
	}
}

private static Account getAccount(AccountManager accountManager) {
	Account[] accounts = accountManager.getAccountsByType("com.google");
	Account account;
	if (accounts.length > 0) {
		account = accounts[0];      
	} else {
		account = null;
	}
	return account;
}
       
    
    public static boolean isEmailValid(String email) {
        String reg = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$";
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(email);
        return m.matches();
    }
    
    public void onRegister(View v) {
        EditText editEmail = (EditText)findViewById(R.id.edit_register_email);
        EditText editPass = (EditText)findViewById(R.id.edit_register_account_password);
        EditText editPassR = (EditText)findViewById(R.id.edit_retype_account_password);
        Button btnRegist = (Button)findViewById(R.id.button_register);
        
        if(editEmail.getText().toString().isEmpty()) {
            Toast toast = Toast.makeText(this, "Email can not be empty.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            editEmail.requestFocus();
            return;
        }
        if(!isEmailValid(editEmail.getText().toString())) {
            Toast toast = Toast.makeText(this, "Email is not valid.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            editEmail.requestFocus();
            editEmail.setText("");
            return;
        }
        if(!editPass.getText().toString().equals(editPassR.getText().toString())) {
            Toast toast = Toast.makeText(this, "Account password is not matched.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        	toast.show();
            editPass.requestFocus();
            return;
        }
        
        if(m_waitdlg != null && m_waitdlg.isShowing())
        m_waitdlg = ProgressDialog.show(this, "Validating account ...", "Waiting for server reply...", true, false);
        
        // call json api to register this user.
        m_remote.register(editEmail.getText().toString(), editPass.getText().toString(), "");  
        btnRegist.setEnabled(false);
    }
    
 //
    
    public void handleMessage(Message msg) {
        Bundle data = msg.getData();
        Button btnRegist = (Button)findViewById(R.id.button_register);
        switch(msg.what) {        
        case RemoteAPI.MessageType: {
            if(m_waitdlg != null && m_waitdlg.isShowing())
                m_waitdlg.cancel();
            
            // if success, login to dashboard
            String code = data.getString("code");
            String message = data.getString("message");
            String method = data.getString("method");
            
            if(method.equalsIgnoreCase("register")) {
                if(!code.equalsIgnoreCase("0")) {
                	if(code.equalsIgnoreCase("2") || code.equalsIgnoreCase("3")) {
                		Toast toast = Toast.makeText(this, "Register failed, " + message, Toast.LENGTH_LONG);
                		toast.setGravity(Gravity.CENTER, 0, 0);
                    	toast.show();
                    	
                    	// register failed, email is not valid.
                        EditText editEmail = (EditText)findViewById(R.id.edit_register_email);
                        editEmail.requestFocus();
                        editEmail.setText("");
                        btnRegist.setEnabled(true);
                		return;
                	}
                    Log.d("ibVPN", "first time register failed, try again...");
                    Log.d("retun message" , message);

                    // can not connect to server, use login2.
                    EditText editEmail, editPass;
                    editEmail = (EditText)findViewById(R.id.edit_register_email);
                    editPass = (EditText)findViewById(R.id.edit_register_account_password);
                    m_remote.register2(editEmail.getText().toString(), editPass.getText().toString(), "");
                    btnRegist.setEnabled(true);
                    return;
                }
                
                // cancel the wait dialog only when login success.
                if(m_waitdlg != null && m_waitdlg.isShowing())
                    m_waitdlg.cancel();
            }
            
            if(method.equalsIgnoreCase("register2")) {
                if(m_waitdlg != null && m_waitdlg.isShowing())
                    m_waitdlg.cancel();
                if(!code.equalsIgnoreCase("0")) {
                    if(message.contains("UnknownHostException")) {
                        Toast toast = Toast.makeText(this, "You have no Internet connection! Please check your Network settings.", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                    	toast.show();
                    	btnRegist.setEnabled(true);
                        return;
                    }
                    
                    // register failed, email is not valid.
                    EditText editEmail = (EditText)findViewById(R.id.edit_register_email);
                    editEmail.requestFocus();
                    editEmail.setText("");
                    
                    // default output java error message.
                    Toast toast = Toast.makeText(this, "Register failed, " + message, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                	toast.show();
                	btnRegist.setEnabled(true);
                    return;
                }
            }
            
            btnRegist.setEnabled(true);
            gotoDashboard(data.getString("userid"));
            break; }
        
        default: {
            Log.d("ibVPN", "Message From Unknown Thread. :)");
            break; }
        }
    }
    
    public void gotoDashboard(String userid) {
        EditText editUsername, editPassword;
        editUsername = (EditText)findViewById(R.id.edit_register_email);
        editPassword = (EditText)findViewById(R.id.edit_register_account_password);
        
        Intent intent = new Intent(this, ActivityDashboard.class);
        intent.putExtra("userid", userid);
        intent.putExtra("username", editUsername.getText().toString());
        intent.putExtra("password", editPassword.getText().toString());
        Log.d("password",editPassword.getText().toString());
        startActivity(intent);
    }
}