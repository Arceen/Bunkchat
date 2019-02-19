package projects.secretdevelopers.com.bunkchat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
    LinearLayout messagescroller;
    TextInputEditText writemess;
    String username;
    boolean isServer;
    ArrayList<int[]> myMessages, otherMessages;
    PowerManager pm;
    PowerManager.WakeLock wl;
    int userColor;
    boolean serverAck;
    boolean doneServ;
    ScrollView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Log.d("address", "comes on chatactivity");
        context = getApplicationContext();
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Receiverlist = new ArrayList<>();
//        incmessages = (TextView) findViewById(R.id.Messages);
//        incmessages.setMovementMethod(new ScrollingMovementMethod());
        messagescroller = (LinearLayout) findViewById(R.id.MessagesScroll);
        writemess = (TextInputEditText) findViewById(R.id.messagebox);
        isServer = getIntent().getBooleanExtra("SERVER", false);
        username = getIntent().getStringExtra("username");
        userColor = getIntent().getIntExtra("usercolor", 123);
        otherMessages = new ArrayList<>();
        myMessages = new ArrayList<>();
        serverAck = false;
        doneServ = true;
        Log.d("address", "" + userColor);
        Log.d("address", "" + isServer);
        Log.d("address", "" + username);
        sv = (ScrollView) findViewById(R.id.MessagesScroller);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Dontfallasleepwhilechatting");


    }

    @Override
    protected void onStart() {

        super.onStart();

        wl.acquire();

        sv.postDelayed(new Runnable() {
            @Override
            public void run() {
                sv.fullScroll(View.FOCUS_DOWN);
            }

        }, 700);

        //Find all the available connections on the network
        //updateAllReceivers();
        //update receivers and listen
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isServer) {
                    Log.d("address", "clients updating receiver list by asking the server.");
                    sendHelloServer();
                    Log.d("address", "done updating client list");
                } else {
                    Log.d("address", "This is the Server");
                }
                Log.d("address", "receiveconnections start");
                receiveconnections();

            }
        }).start();
//

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        wl.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        wl.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void sendHelloServer() {
        WifiManager tempWifiManager = wm;
        String servIP = intToIP(tempWifiManager.getDhcpInfo().serverAddress);
        Receiverlist.add(new ClientScanResult(servIP, true));

//        ArrayList<ClientScanResult> tempReceiverList = new ArrayList<>();
        //Request message
        try {
            Log.d("address", "sending hello to server: " + servIP);
            SendMessAsyncTaskobj clientAST = new SendMessAsyncTaskobj();

            Message newMessage = new Message(1);

            clientAST.execute(newMessage);

            Log.d("address", "sent request message to server");

        } catch (Exception e) {
            Log.d("address", "sendHelloServer request: " + e);
        }
        try {
            Log.d("address", "Waiting for server Response");
            Log.d("address", "created a server socket");
            ServerSocket servSocket = new ServerSocket(9809);
//            servSocket.setReuseAddress(true);
//            servSocket.bind(new InetSocketAddress("localhost", 9809));
            Socket client = null;
            while (true) {
                client = servSocket.accept();
                doneServ = false;
                ServerAsyncTaskobj serverAsyncTaskobj = new ServerAsyncTaskobj();
                serverAsyncTaskobj.execute(client);
                while (!doneServ) {
                    Thread.sleep(500);
                }
                Log.d("address", "done server service");
                if (serverAck) break;

            }
            servSocket.close();

            Log.d("address", "closed the server socket");

        } catch (Exception e) {
            Log.d("address", "sendHelloServer response:" + e);
        }
        ArrayList<ClientScanResult> tempreceivers = Receiverlist;
        for (ClientScanResult x : tempreceivers) {
            if (x.getIpAddr().equals(intToIP(wm.getConnectionInfo().getIpAddress()))) {
                Receiverlist.remove(x);
            }
        }

        SendMessAsyncTaskobj clientAST = new SendMessAsyncTaskobj();
        String text = "Hello " + username;
        Message newMessage = new Message(0, username, new Date(), userColor);
        newMessage.setTextMessage(text);
        clientAST.execute(newMessage);


    }


    public void receiveconnections() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Choosing a port for the conncetion
                    //Need to think of whether a static or dynamic port and how to manage them

                    ServerSocket servSocket = new ServerSocket(9809);
