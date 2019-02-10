package projects.secretdevelopers.com.bunkchat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerClass implements Runnable {
    Context context;
    WifiManager wm;
    ServerClass(Context appcontext, WifiManager wm){
        this.context = appcontext;
        this.wm = wm;
    }


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

    //This will be used when the user has decided to be the server


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

            Log.d("UI thread", "I am the PO UI thread");
            Log.d("", ""+s);

        }
    }

    public String intToIP(int i) {
        return ((i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + ((i >> 24) & 0xFF));
    }

}
