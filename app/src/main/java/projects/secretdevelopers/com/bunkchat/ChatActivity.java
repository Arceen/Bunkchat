package projects.secretdevelopers.com.bunkchat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    Context context;
    WifiManager wm;
    int PORT = 9809;
    ArrayList<ClientScanResult> Receiverlist;
    TextView incmessages;
    TextInputEditText writemess;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        context = getApplicationContext();
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Receiverlist = new ArrayList<>();
        incmessages = (TextView) findViewById(R.id.Messages);
        writemess = (TextInputEditText) findViewById(R.id.messagebox);
    }

    @Override
    protected void onStart() {

        super.onStart();

        //Find all the available connections on the network
        updateAllReceivers();

        //Open a port and receive incoming messages
        receiveconnections();

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
                            Log.d("address", "Waiting on clinet");
                            client = servSocket.accept();
                            Log.d("address", "Doing a thread for client no " + (++connectionno));
                            ServerAsyncTask serverAsyncTask = new ServerAsyncTask();
                            serverAsyncTask.execute(new Socket[]{client});
                        } catch (Exception e) {
                            Log.d("address", ""+e);
                        }
                    }
                } catch (IOException e) {
                    Log.d("address", ""+e);
                }
            }

        });
        t.start();
    }

    public void updateAllReceivers(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    updateReceiverlist();
                    SystemClock.sleep(5000);
                }
            }
        });
        t.start();
    }

    public void sendMessageClick(View view) {

        SendMessAsyncTask clientAST = new SendMessAsyncTask();
        clientAST.execute(String.valueOf(writemess.getText()));
        writemess.setText("");
        incmessages.setText(incmessages.getText()+"\n"+writemess.getText());

    }

    class SendMessAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String mess = strings[0];
            ArrayList<ClientScanResult> temprecievers = Receiverlist;
            int port = PORT;
            Socket socket;
            String result = null;
            String lines[] = mess.split("\r?\n");

            for(ClientScanResult x : temprecievers){
                try {
                    socket = new Socket(x.getIpAddr(), port);
                    Log.d("address", "able to send to server: " + socket.isConnected());
                    InputStream is = socket.getInputStream();
                    PrintWriter out = new PrintWriter(socket.getOutputStream(),
                            true);

                    for(String l : lines) {
                        out.println(l);
                    }
                    socket.close();
                } catch (Exception e) {
                    Log.d("address", "" + e);
                    SystemClock.sleep(1000);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);



        }
    }



    public void updateReceiverlist(){
        WifiManager tempWifiManager = wm;
        int intIP = tempWifiManager.getDhcpInfo().serverAddress;
        String mainIP =(intIP & 0xFF) + "." + ((intIP >> 8) & 0xFF) + "." + ((intIP >> 16) & 0xFF)
                + ".";
        String localIP = intToIP(wm.getConnectionInfo().getIpAddress());
        ArrayList<ClientScanResult> tempReceiverList = new ArrayList<>();
        for(int i = 0; i<=255; i++){
            String currIP = mainIP + i;
            if(localIP.equals(currIP)){
                try {
                    if(InetAddress.getByName(currIP).isReachable(1000) && isPortReachable(currIP, PORT)){
                        tempReceiverList.add(new ClientScanResult(currIP, true));
                    }
                } catch (IOException e) {
                    Log.d("address", ""+e);
                }


            }
        }

        Receiverlist = tempReceiverList;

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
            String result = null;
            Socket mySocket = params[0];
            try {

                InputStream is = mySocket.getInputStream();
                PrintWriter out = new PrintWriter(mySocket.getOutputStream(),
                        true);


                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));



                result = br.readLine();
                while (!result.equals("")){

                    result += br.readLine();
                }

                //mySocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            Log.d("UI thread", "I am the PO UI thread");
            Log.d("", ""+s);
            Looper.prepare();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d("address", "updating client connection");
                    incmessages.setText(incmessages.getText()+"\n"+s);
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
