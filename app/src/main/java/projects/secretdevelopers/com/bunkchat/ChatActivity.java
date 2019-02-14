package projects.secretdevelopers.com.bunkchat;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {

    Context context;
    WifiManager wm;
    int PORT = 9809;
    ArrayList<ClientScanResult> Receiverlist;
    TextView incmessages;
    TextInputEditText writemess;
    String username;
    boolean isServer;
    ArrayList<int[]> myMessages, otherMessages;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Log.d("address","comes on chatactivity");
        context = getApplicationContext();
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Receiverlist = new ArrayList<>();
        incmessages = (TextView) findViewById(R.id.Messages);
        incmessages.setMovementMethod(new ScrollingMovementMethod());
        writemess = (TextInputEditText) findViewById(R.id.messagebox);
        isServer = getIntent().getBooleanExtra("SERVER", false);
        username = getIntent().getStringExtra("username");
        otherMessages = new ArrayList<>();
        myMessages = new ArrayList<>();
        Log.d("address", ""+isServer);
        Log.d("address", ""+username);

    }

    @Override
    protected void onStart() {

        super.onStart();



        //Find all the available connections on the network
        //updateAllReceivers();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!isServer) {
                    Log.d("address", "updating receiver list");
                    sendhello();

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("address", "said hello to everyone");
                            incmessages.setBackgroundColor(Color.argb(10, 20, 100, 125));
                            incmessages.setText("Connected\n");
                            incmessages.setBackgroundColor(Color.WHITE);
                        }
                    });
                }
                else{
                    Log.d("address", "Server ready for listening");
                }
                Log.d("address", "Receiver list updated");
                receiveconnections();
            }
        }).start();
        Log.d("address","comes to receive connections");
        //Open a port and receive incoming messages


    }

    public void sendhello(){
        WifiManager tempWifiManager = wm;
        int intIP = tempWifiManager.getDhcpInfo().serverAddress;
        String mainIP =(intIP & 0xFF) + "." + ((intIP >> 8) & 0xFF) + "." + ((intIP >> 16) & 0xFF)
                + ".";
        String localIP = intToIP(wm.getConnectionInfo().getIpAddress());
        if(localIP.equals("0.0.0.0")) localIP = intToIP(intIP);
        ArrayList<ClientScanResult> tempReceiverList = new ArrayList<>();
        for(int i = 0; i<=255; i++){
            String currIP = mainIP + i;

            if(!localIP.equals(currIP)){
                try {
                    Log.d("address", "trying for: "+currIP);
                    if(InetAddress.getByName(currIP).isReachable(100) && isPortReachable(currIP, PORT)){
                        tempReceiverList.add(new ClientScanResult(currIP, true));

                        Log.d("address","found: "+currIP);
                    }
                } catch (Exception e) {
                    Log.d("address", "updateRecieverlist: "+e);
                }


            }
        }

        Receiverlist = tempReceiverList;
        SendMessAsyncTask clientAST = new SendMessAsyncTask();
        clientAST.execute("Welcome "+username);

    }


    public void receiveconnections(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Choosing a port for the conncetion
                    //Need to think of whether a static or dynamic port and how to manage them

                    ServerSocket servSocket = new ServerSocket(9809);

                    Socket client = null;
                    int connectionno = 0;
                    while (true) {
                        try {
                            Log.d("address", "Waiting on client");
                            client = servSocket.accept();

                            Log.d("address", "Doing a thread for client no " + (++connectionno)+"  ip: "+client.getInetAddress()+"currentip: "+intToIP(wm.getDhcpInfo().ipAddress));
                            addClient(client);
                            Log.d("address", "done addclient stuff");
                            ServerAsyncTask serverAsyncTask = new ServerAsyncTask();
                            serverAsyncTask.execute(client);
                        } catch (Exception e) {
                            Log.d("address", "inreciveconnections: "+e);
                        }
                    }
                } catch (IOException e) {
                    Log.d("address", "outreciveconnections: "+e);
                }
            }

        });
        t.start();
    }
    public void addClient(Socket... socket){
        Log.d("address", "inside addclientk");
        ArrayList<ClientScanResult> temprecievers = Receiverlist;
        int port = PORT;
        Socket sc = socket[0];
        Log.d("address", "checking if socket already exists");
        boolean found = false;
        for(ClientScanResult x : temprecievers) {
            if(x.getIpAddr().equals(sc.getInetAddress().toString().charAt(0)=='/'?sc.getInetAddress().toString().substring(1):sc.getInetAddress().toString())){
                found = true;
            }
        }
        if(!found) {
            Receiverlist.add(new ClientScanResult(sc.getInetAddress().toString().charAt(0)=='/'?sc.getInetAddress().toString().substring(1):sc.getInetAddress().toString(), true));
            Log.d("address", "Adding a new socket for " + (sc.getInetAddress().toString()));

        }

    }
    public void updateAllReceivers(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {

                    Log.d("address","trying to update list");
                    updateReceiverlist();
                    Log.d("address","receiver list updated");
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    public void sendMessageClick(View view) {

        String header = username+":";
        Date date = new Date();

        String strDateFormat = "hh:mm:ss a";

        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);

        String formattedDate= dateFormat.format(date);
        String footer = "\t\t"+ formattedDate;

        SendMessAsyncTask clientAST = new SendMessAsyncTask();
        clientAST.execute(header+writemess.getText().toString()+footer);

        String text = incmessages.getText().toString()+"\n"+header+writemess.getText().toString()+footer;

        Spannable spannable = new SpannableString(text);
        int start = incmessages.getText().toString().length(), finish = incmessages.getText().toString().length()+username.length() + 1;
        myMessages.add(new int[]{start, finish});
        for(int[] x: myMessages){
            spannable.setSpan(new ForegroundColorSpan(Color.RED), x[0], x[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }


        incmessages.setText(spannable);

        writemess.setText("");

    }

    class SendMessAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String mess = strings[0];
            ArrayList<ClientScanResult> temprecievers = Receiverlist;
            int port = PORT;
            Socket socket;
//            String result = null;
//            String header = username+":";
//            Date date = new Date();
//
//            String strDateFormat = "hh:mm:ss a";
//
//            DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
//
//            String formattedDate= dateFormat.format(date);
//            String footer = "\t\t"+ dateFormat.format(formattedDate);
//
//            mess = header + mess + footer;
            String lines[] = mess.split("\r?\n");
            Log.d("address", "sending message to all receivers");

            for(ClientScanResult x : temprecievers){
                try {

                    Log.d("address", "trying to send to ip: " + x.getIpAddr());

                    socket = new Socket(x.getIpAddr().charAt(0)=='/'? x.getIpAddr().substring(1):x.getIpAddr(), port);

                    Log.d("address", "creating socket for ip: " + x.getIpAddr());

                    PrintWriter out = new PrintWriter(socket.getOutputStream(),
                            true);

                    for(String l : lines) {
                        out.println(l);
                    }
                    socket.close();
                } catch (Exception e) {
                    Log.d("address", "error sending message: " + e);
//                    Receiverlist.remove(x);
//                    int index = 0;
//                    for(ClientScanResult y : temprecievers){
//                        if(y.getIpAddr() == x.getIpAddr()){
//                            Receiverlist.remove(index);
//                            break;
//                        }
//                        index++;
//                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("address","message sent");

        }
    }

    class AddClientAsyncTask extends AsyncTask<Socket, Void, String> {
        @Override
        protected String doInBackground(Socket... socket) {
            Log.d("address", "inside addclientasynctask");
            ArrayList<ClientScanResult> temprecievers = Receiverlist;
            int port = PORT;
            Socket sc = socket[0];
            Log.d("address", "checking if socket already exists");
            boolean found = false;
            for(ClientScanResult x : temprecievers) {
                if(x.getIpAddr().equals(sc.getInetAddress().toString())){
                    found = true;
                }
            }
            if(!found) {
                Receiverlist.add(new ClientScanResult(sc.getInetAddress().toString(), true));
                Log.d("address", "Adding a new socket for " + (sc.getInetAddress().toString()));

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("address","adding client finished");

        }
    }


    public void updateReceiverlist(){
        WifiManager tempWifiManager = wm;
        int intIP = tempWifiManager.getDhcpInfo().serverAddress;
        String mainIP =(intIP & 0xFF) + "." + ((intIP >> 8) & 0xFF) + "." + ((intIP >> 16) & 0xFF)
                + ".";
        String localIP = intToIP(wm.getConnectionInfo().getIpAddress());
        if(localIP.equals("0.0.0.0")) localIP = intToIP(intIP);
        ArrayList<ClientScanResult> tempReceiverList = new ArrayList<>();
        for(int i = 0; i<=255; i++){
            String currIP = mainIP + i;
            if(!localIP.equals(currIP)){
                try {
                    if(InetAddress.getByName(currIP).isReachable(80) && isPortReachable(currIP, PORT)){
                        tempReceiverList.add(new ClientScanResult(currIP, true));
                        Log.d("address","found: "+currIP);
                    }
                } catch (Exception e) {
                    Log.d("address", "updateRecieverlist: "+e);
                }


            }
        }

        Receiverlist = tempReceiverList;
        SendMessAsyncTask clientAST = new SendMessAsyncTask();
        clientAST.execute("Welcome "+username);

    }



    boolean isPortReachable(String inHost, int inPort) {
        Socket socket = null;
        boolean retVal = false;
        try {
            try {
                socket = new Socket(inHost, inPort);
            } catch (IOException e) {
                Log.d("address", ""+e);
            }
            retVal = true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch(Exception e) { }
            }
        }
        return retVal;
    }


    public String intToIP(int i) {
        return ((i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + ((i >> 24) & 0xFF));
    }

    class ServerAsyncTask extends AsyncTask<Socket, Void, String> {
        @Override
        protected String doInBackground(Socket... params) {
            Log.d("address", "inside serverasync");
            String main = "";
            Socket mySocket = params[0];
            try {
                Log.d("address", "gets input stream in background for: "+mySocket.getInetAddress());
                 InputStream is = mySocket.getInputStream();

                Log.d("address", "creates buffered reader in background"+mySocket.getInetAddress());
                 BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));

                String result = br.readLine();
                while (result != null && !result.equals("")){
                    Log.d("address","Reading Lines");
                    main += result;
                    result = br.readLine();
                }

                //mySocket.close();
            } catch (Exception e) {
                Log.d("address", "serverasync: "+e);
            }
            Log.d("address", "ends serverasync background");
            return main;
        }

        @Override
        protected void onPostExecute(String s) {

            Log.d("UI thread", "I am the PO UI thread");
            Log.d("address", "onpostexecute: "+s);
//            Looper.prepare();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d("address", "updating client connection");

                    String text = incmessages.getText().toString()+"\n"+s;

                    Spannable spannable = new SpannableString(text);

                    for(int[] x: myMessages){
                        spannable.setSpan(new ForegroundColorSpan(Color.RED), x[0], x[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    incmessages.setText(spannable);

                }
            });

        }
    }






}

class SendMessage implements Runnable {

    //check to see if it is a global message
    boolean sendglobalmess;
    boolean ip;

    SendMessage(boolean sendglobalmess){
        this.sendglobalmess = sendglobalmess;
    }

    @Override
    public void run() {

    }
}

class ReceiveMessage implements Runnable {

    ReceiveMessage(){

    }

    @Override
    public void run() {
        //listen for incoming messages

    }
}
