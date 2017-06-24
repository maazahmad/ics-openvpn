/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// all output will be save in handler message.
// only code = "0", the message is processed success.
public class RemoteAPI {
    public static final int    MessageType = 0x10001;
    
    private static final String ApiKey = "52f4d13c50ed520227ad198f1ccbcd58";
    private static final String Url1 = "https://api.ibvpn.net/android/v4/";
    private static final String Url2 = "https://monitor.amplusnet.ro/api/android/redirect-v4.php";
    private static String Url = Url2;       // default use url2 for no login function.
    
    private Handler m_handler;
    private Context m_context;
    
    public RemoteAPI(Context c, Handler h) {
        // when function is done, run call back handler.
        m_handler = h;
        m_context = c;
    }
    
    public boolean login(String name, String pass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "login");
        rp.put("apikey", ApiKey);
        rp.put("username", name);
        rp.put("password", pass);
         Log.d("ibVPN logp", rp.toString());
        
        Url = Url1;

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", usertype = "", code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                        usertype = obj.getString("userType");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "login");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("usertype", usertype);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "login");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        
        return true;
    }
    
    public boolean login2(String name, String pass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "login");
        rp.put("apikey", ApiKey);
        rp.put("username", name);
        rp.put("password", pass);
        // Log.d("ibVPN", rp.toString());
        
        Url = Url2;

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", usertype = "", code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                        usertype = obj.getString("userType");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "login2");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("usertype", usertype);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "login");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        
        return true;
    }
    
    public boolean register(String email, String pass, String vpnpass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "register");
        rp.put("apikey", ApiKey);
        rp.put("email", email);
        rp.put("password", pass);
        rp.put("vpnpassword", vpnpass);
        rp.put("deviceid", getDeviceId());
        // Log.d("ibVPN", rp.toString());
        
        Url = Url1;

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "register");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "register");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    public boolean register2(String email, String pass, String vpnpass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "register");
        rp.put("apikey", ApiKey);
        rp.put("email", email);
        rp.put("password", pass);
        rp.put("vpnpassword", vpnpass);
        rp.put("deviceid", getDeviceId());
        // Log.d("ibVPN", rp.toString());
        
        Url = Url2;

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "register2");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "register");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    public boolean changePassword(String id, String oldpass, String newpass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "changePassword");
        rp.put("userId", id);
        rp.put("oldPassword", oldpass);
        rp.put("newPassword", newpass);
        // Log.d("ibVPN", rp.toString());

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "changePassword");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "changePassword");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    public boolean changeVPNPassword(String id, String oldpass, String newpass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "changeVPNPassword");
        rp.put("userId", id);
        rp.put("oldPassword", oldpass);
        rp.put("newPassword", newpass);
        // Log.d("ibVPN", rp.toString());

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "changeVPNPassword");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "changeVPNPassword");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    public boolean changeEmail(String id, String pass, String email) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "changeEmail");
        rp.put("userId", id);
        rp.put("password", pass);
        rp.put("newEmail", email);
        // Log.d("ibVPN", rp.toString());

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", mail = "", code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                        mail = obj.getString("email");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "changeEmail");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("email", mail);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "changeEmail");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    public boolean getUserInfo(String id, String pass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "getUserInfo");
        rp.put("userId", id);
        rp.put("password", pass);
        // Log.d("ibVPN", rp.toString());

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String userid = "", email = "", code = "", message = "", firstname = "", lastname = "", country = "", vpnpass = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0")) {
                        userid = obj.getString("userId");
                        email = obj.getString("email");
                        firstname = obj.getString("firstname");
                        lastname = obj.getString("lastname");
                        country = obj.getString("country");
                        vpnpass = obj.getString("VPNPassword");
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "getUserInfo");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("userid", userid);
                data.putString("email", email);
                data.putString("firstname", firstname);
                data.putString("lastname", lastname);
                data.putString("country", country);
                data.putString("vpnpassword", vpnpass);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "getUserInfo");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    public boolean getUserMessage(String id) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "getUserConfigurationProfile");
        rp.put("userId", id);
        // Log.d("ibVPN", rp.toString());

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String code = "", message = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    message = obj.getString("message");
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "getUserConfigurationProfile");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "getUserConfigurationProfile");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    public boolean getUserService(String id, String pass) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("methodName", "getUserServices");
        rp.put("userId", id);
        rp.put("password", pass);
        // Log.d("ibVPN", rp.toString());

        client.post(Url, rp, new AsyncHttpResponseHandler() {
            @Override
            public final void onSuccess(String response) {
                // handle your response here
                // Log.d("ibVPN", "onSuccess:" + response);
                
                String code = "", message = "", packages = "", username = "", vpnpass = "";
                try {
                    // example: {"errorcode":"0","userId":"39136","userType":"0"}
                    JSONObject obj = new JSONObject(response);
                    code = obj.getString("errorcode");
                    if(code.equalsIgnoreCase("0") || code.equalsIgnoreCase("4") || code.equalsIgnoreCase("5")) {
                        vpnpass = obj.getString("vpnpassword");
                        username = obj.getString("username");
                        // package format: 
                        // [name:package 1|name:package 2|name:package 3 ...]\n
                        // [name:server 1|name:server 2|name:server 3...]\n
                        JSONArray opkg = obj.getJSONArray("packages");
                        JSONArray osrv = obj.getJSONArray("servers");
                        packages += opkg.toString() + "\n";
                        packages += osrv.toString() + "\n";
                    } else {
                        message = obj.getString("message");
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                    // Log.d("ibVPN", "JSONException: " + e.toString());
                }
                
                // send login information to main thread.
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "getUserServices");    // method and code must be in the message.
                data.putString("code", code);
                data.putString("username", username);
                data.putString("vpnpassword", vpnpass);
                data.putString("packages", packages);
                data.putString("message", message);
                msg.setData(data);
                m_handler.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.what = MessageType;
                data.putString("method", "getUserServices");
                data.putString("code", "-999");    // error code must be -999, it is a network connection error.
                data.putString("message", e.toString());
                msg.setData(data);
                m_handler.sendMessage(msg);
            }               
        });
        return true;
    }
    
    private String getDeviceId() {
        String res = "";
        res = Settings.Secure.getString(m_context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if( res != null )   return res;

        TelephonyManager telephonyManager = (TelephonyManager)m_context.getSystemService(Context.TELEPHONY_SERVICE);
        res = telephonyManager.getDeviceId();
        if( res != null )   return res;
        return "";
    }
}
