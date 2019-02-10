package projects.secretdevelopers.com.bunkchat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientClass implements Runnable{

    //This will be used when the user has decided to be the client

    Context context;
    WifiManager wm;

    ClientClass(Context appcontext, WifiManager wm){
        this.context = appcontext;
        this.wm = wm;
    }

    @Override
    public void run() {
        ClientAsyncTask clientAST = new ClientAsyncTask();
        clientAST.execute(new String[] {intToIP(wm.getDhcpInfo().serverAddress), "9809", "Niloy"+" : "+"lovenot hate" });

    }



    //Client will make his first call to this.
    //And the server will send a message back.

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
                Log.d("address", "" + socket.isConnected());

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
                    Log.d("address", ""+s);

                }
            });
        }
    }




    //Client Will use this to send message
    //Update name so that it doesn't conflict with the serversendmessage

    class SendMessAsyncTask extends AsyncTask<String, Void, String> {
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

    public String intToIP(int i) {
        return ((i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + ((i >> 24) & 0xFF));
    }
}
