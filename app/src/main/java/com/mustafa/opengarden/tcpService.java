package com.mustafa.opengarden;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by musta on 1/25/2017.
 */

public class tcpService extends Service

{
    final static String MY_ACTION = "MY_ACTION";
    public static final String SERVER_IP = "54.235.39.131"; //server IP address
    public static final int SERVER_PORT = 1234;

//    public static final String SERVER_IP = "64.137.196.232"; //server IP address
//    public static final int SERVER_PORT = 1155;

    MyThread myThread;
    // message to send to the server
    private String sendMsgToServer;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter MsgOut;
    // used to read messages from the server
    private BufferedReader MsgIn;

    private final IBinder myBinder = new MyLocalBinder();

    public class MyLocalBinder extends Binder {
        tcpService getService() {
            return tcpService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {

        return myBinder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this, "Service Started " , Toast.LENGTH_LONG).show();
        startTcp();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }



    public void startTcp(){
        if(myThread == null) {
            myThread = new MyThread();
            myThread.start();
        }

    }

    public boolean checkconn(){

        return mRun;
    }

    public void stopClient() {

        mRun = false;

        if (MsgOut != null) {
            MsgOut.flush();
            MsgOut.close();
        }

        MsgIn = null;
        MsgOut = null;
        sendMsgToServer = null;

    }

    public void sendMessage(String message,Long timenow)  {
        if (MsgOut != null && !MsgOut.checkError()) {
            JSONObject newonbj= new JSONObject();
            try {
                newonbj.put("msg",message);
                newonbj.put("client_time",timenow);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MsgOut.println(newonbj);
            MsgOut.flush();
        }
    }

    public void getHistory(long since){
        if (MsgOut != null && !MsgOut.checkError()) {
            JSONObject newonbj= new JSONObject();
            try {
                newonbj.put("command","history");
                newonbj.put("client_time",System.currentTimeMillis());
                newonbj.put("since",since);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MsgOut.println(newonbj);
            MsgOut.flush();
        }


    }
//running tcp thread here
    public class MyThread extends Thread {
        Intent intent;

        @Override
        public void run() {
            mRun = true;

            try {

                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                

                Log.e("TCP Client", "Connecting");


                Socket socket = new Socket(serverAddr, SERVER_PORT);


                try {



                    //sends the message
                    MsgOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                    //receives the message
                    MsgIn = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));

                    //broadcasting message to nofity connected
                    intent = new Intent();
                    intent.setAction(MY_ACTION);
                    intent.putExtra("Con", "start");

                    sendBroadcast(intent);





                    //in this while the client listens for the messages sent by the server
                    while (mRun) {

                        Log.d("im","1");
                        sendMsgToServer = MsgIn.readLine();

                        if (sendMsgToServer != null ) {

                            Log.d("im","2");

                            //notifiy the message
                            intent = new Intent();
                            intent.setAction(MY_ACTION);
                            intent.putExtra("DATAPASSED", sendMsgToServer);

                            sendBroadcast(intent);
                            Log.e("RESPONSE FROM SERVER", sendMsgToServer );


                        }else{
                            Log.d("im","3");
                            mRun=false;
                        }

                        sendMsgToServer=null;

                    }



                } catch (Exception e) {

                    Log.e("TCP", "S: Error", e);

                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    socket.close();
                    stopSelf();
                    mRun=false;
                }

            } catch (Exception e) {

                Log.e("TCP", "C: Error", e);


            }
            Log.d("im","4");

            intent = new Intent();
            intent.setAction(MY_ACTION);
            intent.putExtra("discon", "stop");
            sendBroadcast(intent);

        }



    }
}
