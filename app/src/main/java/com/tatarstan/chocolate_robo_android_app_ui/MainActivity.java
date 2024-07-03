package com.tatarstan.chocolate_robo_android_app_ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements WiFiDialogFragment.WiFiDialogListener {

    private String camera_IP;
    private final static String STREAM_SUFFIX = "/stream";
    private final static String COMMAND_SUFFIX = "/command?action=";
    private final static String ADD_POINT_SUFFIX = "/add_point";
    private final static String RECORD_REFRESH_SUFFIX = "/record_refresh";
    private final static String STOP_COMMAND = "stop";
    private final static String ROUTE_COMMAND = "route";
    private final static String UP_COMMAND = "up";
    private final static String LEFT_COMMAND = "left";
    private final static String RIGHT_COMMAND = "right";
    private final static String DOWN_COMMAND = "down";
    private final static String NO_ACTION = "";
    private final static String PORT = ":8081";
    private final static String ESP_32_HOTSPOT = "\"ESP32-Hotspot\"";

    public enum RecordState {
        STOPPED,
        RUNNING,
        PAUSED
    }

    ImageButton buttonUp;
    ImageButton buttonLeft;
    ImageButton buttonRight;
    ImageButton buttonDown;
    Button buttonRecord;
    Button buttonReproduce;
    Button buttonAddPoint;
    Button buttonLoad;
    Button buttonUpdateWiFi;
    EditText editTextUrl;


    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        buttonUp = (ImageButton) findViewById(R.id.buttonUp);
        buttonLeft = (ImageButton) findViewById(R.id.buttonLeft);
        buttonRight = (ImageButton) findViewById(R.id.buttonRight);
        buttonDown = (ImageButton) findViewById(R.id.buttonDown);

        buttonRecord = (Button) findViewById(R.id.buttonRecord);
        buttonReproduce = (Button) findViewById(R.id.buttonReproduce);
        buttonAddPoint = (Button) findViewById(R.id.buttonAddPoint);
        buttonLoad = (Button) findViewById(R.id.buttonLoad);
        buttonUpdateWiFi = (Button) findViewById(R.id.buttonUpdateWiFi);
        editTextUrl = (EditText) findViewById(R.id.editTextURL);
        AtomicReference<RecordState> recordState = new AtomicReference<>(RecordState.STOPPED);

        buttonUp.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (recordState.get().equals(RecordState.RUNNING)) {
                    buttonReproduce.setText(R.string.btn_resume_record);
                }
                handleRequest(COMMAND_SUFFIX, UP_COMMAND);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                handleRequest(COMMAND_SUFFIX, STOP_COMMAND);
            }
            return false;
        });
        buttonDown.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (recordState.get().equals(RecordState.RUNNING)) {
                    buttonReproduce.setText(R.string.btn_resume_record);
                }
                handleRequest(COMMAND_SUFFIX, DOWN_COMMAND);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                handleRequest(COMMAND_SUFFIX, STOP_COMMAND);
            }
            return false;
        });
        buttonLeft.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (recordState.get().equals(RecordState.RUNNING)) {
                    buttonReproduce.setText(R.string.btn_resume_record);
                }
                handleRequest(COMMAND_SUFFIX, LEFT_COMMAND);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                handleRequest(COMMAND_SUFFIX, STOP_COMMAND);
            }
            return false;
        });
        buttonRight.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (recordState.get().equals(RecordState.RUNNING)) {
                    buttonReproduce.setText(R.string.btn_resume_record);
                }
                handleRequest(COMMAND_SUFFIX, RIGHT_COMMAND);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                handleRequest(COMMAND_SUFFIX, STOP_COMMAND);
            }
            return false;
        });

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_WIFI_STATE,
                             Manifest.permission.ACCESS_COARSE_LOCATION,
                             Manifest.permission.ACCESS_FINE_LOCATION},
                PackageManager.PERMISSION_GRANTED);

        buttonRecord.setOnClickListener(view -> {
            buttonAddPoint.setEnabled(true);
            handleRequest(RECORD_REFRESH_SUFFIX, NO_ACTION);
            buttonRecord.setText(R.string.btn_refresh_record);
            if(recordState.get().equals(RecordState.RUNNING) || recordState.get().equals(RecordState.PAUSED)) {
                recordState.set(RecordState.STOPPED);
                buttonReproduce.setText(R.string.btn_reproduce_record);
            }
        });

        buttonAddPoint.setOnClickListener(view -> {
            handleRequest(ADD_POINT_SUFFIX, NO_ACTION);
        });

        buttonReproduce.setOnClickListener(view -> {
            if (recordState.get().equals(RecordState.PAUSED) || recordState.get().equals(RecordState.STOPPED)) {
                handleRequest(COMMAND_SUFFIX, ROUTE_COMMAND);
                recordState.set(RecordState.RUNNING);
                buttonReproduce.setText(R.string.btn_pause_record);
            } else if (recordState.get().equals(RecordState.RUNNING)) {
                handleRequest(COMMAND_SUFFIX, STOP_COMMAND);
                recordState.set(RecordState.PAUSED);
                buttonReproduce.setText(R.string.btn_resume_record);
            }
        });

        buttonUpdateWiFi.setOnClickListener(view -> {
            String ssid = getCurrentSsid();
            if (ssid.equals(ESP_32_HOTSPOT)) {
                showWiFiDialog();
            }
        });

        buttonLoad.setOnClickListener(view -> {
            camera_IP = editTextUrl.getText().toString();
            webView.loadUrl(camera_IP + STREAM_SUFFIX);
        });

    }

    private void handleRequest(String suffix, String action){
            RequestQueue volleyQueue = Volley.newRequestQueue(MainActivity.this);
            String url = "http://" + camera_IP + PORT + suffix + action;
            Log.d("URL: ",url);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {},
                    error -> {}
            );
            volleyQueue.add(jsonObjectRequest);
    }

    public String getCurrentSsid() {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        assert networkInfo != null;
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !connectionInfo.getSSID().equals("")) {
                ssid = connectionInfo.getSSID();
            }
        }
        return ssid;
    }

    public void showWiFiDialog() {
        DialogFragment dialog = new WiFiDialogFragment();
        dialog.show(getSupportFragmentManager(), "WiFiDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        EditText ssidEditText = (EditText) dialog.getDialog().findViewById(R.id.ssid);
        EditText pswdEditText = (EditText) dialog.getDialog().findViewById(R.id.password);
        RequestQueue volleyQueue = Volley.newRequestQueue(MainActivity.this);
        String url = "http://192.168.4.1/credentials?ssid=" + ssidEditText.getText().toString() + "&pswd=" + pswdEditText.getText().toString() + "&";
        Log.d("URL: ",url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {},
                error -> {}
        );
        volleyQueue.add(jsonObjectRequest);
    }

}