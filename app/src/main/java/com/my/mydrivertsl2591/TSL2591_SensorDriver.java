package com.my.mydrivertsl2591;

import static android.content.ContentValues.TAG;

import android.app.Activity;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

//Driver koji ispisuje neku adresu

public class TSL2591_SensorDriver extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState)  {

        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);
        try{


            PeripheralManager manager = PeripheralManager.getInstance();
            List<String> deviceList = manager.getI2cBusList();
            Log.d(TAG, "Device:" + deviceList);




            I2cDevice device = PeripheralManager.getInstance().openI2cDevice("I2C1", 0x29);
            TSL2591 sensor = new TSL2591(device);

            sensor.setIntegration(TSL2591.TSL2591_INTEGRATIONTIME_600MS);
            sensor.setGain(TSL2591.TSL2591_GAIN_LOW);




            float lux = sensor.getLux();
            Log.d(TAG, "Lux:" +lux);
            DatabaseReference dataBase;
            dataBase = FirebaseDatabase.getInstance().getReference("Lux");
            Configuration con = new Configuration(lux);
            dataBase.setValue(con);

            sensor.close();





            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }


