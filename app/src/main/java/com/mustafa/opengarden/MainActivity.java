package com.mustafa.opengarden;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button b_send;
    EditText et_message;
    TextView tv_response;
    MyReceiver myReceiver;
    tcpService myService;
    Intent ser;
    SharedPreferences offlinesupp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start and bind service
        ser = new Intent(this, tcpService.class);
        startService(ser);
        bindService(ser, mServerConn, Context.BIND_AUTO_CREATE);


        b_send = (Button) findViewById(R.id.b_send);

        et_message = (EditText) findViewById(R.id.et_message);
        tv_response = (TextView) findViewById(R.id.tv_response);

        tv_response.setMovementMethod(new ScrollingMovementMethod());

        b_send.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Log.d("test", "<><> Button Clicked with message: " + et_message.getText().toString());
                String val=et_message.getText().toString();
                if( !val.isEmpty()) {
                    long timenow=System.currentTimeMillis();
                        if(myService.checkconn()){

                                myService.sendMessage(val,timenow);
                                addOfflineMsg(val,timenow);

                        }else{
                                addOfflineMsg(val,timenow);
                        };
                }

            }
        });



    }

    protected void onStart(){
        super.onStart();
        bindService(ser, mServerConn, Context.BIND_AUTO_CREATE);
        ///custom broadcst reciever
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(tcpService.MY_ACTION);//getting service by name
        registerReceiver(myReceiver, intentFilter);//here imclubing service with broadcast
    }

    protected void onStop(){
        super.onStop();
        unregisterReceiver(myReceiver);


    }

    protected void onDestroy(){
        super.onDestroy();
        stopService(ser);
        unbindService(mServerConn);

    }

    //binding service
    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            tcpService.MyLocalBinder BBinder = (tcpService.MyLocalBinder) binder;
            myService = BBinder.getService();

            Log.d("service", "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("service", "onServiceDisconnected");
        }
    };

    //adding messsage in sharepreference
    public void addOfflineMsg(String msg,long timenow){

        offlinesupp=getSharedPreferences("offlineSupport", 0);

        SharedPreferences.Editor edit=offlinesupp.edit();
        edit.putString(Long.toString(timenow),msg);
        edit.apply();
        Log.d("AddNewRecord", "getAll: " + offlinesupp.getAll());
    }

    public void checkOffline(){
        Log.d("AddNewRecord","offline");
        offlinesupp=getSharedPreferences("offlineSupport", 0);


        offlinesupp=getSharedPreferences("offlineSupport", 0);
        long val=offlinesupp.getLong("OfflineTime",0);
        SharedPreferences.Editor edit = offlinesupp.edit();
        if(val>0) {
            myService.getHistory(val);
            edit.putLong("OfflineTime", 0);
            edit.apply();
        }

        Map<String,?> keys = offlinesupp.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            Log.d("map values",entry.getKey() + ": " +
                    entry.getValue().toString());
            if (myService.checkconn() && !entry.getKey().equals("OfflineTime")) {
                myService.sendMessage(entry.getValue().toString(),Long.parseLong(entry.getKey()));
                edit.remove(entry.getKey());
                edit.apply();
            }
        }


    }

    //created custom boradcast to recevie events from services
    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            if(arg1.hasExtra("Con")){
                checkOffline();

            }else if(arg1.hasExtra("discon")){
                offlinesupp=getSharedPreferences("offlineSupport", 0);
                SharedPreferences.Editor edit=offlinesupp.edit();
                edit.putLong("OfflineTime",System.currentTimeMillis());
                edit.apply();
            }
            else {

                String datapassed = arg1.getStringExtra("DATAPASSED");
                MessageHandler(datapassed);
                Log.d("ddd", " " + datapassed);
            }


        }
    }
    //check if wifi or network changes happen for internet connection
    public class WifiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            //should check null because in airplane mode it will be null
            if (netInfo != null && netInfo.isConnected()){

                myService.startTcp();

            };
        }
    }

    public void MessageHandler(String values ){

        String msg="",error="",command="";
        long client_time=0,since=0;
        try {
            JSONObject jsonObj = new JSONObject(values);
            if (jsonObj.has("msg")) {
                msg = jsonObj.getString("msg");
            }
            if (jsonObj.has("error")){
                error = jsonObj.getString("error");

            }
            if (jsonObj.has("client_time")){
                client_time = jsonObj.getLong("client_time");

            }
            if (jsonObj.has("since")){
                since = jsonObj.getLong("since");

            }
            if (jsonObj.has("command")){
                command = jsonObj.getString("command");

            }
            SharedPreferences.Editor edit=offlinesupp.edit();
            edit.remove(Long.toString(client_time));
            edit.apply();

            tv_response.append(msg+"\n");


            Log.d("result",msg+" "+client_time);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
