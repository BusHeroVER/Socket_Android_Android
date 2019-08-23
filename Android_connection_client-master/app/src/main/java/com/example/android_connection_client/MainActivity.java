package com.example.android_connection_client;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private String IP ="10.0.2.2" ;
    private String Port="9527" ;

    private Button button_auto;
    private Button button_connect  ;
    private Button button_disconnect ;
    private Button button_send ;

    private EditText editText_ip ;
    private EditText editText_port ;
    private EditText editText_messageSend ;

    private TextView textView_output ;
    private TextView textView_status ;

    Socket m_Socket ;
    Handler m_Handler ;

    private String input_data ;
    private String server_output_data ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Component
        Initialize();
    }

    // This function is main to initialize the whole APP
    // Doing Connection of Component and Event listeners)
    public void Initialize(){
        button_connect = findViewById(R.id.Button_connect) ;
        button_disconnect =findViewById(R.id.button_disconnect) ;

        button_send =findViewById(R.id.button_send);
        button_auto=findViewById(R.id.button_auto) ;

        editText_ip =findViewById(R.id.EditText_ip );
        editText_port =findViewById(R.id.EditText_port) ;
        editText_messageSend =findViewById(R.id.EditText_send) ;

        textView_output = findViewById(R.id.textView_output) ;
        textView_status =findViewById(R.id.textView_status) ;


        m_Handler = new Handler() ;

        // Setting Listeners...
        set_listeners();
    }
    // This function is main to connect the function and the component
    public void set_listeners(){
        button_connect.setOnClickListener(Socket_connect);
        button_disconnect.setOnClickListener(Socket_disconnect);

        button_send.setOnClickListener(Send_Data);
        button_auto.setOnClickListener(Auto_input);
    }

    /* ================================================= */
    //  Below is many kinds of Runnable Event
    // 1. Basic Button Event
    //
    /* ================================================= */

    // Socket_connection -> This method can start the use of socket
    private View.OnClickListener Socket_connect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // First get IP and port
            IP = editText_ip.getText().toString();
            Port =editText_port.getText().toString();
            textView_output.setText("");

            Thread thread_socket = new Thread(run_socket_connect) ;
            thread_socket.start();

        }
    };
    private View.OnClickListener Send_Data = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Get message first
            input_data = editText_messageSend.getText().toString();

            Thread thread_send = new Thread(run_send_data);
            thread_send.start();
        }
    };
    private View.OnClickListener Auto_input = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            editText_ip.setText("10.0.2.2");
            editText_port.setText("9527");
        }
    };
    private View.OnClickListener Socket_disconnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try{
                m_Socket.close();
                Check_Connection();

                m_Handler.removeCallbacks(run_heartbeat);

            }catch(IOException e){
                textView_output.setText("Socket disconnect error: "+e);
            }
        }
    };

    // Function main to check the socket status -> Show  the status on textView_status
    private void Check_Connection(){
        if (m_Socket.isClosed() ==true){
            textView_status.setText("Unconnected !");
        }else{
            textView_status.setText("Connected !");
        }
    }

    /* ================================================= */
    //  Runnable Event About..
    //  1. Socket
    //  2. Receive Data
    //  3. Send Data
    /* ================================================= */
    private Runnable run_socket_connect = new Runnable() {
        @Override
        public void run() {
            try{
                m_Socket = new Socket(IP, Integer.parseInt(Port)) ;

                Check_Connection();
                HeartBeat_start();
                // Need a thread to catch information

                Receive_data();

            }catch (IOException e){
                textView_output.setText("Socket connect error: "+e+"\n");
            }
        }
    };

    // Method to Receive data
    private void Receive_data(){
        // Need a thread to catch information
        Thread m_thread_Receive = new Thread(run_receive_data) ;
        m_thread_Receive.start();
    }

    private Runnable run_receive_data = new Runnable() {
        @Override
        public void run() {
            // Input stream setting
            try{
                InputStream m_InputSteam = m_Socket.getInputStream();
                InputStreamReader m_InputStreamReader = new InputStreamReader(m_InputSteam) ;
                BufferedReader m_BufferedReader = new BufferedReader(m_InputStreamReader) ;

                server_output_data = m_BufferedReader.readLine();

                // Get information -> Call handler to Handle UI
                Modify_UI(server_output_data);

            }catch (IOException e){
                textView_output.setText("Receive data error: "+e+"\n");
            }
        }
    };

    private Runnable run_send_data = new Runnable() {
        @Override
        public void run() {
            try{
                OutputStream m_outputstream = m_Socket.getOutputStream();
                m_outputstream.write((input_data+"\n").getBytes());
                m_outputstream.flush();

                // Receive data echo
                Receive_data();

            }catch(IOException e){
                textView_output.setText("Send data error:"+e+"\n");
            }
        }
    };

    private void Modify_UI(final String source){
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                textView_output.append(source +"\n");
            }
        });
    }
    private void Modify_UI_Status(final String source){
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                textView_status.setText(source);
            }
        });
    }

    private void HeartBeat_start(){
        m_Handler.postDelayed(run_heartbeat,2000) ;
    }

    private Runnable run_heartbeat = new Runnable() {
        @Override
        public void run() {
            try{

                //Modify_UI("beat..");
                m_Socket.sendUrgentData(0xff);
                m_Handler.postDelayed(this,2000) ;

            }catch(IOException e){
                Modify_UI("Heart broken");
                Modify_UI_Status("Unconnected !");
                m_Handler.removeCallbacks(run_heartbeat);

            }
        }
    };
}
