package projects.secretdevelopers.com.bunkchat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    List<ScanResult> sclist = null;
    TextView tb;
    WifiManager wm;
    IntentFilter mIntentFilter;
    Intent intent;
    WifiP2pManager wp2p;
    EditText message;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver bReceiver;
    int settingsCanWrite;
    Button clientbutt, hostbutt, sendmessbutt;
    TextView tv;
    String TAG = "wifip2p";
    boolean abletosend = false;
    boolean abletorecieve = false;
    ArrayList<ClientScanResult> allclients = null;
    class fc implements FinishScanListener{

        String s = "";
        @Override
        public void onFinishScan(ArrayList<ClientScanResult> clients) {
            Log.d("address", "Client details changed");
            allclients = clients;
        }
    }


    fc finishScanListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        message = (EditText) findViewById(R.id.mess);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean canWrite = Settings.System.canWrite(getApplicationContext());
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_SETTINGS)
                    != PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Log.d("tag", "comes here " + (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_SETTINGS)
                        == PackageManager.PERMISSION_GRANTED) + " for build version " + Build.VERSION.SDK_INT);
            }
        }
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED))
        {
            Log.d("wifip2p", "Requesting permissions");

            //Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,},
                    123);


        }
        else
            Log.d("wifip2p", "Permissions already granted");
        clientbutt = (Button) findViewById(R.id.clientbutt);
        hostbutt = (Button) findViewById(R.id.hostbutt);
        sendmessbutt = (Button) findViewById(R.id.sendmess);
        clientbutt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startClientThread();
            }
        });

        hostbutt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startHostThread();
            }
        });

        sendmessbutt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(abletosend){
                    SendMessAsyncTask sendMessAsyncTask = new SendMessAsyncTask();
                    sendMessAsyncTask.execute(new String[] {message.getText().toString(),
                            intToIP(wm.getDhcpInfo().serverAddress), "9809" });
                }
                else if(abletorecieve){

                }
            }
        });

        tv = (TextView) findViewById(R.id.connectedbox);
        tv.setMovementMethod(new ScrollingMovementMethod());
        bReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wm.getScanResults();
            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void startHostThread() {
        Thread th = new Thread(new Runnable() {
            public void run() {
                Log.d("wifip2p", "enables hotspot");
                wm.setWifiEnabled(false);
                boolean b = setHotSpot("abc", "12345678");
                if(b){
                    abletorecieve = true;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("address", "I am the UI server thread");
                            tv.setText("Became a Server");
                        }
                    });

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            try {
                                ServerSocket socServer = new ServerSocket(9809);
                                Socket socClient = null;
                                int cno = 0;
                                while (true) {
                                    try {
                                        Log.d("address", "Waiting on clinet");
                                        socClient = socServer.accept();
                                        getClientList(finishScanListener);
                                        Log.d("address", "Doing a thread for client no " + (++cno));
                                        ServerAsyncTask serverAsyncTask = new ServerAsyncTask();
                                        serverAsyncTask.execute(new Socket[]{socClient});
                                    } catch (Exception e) {
                                        Log.d("address", ""+e);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();


                }

            }
        });
        th.start();
    }

    private void startClientThread() {
        Thread th = new Thread(new Runnable() {
            public void run() {
                Log.d("wifip2p", "Became a client");
                if(!wm.isWifiEnabled()){
                    wm.setWifiEnabled(true);
                }
                boolean b  = connectToHotspot("abc", "12345678");
                if(b){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("UI thread", "I am the UI thread");
                            tv.setText("Connected as a client");
                        }
                    });
//
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);


                    SystemClock.sleep(4000);


                    ClientAsyncTask clientAST = new ClientAsyncTask();
                    clientAST.execute(new String[] {intToIP(wm.getDhcpInfo().serverAddress), "9809", "Niloy"+" : "+"lovenot hate" });

                }
            }
        });
        th.start();
    }


    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
    }
    // this method help to create hotspot programmatically
    public boolean setHotSpot(String SSID, String passWord) {
        boolean apstatus = false;
        WifiConfiguration wifiCon = new WifiConfiguration();
        wifiCon.SSID = SSID;
        wifiCon.preSharedKey = passWord;
        wifiCon.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        wifiCon.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiCon.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiCon.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        try
        {
            Method setWifiApMethod = wm.getClass().getDeclaredMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            apstatus=(Boolean) setWifiApMethod.invoke(wm, wifiCon,true);
            Log.d("wifip2p", "comes here2");
        }
        catch (Exception e)
        {
            Log.d("error", "", e);
        }
        return apstatus;

    }


    // this method help to connect hotspot programmatically 
    public boolean connectToHotspot(String netSSID, String netPass) {
        WifiConfiguration wifiConf = new WifiConfiguration();
        WifiInfo wifiInfo = wm.getConnectionInfo();
        List<ScanResult> scanResultList = null;
        scanResultList = wm.getScanResults();
            //do something, permission was previously granted; or legacy device


        Log.d("wifip2p", "inside connect to hotspot");
        if (wm.isWifiEnabled()) {
            for (ScanResult result : scanResultList) {

                Log.d("wifip2p", ""+result.SSID);
                if (result.SSID.equals(netSSID)) {
                    Log.d("wifip2p", "finds network");
                   try{

                       int netID = -1;
                    String confSSID = "\"" + netSSID + "\"";
                    String confPassword = "\"" + netPass + "\"";

                    netID = getExistingNetworkId(confSSID, netID);
                    if(netID == -1) {
                        wifiConf.SSID = confSSID;
                        wifiConf.preSharedKey = confPassword;
                        wifiConf.hiddenSSID = true;
                        wifiConf.status = WifiConfiguration.Status.ENABLED;
                        wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                        wifiConf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                        netID = wm.addNetwork(wifiConf);

                        wm.saveConfiguration();
                    }
                    wm.disconnect();
                    wm.enableNetwork(netID, true);
                    wm.reconnect();
                    Log.d("error", "finds network");
                }catch(Exception e){
                       Log.d("wifip2p", ""+e);
                   }
                    return true;

                }
            }
        }
        return false;
    }
    private int getExistingNetworkId(String SSID, int netID) {
        List<WifiConfiguration> wifiConfigurationList = wm.getConfiguredNetworks();
        for (WifiConfiguration item : wifiConfigurationList){
        /*
          Find if the SSID is in the preconfigured list - if found get netID
         */
            if (item.SSID != null && item.SSID.equals(SSID)){

                Log.d(TAG, "Pre-configured running");
                netID = item.networkId;
                break;
            }
        }
        return netID;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        Log.d(TAG, "onRequestPermissionsResult");

        switch (requestCode)
        {
            case 123:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted
                    Log.d(TAG, "permission granted: " + permissions[0]);
                }
                else
                {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(TAG, "permission denied: " + permissions[0]);
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    class SendMessAsyncTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... strings) {
            String mess = strings[0];
            String socketstring = strings[1];
            int port = Integer.parseInt(strings[2]);

            String result = null;
            boolean b = true;
            Socket socket = null;
            while(b) {
                try {
                    socket = new Socket(socketstring, port);

                    b = false;
                } catch (Exception e) {
                    Log.d("address", "" + e);
                    SystemClock.sleep(1000);
                }
            }

            try {
                Log.d("address", "able to send to server: " + socket.isConnected());
                InputStream is = socket.getInputStream();
                PrintWriter out = new PrintWriter(socket.getOutputStream(),
                        true);
                out.println(mess);

                socket.close();
            }
            catch(Exception e){
                Log.d("address", "error");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {

        }
    }
    class ServerAsyncTask extends AsyncTask<Socket, Void, String> {
        @Override
        protected String doInBackground(Socket... params) {
            String result = null;
            Socket mySocket = params[0];
            try {

                InputStream is = mySocket.getInputStream();
                PrintWriter out = new PrintWriter(mySocket.getOutputStream(),
                        true);

                out.println("Welcome to Niloy's Server");

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));

                result = br.readLine();

                //mySocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            Looper.prepare();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d("UI thread", "I am the PO UI thread");
                    tv.setText(tv.getText()+s);
                }
            });
        }
    }

    class ClientAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String result = null;
            boolean b = true;
            Socket socket = null;
            while(b) {
                try {
                    socket = new Socket(params[0],
                    Integer.parseInt(params[1]));

                    b = false;
                } catch (Exception e) {
                    Log.d("address", "" + e);
                    SystemClock.sleep(1000);
                }
            }

            try {
                Looper.prepare();
                abletosend = true;
                Log.d("address", "" + socket.isConnected());

                Toast.makeText(getApplicationContext(),"Connected to server port",Toast.LENGTH_LONG);
                InputStream is = socket.getInputStream();

                PrintWriter out = new PrintWriter(socket.getOutputStream(),
                        true);

                out.println(params[2]);

//                BufferedReader br = new BufferedReader(
//                        new InputStreamReader(is));
//
//                result = br.readLine();

                Log.d("address", "buffer stuff ");
                socket.close();
            }
            catch(Exception e){
                Log.d("address", ""+e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d("address", "updating client connection");
                    tv.setText(tv.getText()+s);
                }
            });
        }
    }

    public String intToIP(int i) {
        return ((i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + ((i >> 24) & 0xFF));
    }


    public void getClientList(FinishScanListener finishListener) {
        getClientList(true, 300, finishListener);
    }

    /**
     * Gets a list of the clients connected to the Hotspot
     *
     * @param onlyReachables   {@code false} if the list should contain unreachable (probably disconnected) clients, {@code true} otherwise
     * @param reachableTimeout Reachable Timout in miliseconds
     * @param finishListener,  Interface called when the scan method finishes
     */
    public void getClientList(final boolean onlyReachables, final int reachableTimeout, final FinishScanListener finishListener) {
        Runnable runnable = new Runnable() {
            public void run() {

                BufferedReader br = null;
                final ArrayList<ClientScanResult> result = new ArrayList<ClientScanResult>();

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if ((splitted != null) && (splitted.length >= 4)) {
                            // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..")) {
                                boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(reachableTimeout);

                                if (!onlyReachables || isReachable) {
                                    result.add(new ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d("address", e.toString());
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.d("address", e.getMessage());
                    }
                }

                // Get a handler that can be used to post to the main thread
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

                Log.d("address", "error happens afer this");
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        for(ClientScanResult x: result)
                        Log.d("address", "Hardware address: " + x.getHWAddr() + "  IP Address" + x.getIpAddr());

                    }
                };
                mainHandler.post(myRunnable);
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }

//    public void getallconnected(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }).start();
//
//
//    }


}
