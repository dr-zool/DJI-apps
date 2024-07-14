package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends Activity implements OnClickListener{

    private static final String TAG = MainActivity.class.getName();

    private static final int FLOAT_SIZE = 4 ;
    private static final int BITS_IN_BYTE = 8;
    private Button mSetLocBtn, mSpiralBtn, mTestSignal;
    private EditText heightView, spiralView, rotateView;
    private TextView rView, pView, yView, latView, lonView, altView, batteryView, gpsView, LiveLView, LiveHView, DistLView, DistHView, DistanceView, DistLabelView;
    private Spinner radius_spin;
    private Handler handler;
    private CommonCallbacks.CompletionCallback sendDataCallback;
    String radius_arr[] = {"6","7","8","9","10","11","12","13"};
    float height;
    int gpsHealth = 0;
    String radius;
    String vel_x,vel_y, vel_z;
    String lat,lon, alt;
    String r,p, y;
    Aircraft mAircraft ;
    FlightController mFlightController  ;
    AlertDialog.Builder builder ;
    AlertDialog alertDialog;




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();


    initUI();



        };





    protected void onProductChange() {
        initPreviewer();

    }



    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
//        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }


    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {

        mSetLocBtn = (Button) findViewById(R.id.set_loc);
        mSpiralBtn = (Button) findViewById(R.id.spiral);
        radius_spin = (Spinner) findViewById(R.id.radius_id);

        heightView = (EditText)findViewById(R.id.height);
        rotateView = (EditText)findViewById(R.id.rotate_time);
        spiralView = (EditText)findViewById(R.id.spiral_time);


        mSetLocBtn.setOnClickListener(this);
        mSpiralBtn.setOnClickListener(this);

        ArrayAdapter ad
                = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                radius_arr);

        ad.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item);
        radius_spin.setAdapter(ad);
        radius_spin.setSelection(0);




    }

    private void initPreviewer() {

        BaseProduct product = com.example.myapplication.FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            mAircraft = (Aircraft)product;
            mFlightController= mAircraft.getFlightController();
            gpsView = (TextView)findViewById(R.id.gps_health);
            LiveLView = (TextView) findViewById(R.id.live_low);
            LiveHView = (TextView) findViewById(R.id.live_high);
            DistLView = (TextView) findViewById(R.id.interrupt_low);
            DistHView = (TextView) findViewById(R.id.interrupt_high);
            DistanceView = (TextView) findViewById(R.id.distance);
            DistLabelView =(TextView)findViewById(R.id.interrupt_id);
            mFlightController.setOnboardSDKDeviceDataCallback(new FlightController.OnboardSDKDeviceDataCallback() {
            @Override
            public void onReceive(byte[] bytes) {
                StringBuilder buffer = new StringBuilder();


                for (byte b : bytes) {

                    buffer.append((char) b);
                }
                Log.v(TAG, Arrays.toString(bytes));
                if(bytes[0] == 2){
                    if(bytes[1] == 1){
                        showToast("Upload successful");
                    }
                    else{
                        showToast("Upload failed");
                    }
                }
                if(bytes[0]==3){
                    gpsHealth = bytes[1];
                    String gps_s = String.valueOf(gpsHealth);
                    gpsView.setText(gps_s);
                    if (gpsHealth < 3){
                        gpsView.setTextColor(Color.parseColor("#F44336"));
                    }
                    else if(gpsHealth < 4){
                        gpsView.setTextColor(Color.parseColor("#FFEB3B"));
                    }
                    else{
                        gpsView.setTextColor(Color.parseColor("#2CB027"));
                    }
                }
                if(bytes[0]==4){
                    showToast("GPS Less than 4, braked!");
                }
                if(bytes[0] == 7){
                    byte[] data_bytes = Arrays.copyOfRange(bytes,1,5);
                    int freq = ByteBuffer.wrap(data_bytes).getInt();
                    String freq_s = String.format("%d MHz",freq);
//                    Log.v(TAG, String.valueOf(freq));
                    LiveLView.setText(freq_s);
                    if(gpsHealth <1){
                        if(freq < 27000 && freq > 900){
                            DistLabelView.setText("Interruption Frequency (2.4G)");
                        }
                        else{
                            DistLabelView.setText("Interruption Frequency (5.8G)");
                        }
                        DistLView.setText(freq_s);
                    }
                    if (gpsHealth < 3){
                        LiveLView.setTextColor(Color.parseColor("#F44336"));
                    }
                    else if(gpsHealth < 4){
                        LiveLView.setTextColor(Color.parseColor("#FFEB3B"));
                    }
                    else{
                        LiveLView.setTextColor(Color.parseColor("#2CB027"));
                    }


                }

                if(bytes[0] == 8){
                    byte[] data_bytes = Arrays.copyOfRange(bytes,1,5);
                    int freq = ByteBuffer.wrap(data_bytes).getInt();
                    String freq_s = String.format("%d MHz",freq);

                    LiveHView.setText(freq_s);
                    if(gpsHealth <1){
                        DistHView.setText(freq_s);
                    }
                    if (gpsHealth < 3){
                        DistHView.setText(freq_s);
                        LiveHView.setTextColor(Color.parseColor("#F44336"));
                    }
                    else if(gpsHealth < 4){
                        LiveHView.setTextColor(Color.parseColor("#FFEB3B"));
                    }
                    else{
                        LiveHView.setTextColor(Color.parseColor("#2CB027"));
                    }


                }
                if(bytes[0] == 9 ){
                    DistanceView.setText( String.valueOf(bytes[1]));
                }

            }
            });

            sendDataCallback = new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    //showToast("数据透传" + (djiError == null ? "成功！" : djiError.getDescription()));
                }
            };

            rView = (TextView)findViewById(R.id.row) ;
            pView = (TextView)findViewById(R.id.pitch) ;
            yView = (TextView)findViewById(R.id.yaw) ;

            latView = (TextView)findViewById(R.id.lat) ;
            lonView = (TextView)findViewById(R.id.lon) ;
            altView = (TextView)findViewById(R.id.alt) ;

            Log.v(TAG, "SDFN");