//                    servSocket.setReuseAddress(true);
//                    servSocket.bind(new InetSocketAddress("locathost", 9809));
                    Socket client = null;

                    int connectionno = 0;
                    while (true) {
                        try {

                            Log.d("address", "Waiting on client");
                            client = servSocket.accept();

                            Log.d("address", "Doing a thread for client no " + (++connectionno) + "  ip: " + client.getInetAddress() + "currentip: " + intToIP(wm.getDhcpInfo().ipAddress));
                            addClient(client);
                            Log.d("address", "done addclient stuff");
                            ServerAsyncTaskobj serverAsyncTaskobj = new ServerAsyncTaskobj();
                            serverAsyncTaskobj.execute(client);
//                            Log.d("address", "receiving message object");
//
//                            client = servSocket.accept();
//                            ServerAsyncTaskobj serverAsyncTaskobj = new ServerAsyncTaskobj();
//                            serverAsyncTaskobj.execute(client);


                        } catch (Exception e) {
                            Log.d("address", "inreciveconnections: " + e);
                        }
                    }
                } catch (IOException e) {
                    Log.d("address", "outreciveconnections: " + e);
                }
            }

        });
        t.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(Menu.NONE, 1, Menu.NONE, "Connected Clients");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                Toast.makeText(getApplicationContext(), "item 1 selected", Toast.LENGTH_LONG).show();
                return true;

            default:
                return false;
        }
    }


    public void addClient(Socket... socket) {
        Log.d("address", "inside addclientk");
        ArrayList<ClientScanResult> temprecievers = Receiverlist;
        int port = PORT;
        Socket sc = socket[0];
        Log.d("address", "checking if socket already exists");
        boolean found = false;
        for (ClientScanResult x : temprecievers) {
            if (x.getIpAddr().equals(sc.getInetAddress().toString().charAt(0) == '/' ? sc.getInetAddress().toString().substring(1) : sc.getInetAddress().toString())) {
                found = true;
            }
        }
        if (!found) {
            Receiverlist.add(new ClientScanResult(sc.getInetAddress().toString().charAt(0) == '/' ? sc.getInetAddress().toString().substring(1) : sc.getInetAddress().toString(), true));
            Log.d("address", "Adding a new socket for " + (sc.getInetAddress().toString()));

        }

    }

    public void updateAllReceivers() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    Log.d("address", "trying to update list");
                    updateReceiverlist();
                    Log.d("address", "receiver list updated");
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
        //Sending message using normal socket method
        String header = username + ":";
        Date date = new Date();

        String strDateFormat = "hh:mm:ss a";

        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);

        String formattedDate = dateFormat.format(date);
//        String footer = "\t\t"+ formattedDate;
////
////        SendMessAsyncTask clientAST = new SendMessAsyncTask();
////        clientAST.execute(header+writemess.getText().toString()+footer);
//
//        String text = incmessages.getText().toString()+"\n"+header+writemess.getText().toString()+footer;
//
//        Spannable spannable = new SpannableString(text);
//        int start = incmessage.getText().toString().length(), finish = incmessages.getText().toString().length()+username.length() + 1;
//        myMessages.add(new int[]{start, finish});
//        for(int[] x: myMessages){
//            spannable.setSpan(new ForegroundColorSpan(userColor), x[0], x[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        }
//
//
//        incmessages.setText(spannable);
        String messText = writemess.getText().toString();

        writemess.setText("");
        //Putting text on a TextView with a messageshape background
        //and pushing them into the vertical oriented LinearLayout
        //inside scrollview
        LinearLayout ll1 = new LinearLayout(getApplicationContext());

        ll1.setOrientation(LinearLayout.VERTICAL);
        ll1.setGravity(Gravity.END);
        ll1.setGravity(Gravity.RIGHT);
        TextView myMessage = new TextView(getApplicationContext());
        myMessage.setText(messText);
        TextView myMessageDet = new TextView(getApplicationContext());
        myMessageDet.setText(username + " at " + formattedDate);
        myMessageDet.setTextSize(13);
        myMessageDet.setTextColor(Color.BLACK);
        myMessageDet.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

