package com.example.server;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private TextView info;
    private TextView infoip;
    private TextView message;

    private String text = "";

    private Button close;

    private ServerSocket serverSocket;
    private replyThread replyThread;

    private boolean flag = false;

    Socket socket;
    Handler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Initialize();
        threadStart();
    }
    public void Initialize(){
        info = (TextView) findViewById(R.id.info);
        infoip = (TextView) findViewById(R.id.infoip);
        message = (TextView) findViewById(R.id.msg);

        close = (Button) findViewById(R.id.close);
        close.setOnClickListener(serverClose);

        infoip.setText(getIpAddress());
    }

    private View.OnClickListener serverClose = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(!socket.isClosed()){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                message.setText("Socket is closed.");
            }
        }
    };

    public void threadStart(){

        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    private class SocketServerThread extends Thread {

        static final int SocketServerPORT = 9487;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        info.setText("Port : "
                                + serverSocket.getLocalPort());
                    }
                });
                while (true) {
                    socket = serverSocket.accept();

                    replyThread = new replyThread(socket);
                    replyThread.run();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        message.setText("Client is unconnected.");
                    }
                });
            }
            flag = true;
            threadStart();
        }
    }

    private class replyThread extends Thread {

        private Socket mySocket;
        private int count = 0;
        private String messageFromClient = "";

        replyThread(Socket socket) {mySocket = socket;}

        @Override
        public void run() {

            try{
                while(true){
                    InputStream myInputSteam = mySocket.getInputStream();
                    InputStreamReader myInputSteamReader = new InputStreamReader(myInputSteam) ;
                    BufferedReader myBufferedReader = new BufferedReader(myInputSteamReader) ;

                    messageFromClient = myBufferedReader.readLine();
                    if(messageFromClient == null)
                        break;

                    count++;
                    text += "#" + count + " from : " + mySocket.getInetAddress()
                            + " : " + mySocket.getPort() + "\n"
                            + "Message from client : " + messageFromClient + ".\n";

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            message.setText(text);
                        }
                    });

                    OutputStream myOutputSteam = mySocket.getOutputStream();
                    OutputStreamWriter myOutputSteamWriter = new OutputStreamWriter(myOutputSteam);
                    BufferedWriter myBufferedWriter = new BufferedWriter(myOutputSteamWriter);
                    myBufferedWriter.write("Receive : " + messageFromClient + "\n");
                    myBufferedWriter.flush();
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                final String errMsg = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        message.setText(errMsg);
                    }
                });
            } finally {
                if (mySocket != null) {
                    try {
                        mySocket.close();
                        serverSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

//                if (dataInputStream != null) {
//                    try {
//                        dataInputStream.close();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//
//                if (dataOutputStream != null) {
//                    try {
//                        dataOutputStream.close();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
            }
        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip = "IP : "
                                + inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong !" + e.toString() + "\n";
        }
        return ip;
    }
}