//            batteryView= (TextView)findViewById(R.id.battery_status);

            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {

                    r = String.valueOf(flightControllerState.getAttitude().roll);
                    p = String.valueOf(flightControllerState.getAttitude().pitch);
                    y = String.valueOf(flightControllerState.getAttitude().yaw);

                    alt = String.valueOf(flightControllerState.getAircraftLocation().getAltitude());
                    lat = String.format("%.2f",flightControllerState.getAircraftLocation().getLatitude());
                    lon = String.format("%.2f",flightControllerState.getAircraftLocation().getLongitude());

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(r != null){ rView.setText(r);}
                            if(p != null){ pView.setText(p);}
                            if(y != null){ yView.setText(y);}

                            if(lat != null){latView.setText(lat);}
                            if(lon != null){lonView.setText(lon);}
                            if(alt != null){altView.setText(alt);}

                        }
                    });








                }
            });
//            mAircraft.getBattery().setStateCallback(new BatteryState.Callback() {
//                @Override
//                public void onUpdate(BatteryState djiBatteryState) {
//                    MainActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            batteryView.setText(djiBatteryState.getChargeRemainingInPercent());
//                        }
//                    });
//
//
//                }
//            });







        }
    }

    private void uninitPreviewer() {
        Camera camera = com.example.myapplication.FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }



    @Override
    public void onClick(View v) {
        byte cmd, height, rotate, spiral,radius_b;
        height = new Byte(heightView.getText().toString());

        radius_b = new Byte(radius_spin.getSelectedItem().toString());
        AlertDialog alertDialog;
        switch (v.getId()) {
            case R.id.set_loc:
                cmd = 1;
                builder = new AlertDialog.Builder(this);
                builder.setMessage("Do you want to set current location as origin (XYZ) ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mFlightController.sendDataToOnboardSDKDevice(new byte[]{cmd}, sendDataCallback);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showToast("Please set again later ");
                            }
                        });
                // Create the AlertDialog object and return it
                alertDialog = builder.create();
                alertDialog.show();

                break;

            case R.id.spiral:
                cmd=0;
                builder = new AlertDialog.Builder(this);
                builder.setMessage("Do you want to start move spiral with \nHeight: "+ height +"m\n Radius: "+radius_spin.getSelectedItem().toString()+"m ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mFlightController.sendDataToOnboardSDKDevice(new byte[]{cmd,height,radius_b,}, sendDataCallback);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showToast("Please double check the values ");
                            }
                        });
                // Create the AlertDialog object and return it
                alertDialog = builder.create();
                alertDialog.show();

                break;



            default:
                break;
        }
    }



    public void onItemSelected(AdapterView<?> parent,
                               View arg1,
                               int position,
                               long id)
    {
        radius = radius_arr[position];
    }

    public void onNothingSelected(AdapterView<?> parent)
    {
        // Auto-generated method stub
    }



    private boolean isM300(){
        BaseProduct baseProduct = com.example.myapplication.FPVDemoApplication.getProductInstance();

        if (baseProduct != null) {
            return baseProduct.getModel() == Model.MATRICE_300_RTK;
        }
        return false;
    }

    public static void FloatToBytes(byte [] buffer, int pos, float input)
    {
        //  Integer representation for bitwise operations
        int value = Float.floatToIntBits(input);

        // Convert the float to array of bytes
        for(int i = pos; i < pos + FLOAT_SIZE; i++)
        {
            buffer[i] = (byte) (value >> (((FLOAT_SIZE -1) -(i - pos)) * BITS_IN_BYTE));
        }

    }
}