//        myMessage.setBackgroundResource(R.drawable.messageshape);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(userColor);
        shape.setSize(200, 80);
        shape.setStroke((int) 0.5, getResources().getColor(R.color.mymessagestroke));
        shape.setCornerRadii(new float[]{10,10,10,10,0,0,10,10});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            myMessage.setBackground(shape);
        }
        else{
            myMessage.setBackgroundDrawable(shape);
        }
        myMessage.setTextSize(20);
        myMessage.setTextColor(Color.BLACK);
        myMessage.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        myMessage.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        ll1.setPadding(5, 8, 5, 5);
        ll1.addView(myMessageDet);
        ll1.addView(myMessage);
        messagescroller.addView(ll1);

        //Sending using objectstream method

        //Creating the message
        Log.d("address", "trying to send message click");
        Message newMessage = new Message(0, username, new Date(), userColor);
        newMessage.setTextMessage(messText);
        SendMessAsyncTaskobj clientASTobj = new SendMessAsyncTaskobj();
        clientASTobj.execute(newMessage);
        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.fullScroll(View.FOCUS_DOWN);
            }

        });
    }

    public void opencloseemojis(View view) {
        ConstraintLayout emojilayout = (ConstraintLayout) findViewById(R.id.emojis);
        if (emojilayout.getVisibility() == View.GONE) {
            emojilayout.setVisibility(View.VISIBLE);
        } else {
            emojilayout.setVisibility(View.GONE);
        }

    }

    public void sleepingemoji(View view) {
        emojiwork("sleepingemoji", R.drawable.sleepy);

    }

    public void emojiwork(String name, int id) {
        SendMessAsyncTaskobj clientAST = new SendMessAsyncTaskobj();
        String text = name;
        Message newMessage = new Message(4, username, new Date(), userColor);
        newMessage.setTextMessage(text);
        clientAST.execute(newMessage);


        Date date = new Date();

        String strDateFormat = "hh:mm:ss a";

        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);

        String formattedDate = dateFormat.format(date);
        LinearLayout ll1 = new LinearLayout(getApplicationContext());
        ll1.setOrientation(LinearLayout.VERTICAL);
        ll1.setGravity(Gravity.START);
        ll1.setGravity(Gravity.LEFT);
        ImageView img = new ImageView(getApplicationContext());
        TextView myMessageDet = new TextView(getApplicationContext());
        myMessageDet.setText(username + " at " + formattedDate);
        myMessageDet.setTextSize(13);
        myMessageDet.setTextColor(Color.BLACK);
        myMessageDet.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        img.setImageResource(id);

        img.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
        ll1.setPadding(20, 15, 5, 5);
        ll1.addView(myMessageDet);
        ll1.addView(img);
        messagescroller.addView(ll1);
        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.fullScroll(View.FOCUS_DOWN);
            }

        });

    }

    public void happyemoji(View view) {

        emojiwork("happyemoji", R.drawable.happy);


    }

    public void angryemoji(View view) {

        emojiwork("angryemoji", R.drawable.angry);
    }

    public void sademoji(View view) {

        emojiwork("sademoji", R.drawable.sad_1);
    }

    public void laughingemoji(View view) {

        emojiwork("laughingemoji", R.drawable.laughing_1);
    }

    public void boredemoji(View view) {

        emojiwork("boredemoji", R.drawable.bored);
    }


    class SendMessAsyncTaskobj extends AsyncTask<Message, Void, String> {
        @Override
        protected String doInBackground(Message... messages) {
            Message mess = messages[0];
            ArrayList<ClientScanResult> temprecievers = Receiverlist;
            int port = PORT;
            Socket socket;
            for (ClientScanResult x : temprecievers) {
                try {

                    Log.d("address", "trying to send to ip: " + x.getIpAddr());

                    socket = new Socket(x.getIpAddr().charAt(0) == '/' ? x.getIpAddr().substring(1) : x.getIpAddr(), port);

//                    PrintWriter out = new PrintWriter(socket.getOutputStream(),
//                            true);
//                    out.println(-1);

                    Log.d("address", "done creating socket for ip: " + x.getIpAddr());
                    ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
                    Log.d("address", "done creating object stream for ip: " + x.getIpAddr());
                    os.writeInt(-1);
                    os.writeObject(mess);
                    os.flush();

                    Log.d("address", "done writing object stream for ip: " + x.getIpAddr());
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

            Log.d("address", "message object sent");

        }
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

            for (ClientScanResult x : temprecievers) {
                try {

                    Log.d("address", "trying to send to ip: " + x.getIpAddr());

                    socket = new Socket(x.getIpAddr().charAt(0) == '/' ? x.getIpAddr().substring(1) : x.getIpAddr(), port);

                    Log.d("address", "creating socket for ip: " + x.getIpAddr());

                    PrintWriter out = new PrintWriter(socket.getOutputStream(),
                            true);

                    for (String l : lines) {
                        out.println(l);
                        Log.d("address", "sending message: " + l);
                    }
                    Log.d("address", "sending -1");
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

            Log.d("address", "message sent");

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
            for (ClientScanResult x : temprecievers) {
                if (x.getIpAddr().equals(sc.getInetAddress().toString())) {
                    found = true;
                }
            }
            if (!found) {
                Receiverlist.add(new ClientScanResult(sc.getInetAddress().toString(), true));
                Log.d("address", "Adding a new socket for " + (sc.getInetAddress().toString()));

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("address", "adding client finished");

        }
    }


    public void updateReceiverlist() {
        WifiManager tempWifiManager = wm;
        int intIP = tempWifiManager.getDhcpInfo().serverAddress;
        String mainIP = (intIP & 0xFF) + "     ." + ((intIP >> 8) & 0xFF) + "." + ((intIP >> 16) & 0xFF)
                + ".";
        String localIP = intToIP(wm.getConnectionInfo().getIpAddress());
        if (localIP.equals("0.0.0.0")) localIP = intToIP(intIP);
        ArrayList<ClientScanResult> tempReceiverList = new ArrayList<>();
        for (int i = 0; i <= 255; i++) {
            String currIP = mainIP + i;
            if (!localIP.equals(currIP)) {
                try {
                    if (InetAddress.getByName(currIP).isReachable(80) && isPortReachable(currIP, PORT)) {
                        tempReceiverList.add(new ClientScanResult(currIP, true));
                        Log.d("address", "found: " + currIP);
                    }
                } catch (Exception e) {
                    Log.d("address", "updateRecieverlist: " + e);
                }


            }
        }

        Receiverlist = tempReceiverList;
        String messText = writemess.getText().toString();
        Message newMessage = new Message(0, username, new Date(), userColor);
        newMessage.setTextMessage(messText);
        SendMessAsyncTaskobj clientAST = new SendMessAsyncTaskobj();
        clientAST.execute(newMessage);

    }


    boolean isPortReachable(String inHost, int inPort) {
        Socket socket = null;
        boolean retVal = false;
        try {
            try {
                socket = new Socket(inHost, inPort);
            } catch (IOException e) {
                Log.d("address", "" + e);
            }
            retVal = true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                }
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
                Log.d("address", "gets input stream in background for: " + mySocket.getInetAddress());
                InputStream is = mySocket.getInputStream();

                Log.d("address", "creates buffered reader in background" + mySocket.getInetAddress());
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));

                String result = br.readLine();
                if (result == null) return null;

                while (result != null && !result.equals("-1") && !result.equals("")) {
                    Log.d("address", "Reading Line: " + result);
                    main += result;
                    result = br.readLine();
                }
                Log.d("address", "receiving message object");

                if (result.equals("-1")) {
                    Message receivedMessage;
                    ObjectOutputStream oos = null;
                    ObjectInputStream ois = null;

                    try {
                        Log.d("address", "gets object stream in background for: " + mySocket.getInetAddress());

                        ois = new ObjectInputStream(mySocket.getInputStream());

                        receivedMessage = (Message) ois.readObject();

                        ois.close();
//                BufferedReader br = new BufferedReader(
//                        new InputStreamReader(is));
//
//                String result = br.readLine();
//                while (result != null && !result.equals("")){
//                    Log.d("address","Reading Lines");
//                    main += result;
//                    result = br.readLine();
//                }

                        //mySocket.close();
                    } catch (Exception e) {
                        Log.d("address", "serverasyncobj: " + e);

                        try {
                            oos.close();
                            ois.close();
                        } catch (Exception e1) {
                            Log.d("address", "serverasyncobj: " + e1);
                            return null;
                        }
                        return null;
                    }
                    Log.d("address", "ends serverasyncobj background");
                    return receivedMessage.getSender() + " : " + receivedMessage.getTextMessage();


                }

                //mySocket.close();
            } catch (Exception e) {
                Log.d("address", "serverasync: " + e);
            }
            Log.d("address", "ends serverasync background");

            return main;
        }

        @Override
        protected void onPostExecute(String s) {

            Log.d("UI thread", "I am the PO UI thread");
            Log.d("address", "onpostexecute: " + s);
//            Looper.prepare();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d("address", "updating client connection");

                    String text = incmessages.getText().toString() + "\n" + s;

                    Spannable spannable = new SpannableString(text);

                    for (int[] x : myMessages) {
                        spannable.setSpan(new ForegroundColorSpan(Color.RED), x[0], x[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    incmessages.setText(spannable);

                }
            });

        }
    }


    class ServerAsyncTaskobj extends AsyncTask<Socket, Void, Message> {
        @Override
        protected Message doInBackground(Socket... params) {
            Log.d("address", "inside serverasyncobj");
            Message receivedMessage = null;
            Socket mySocket = params[0];
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;

            try {
                Log.d("address", "gets object stream in background for: " + mySocket.getInetAddress());

                ois = new ObjectInputStream(mySocket.getInputStream());
                if (ois.readInt() == -1) {

                    receivedMessage = (Message) ois.readObject();
                }

                ois.close();
//                BufferedReader br = new BufferedReader(
//                        new InputStreamReader(is));
//
//                String result = br.readLine();
//                while (result != null && !result.equals("")){
//                    Log.d("address","Reading Lines");
//                    main += result;
//                    result = br.readLine();
//                }

                //mySocket.close();
            } catch (Exception e) {
                Log.d("address", "serverasyncobj: " + e);

                try {
                    oos.close();
                    ois.close();
                } catch (Exception e1) {
                    Log.d("address", "serverasyncobj: " + e1);
                    return null;
                }
                return null;
            }
            Log.d("address", "ends serverasyncobj background");
            return receivedMessage;
        }

        @Override
        protected void onPostExecute(Message message) {
            if (message == null) return;
            Log.d("UI thread", "I am the PO UI thread");
            Log.d("address", "onpostexecuteserverobj   :   " + message.getTextMessage());
            Log.d("address", "" + message.getMessType());
            //Check type of message
            if (message.getMessType() == 0) {

                //Looper.prepare();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("address", "updating client : " + message.getTextMessage());
//
//                    String text = incmessages.getText().toString()+"\n"+s;
//
//                    Spannable spannable = new SpannableString(text);
//
//                    for(int[] x: myMessages){
//                        spannable.setSpan(new ForegroundColorSpan(Color.RED), x[0], x[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    }

//                        incmessages.setText(incmessages.getText().toString() + "\n" + message.getSender() + " : " + message.getTextMessage() + "  " + message.getFormattedDate());
//                        Log.d("address","done updating UI");
//
                        LinearLayout ll1 = new LinearLayout(getApplicationContext());

                        ll1.setOrientation(LinearLayout.VERTICAL);
                        ll1.setGravity(Gravity.START);
                        ll1.setGravity(Gravity.LEFT);
                        TextView myMessage = new TextView(getApplicationContext());
                        myMessage.setText(message.getTextMessage());
                        TextView myMessageDet = new TextView(getApplicationContext());
                        myMessageDet.setText(message.getSender() + " at " + message.getFormattedDate());
                        myMessageDet.setTextSize(13);
                        myMessageDet.setTextColor(Color.BLACK);
                        myMessageDet.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

//        myMessage.setBackgroundResource(R.drawable.messageshape);
                        GradientDrawable shape = new GradientDrawable();
                        shape.setShape(GradientDrawable.RECTANGLE);
                        shape.setColor(message.getColor());
                        shape.setSize(200, 80);
                        shape.setStroke((int) 0.5, getResources().getColor(R.color.mymessagestroke));
                        shape.setCornerRadii(new float[]{10,10,10,10,10,10,0,0});
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            myMessage.setBackground(shape);
                        }
                        else{
                            myMessage.setBackgroundDrawable(shape);
                        }
                        Log.d("adf", "in the middle of something");
                        myMessage.setTextSize(20);
                        myMessage.setTextColor(Color.BLACK);
                        myMessage.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

                        myMessage.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                        ll1.setPadding(5, 8, 5, 5);
                        ll1.addView(myMessageDet);
                        ll1.addView(myMessage);
                        messagescroller.addView(ll1);
                        sv.post(new Runnable() {
                            @Override
                            public void run() {
                                sv.fullScroll(View.FOCUS_DOWN);
                            }

                        });
                    }
                });
            } else if (message.getMessType() == 1) {
                //Send the Server's receiverlist to client
                Log.d("address", "Sending server list to the client");
                SendMessAsyncTaskobj clientAST = new SendMessAsyncTaskobj();

                Message newMessage = new Message(2, Receiverlist);

                clientAST.execute(newMessage);
            } else if (message.getMessType() == 2) {
                //Get the response receiverlist from server
                Log.d("address", "using the response to update our receiverlist");
                Receiverlist = message.getReceiverList();
                Log.d("address", "done receiving the list from server");
                serverAck = true;
            } else if (message.getMessType() == 4) {
                //Emoji message received
                //Show it to user
                Log.d("address", "using the response to update our receiverlist");
                LinearLayout ll1 = new LinearLayout(getApplicationContext());

                ll1.setOrientation(LinearLayout.VERTICAL);
                ll1.setGravity(Gravity.START);
                ll1.setGravity(Gravity.LEFT);
                ImageView img = new ImageView(getApplicationContext());
                switch (message.getTextMessage()) {
                    case "happyemoji":
                        img.setImageResource(R.drawable.happy);
                        break;

                    case "angryemoji":
                        img.setImageResource(R.drawable.angry);
                        break;


                    case "sademoji":
                        img.setImageResource(R.drawable.sad_1);
                        break;

                    case "laughingemoji":
                        img.setImageResource(R.drawable.laughing_1);
                        break;
                    case "boredemoji":
                        img.setImageResource(R.drawable.bored);
                        break;

                    case "sleepingemoji":
                        img.setImageResource(R.drawable.sleepy);
                        break;

                    default:
                        img.setImageResource(R.drawable.happy);

                }

                TextView myMessageDet = new TextView(getApplicationContext());
                myMessageDet.setText(message.getSender() + " at " + message.getFormattedDate());
                myMessageDet.setTextSize(13);
                myMessageDet.setTextColor(Color.BLACK);
                myMessageDet.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));


                img.setLayoutParams(new FrameLayout.LayoutParams(100, 100));
                ll1.setPadding(20, 15, 5, 5);
                ll1.addView(myMessageDet);
                ll1.addView(img);
                messagescroller.addView(ll1);
                sv.post(new Runnable() {
                    @Override
                    public void run() {
                        sv.fullScroll(View.FOCUS_DOWN);
                    }

                });

            }
            doneServ = true;

        }
    }


}
//
//class SendMessage implements Runnable {
//
//    //check to see if it is a global message
//    boolean sendglobalmess;
//    boolean ip;
//
//    SendMessage(boolean sendglobalmess){
//        this.sendglobalmess = sendglobalmess;
//    }
//
//    @Override
//    public void run() {
//
//    }
//}
//
//class ReceiveMessage implements Runnable {
//
//    ReceiveMessage(){
//
//    }
//
//    @Override
//    public void run() {
//        //listen for incoming messages
//
//    }
//}
