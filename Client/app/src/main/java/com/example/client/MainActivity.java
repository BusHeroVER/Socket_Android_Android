package com.example.client;

import android.app.Activity;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MainActivity extends Activity {

    private TextView textResponse;
    private TextView state;

    private EditText editTextAddress;
    private EditText editTextPort;
    private EditText message;

    private Button buttonConnect;
    private Button buttonDisconnect;
    private Button buttonSend;

    Socket mySocket ;
    Handler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Initialize();
        setListeners();
    }

    public void Initialize(){
        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        message = (EditText)findViewById(R.id.message);

        buttonConnect = (Button) findViewById(R.id.connect);
        buttonDisconnect = (Button) findViewById(R.id.disconnect);
        buttonSend = (Button) findViewById(R.id.send);

        textResponse = (TextView) findViewById(R.id.response);
        state = (TextView) findViewById(R.id.state);

        myHandler = new Handler();
    }

    public void setListeners(){
        buttonConnect.setOnClickListener(socketConnect);
        buttonDisconnect.setOnClickListener(socketDisconnect);

        buttonSend.setOnClickListener(sendData);//補
    }

    private View.OnClickListener socketConnect = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Thread threadSocket = new Thread(runSocketConnect);
            threadSocket.start();
        }
    };

    private View.OnClickListener socketDisconnect = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            try {
                mySocket.close();

                if (mySocket.isClosed()) {
                    state.setText("Unconnect.");
                    myHandler.removeCallbacks(heartbeat);
                    myHandler.removeCallbacks(runReceiveData);
                } else {
                    state.setText("Connect.");
                }
            } catch (IOException e) {
                textResponse.setText("Disconnect error : " + e + ".\n");
            }
        }
    };

    private View.OnClickListener sendData = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Thread myThreadSend = new Thread(runSendData);
            myThreadSend.start();
        }
    };

    private  Runnable runSocketConnect = new Runnable() {

        @Override
        public void run() {
            try {
                mySocket = new Socket(editTextAddress.getText().toString(), Integer.parseInt(editTextPort.getText().toString()));

                if (mySocket.isClosed()) {
                    modifyStatus("Unconnect.");
                } else {
                    modifyStatus("Connect.");
                    clearResponse();

                    myHandler.postDelayed(heartbeat,1000);
                    Thread myThreadReceive = new Thread(runReceiveData);
                    myThreadReceive.start();
                }
            } catch (IOException e) {
                modifyResponse("Connect error : " + e + ".\n");

            }
        }
    };

    private Runnable runReceiveData = new Runnable() {
        @Override
        public void run() {
            try{
                InputStream myInputSteam = mySocket.getInputStream();
                InputStreamReader myInputSteamReader = new InputStreamReader(myInputSteam) ;
                BufferedReader myBufferedReader = new BufferedReader(myInputSteamReader) ;

                modifyResponse(myBufferedReader.readLine());
            }catch (IOException e){
                modifyResponse("Receive data error : "+ e +"\n");
            }
        }
    };

    private Runnable runSendData = new Runnable() {
        @Override
        public void run() {
            try{
                OutputStream myOutputSteam = mySocket.getOutputStream();
                OutputStreamWriter myOutputSteamWriter = new OutputStreamWriter(myOutputSteam);
                BufferedWriter myBufferedWriter = new BufferedWriter(myOutputSteamWriter);
                myBufferedWriter.write(message.getText().toString() + "\n");
                myBufferedWriter.flush();

                Thread myThreadReceive = new Thread(runReceiveData);
                myThreadReceive.start();
            }catch(IOException e){
                modifyResponse("Send data error :"+e+"\n");
            }
        }
    };

    private Runnable heartbeat = new Runnable() {
        @Override
        public void run() {
            try {
                mySocket.sendUrgentData(0xff);
                myHandler.postDelayed(this,1000) ;
            } catch (IOException e) {
                modifyStatus("Unconnect.");
                modifyResponse("Connect broken.");

                myHandler.removeCallbacks(heartbeat);
            }
        }
    };

    private void modifyResponse(final String text){
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                textResponse.append(text +"\n");
            }
        });
    }

    private void clearResponse(){
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                textResponse.setText("");
            }
        });
    }

    private void modifyStatus(final String text){
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                state.setText(text);
            }
        });
    }
}
