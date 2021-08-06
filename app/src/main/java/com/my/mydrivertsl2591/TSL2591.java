package com.my.mydrivertsl2591;

import android.os.SystemClock;

import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.Observable;

public class TSL2591 implements AutoCloseable {
    /// Lux coefficient
    public static final float TSL2591_LUX_DF = 408.0F;

    //Channels of light spectrum
    private static final int TSL2591_VISIBLE = 2; // channel 0 - channel 1
    private static final int TSL2591_INFRARED = 1; // channel 1
    private static final int TSL2591_FULLSPECTRUM = 0; // channel 0

    // Control register
    private static final int TSL2591_REGISTER_CONTROL = 0x01;

    // Enumeration for the sensor gain
    public static final int TSL2591_GAIN_LOW = 0x00;  /// low gain (1x)
    public static final int TSL2591_GAIN_MED = 0x10;  /// medium gain (25x)
    public static final int TSL2591_GAIN_HIGH = 0x20; /// medium gain (428x)
    public static final int TSL2591_GAIN_MAX = 0x30;  /// max gain (9876x)

    // Enumeration for the sensor integration timing
    public static final int TSL2591_INTEGRATIONTIME_100MS = 0x00; // 100 millis
    public static final int TSL2591_INTEGRATIONTIME_200MS = 0x01; // 200 millis
    public static final int TSL2591_INTEGRATIONTIME_300MS = 0x02; // 300 millis
    public static final int TSL2591_INTEGRATIONTIME_400MS = 0x03; // 400 millis
    public static final int TSL2591_INTEGRATIONTIME_500MS = 0x04; // 500 millis
    public static final int TSL2591_INTEGRATIONTIME_600MS = 0x05; // 600 millis

    private static final int TSL2591_COMMAND_BIT = 0xA0;
    // Enable register
    private static final int TSL2591_REGISTER_ENABLE = 0x00;
    // Control register
    private static final int TSL2591_REGISTER_CONFIG = 0x01;
    // Flag for ENABLE register to disable
    private static final int TSL2591_ENABLE_POWEROFF = 0x00;
    // Flag for ENABLE register to enable
    private static final int TSL2591_ENABLE_POWERON = 0x01;
    //ALS Enable. This field activates ALS function. Writing a one activates the ALS. Writing a zero disables the ALS.
    private static final int TSL2591_ENABLE_AEN = 0x02;
    /// ALS Interrupt Enable. When asserted permits ALS interrupts to be generated, subject to the persist filter.
    private static final int TSL2591_ENABLE_AIEN = 0x10;

    // Channel 0 data, low byte
    private static final int TSL2591_REGISTER_CHAN0_LOW = 0x14;
    // Channel 0 data, high byte
    private static final int TSL2591_REGISTER_CHAN0_HIGH = 0x15;
    // Channel 1 data, low byte
    private static final int TSL2591_REGISTER_CHAN1_LOW = 0x16;
    // Channel 1 data, high byte
    private static final int TSL2591_REGISTER_CHAN1_HIGH = 0x17;


    public static final float TSL2591_LUX_COEFB = 1.64F; ///< CH0 coefficient
    public static final float TSL2591_LUX_COEFC = 0.59F; ///< CH1 coefficient A
    public static final float TSL2591_LUX_COEFD = 0.86F;  ///< CH2 coefficient B

    private int gain = TSL2591_GAIN_LOW;
    private int integration = TSL2591_INTEGRATIONTIME_600MS;
    private I2cDevice mDevice;
    private boolean autoGain = true;
    //Default I2C address for the sensor
    public static final int DEFAULT_I2C_ADDRESS = 0x29;
    private final byte[] mBuffer = new byte[3]; // for reading sensor values

    public TSL2591(String bus) throws Exception {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    public TSL2591(String bus, int address) throws Exception {
        PeripheralManager pioService = PeripheralManager.getInstance();
        I2cDevice device = pioService.openI2cDevice(bus, address);
        try {
            connect(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    TSL2591(I2cDevice device) throws IOException {
        connect(device);
    }

    @Override
    public void close() throws Exception {
        if (mDevice != null) {
            try {
               // turnOff();
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    public void setAutoGain(final boolean autoGain) {
        this.autoGain = autoGain;
    }


    public void connect(I2cDevice device) throws IOException {
        mDevice = device;

     //   turnOn();
        setGain();
        setIntegration();
    }




    public void setGain() throws IOException {
        setGain(TSL2591_GAIN_LOW);
    }

    public void setGain(final int gain) throws IOException {
        setGainAndIntegration(gain, integration);
    }

    public void setIntegration() throws IOException {
        setIntegration(TSL2591_INTEGRATIONTIME_600MS);
    }

    public void setIntegration(final int integration) throws IOException {
        setGainAndIntegration(gain, integration);
    }

    public void setGainAndIntegration(final int gain, final int integration) throws IOException {
        if (gain != TSL2591_GAIN_LOW && gain != TSL2591_GAIN_HIGH) {
            throw new IllegalArgumentException("Bad gain value [" + gain + "]");
        }
        if (integration != TSL2591_INTEGRATIONTIME_100MS && integration != TSL2591_INTEGRATIONTIME_200MS
                && integration != TSL2591_INTEGRATIONTIME_300MS && integration != TSL2591_INTEGRATIONTIME_400MS
                && integration != TSL2591_INTEGRATIONTIME_500MS && integration != TSL2591_INTEGRATIONTIME_600MS) {
            throw new IllegalArgumentException("Bad integration time value [" + integration + "]");
        }

        if (gain != this.gain || integration != this.integration) {
            writeSample(TSL2591_REGISTER_CONFIG,(byte) (gain | integration));
            this.gain = gain;
            this.integration = integration;

            switch (integration) {
                case TSL2591_INTEGRATIONTIME_100MS:
                    waitFor(101L);
                    break;
                case TSL2591_INTEGRATIONTIME_200MS:
                    waitFor(201L);
                    break;
                case TSL2591_INTEGRATIONTIME_300MS:
                    waitFor(301L);
                    break;
                case TSL2591_INTEGRATIONTIME_400MS:
                    waitFor(401L);
                    break;
                case TSL2591_INTEGRATIONTIME_500MS:
                    waitFor(501L);
                    break;
                case TSL2591_INTEGRATIONTIME_600MS:
                default:
                    waitFor(601L);
                    break;
            }
        }
    }



    public void turnOn() throws IOException {
        writeSample(TSL2591_REGISTER_CONTROL, (byte) TSL2591_ENABLE_POWERON);
    }


    private void turnOff() throws IOException {
        writeSample(TSL2591_REGISTER_ENABLE, (byte) TSL2591_ENABLE_POWEROFF);
    }

    private void writeSample(final int address, final byte command) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        mDevice.writeRegByte(TSL2591_COMMAND_BIT | address, command);
    }


    private void waitFor(final long howMuch) {
        SystemClock.sleep(howMuch);
    }



    public  float getLux() {
        float f_time;
        float f_gain;
        float cpl;
        float ch0 = 0;
        float ch1 = 0;
        float     lux1, lux2, lux;
        float chan0, chan1;
/*
        // Check for overflow conditions first
        if ((ch0 == 0xFFFF) | (ch1 == 0xFFFF))
        {
            // Signal an overflow
            return 0;
        }*/


        switch (integration){
            case TSL2591_INTEGRATIONTIME_100MS:
                f_time = 100.0F;
                break;
            case TSL2591_INTEGRATIONTIME_200MS:
                f_time = 200.0F;
                break;
            case TSL2591_INTEGRATIONTIME_300MS:
                f_time = 300.0F;
                break;
            case TSL2591_INTEGRATIONTIME_400MS:
                f_time = 400.0F;
                break;
            case TSL2591_INTEGRATIONTIME_500MS:
                f_time = 500.0F;
                break;
            case TSL2591_INTEGRATIONTIME_600MS:
                f_time = 600.0F;
                break;
            default:
                f_time = 100.0F;
                break;

        }
        switch (gain){
            case TSL2591_GAIN_LOW:
                f_gain = 1.0F;
            case TSL2591_GAIN_MED:
                f_gain = 25.0F;
                break;
            case TSL2591_GAIN_HIGH:
                f_gain = 428.0F;
                break;
            case TSL2591_GAIN_MAX:
                f_gain = 9876.0F;
                break;
            default:
                f_gain = 1.0F;
                break;

        }


        // cpl = (ATIME * AGAIN) / DF
        cpl = (f_time * f_gain) / TSL2591_LUX_DF;

        lux1 = ( (float)ch0 - (TSL2591_LUX_COEFB * (float)ch1) ) / cpl;
        lux2 = ( ( TSL2591_LUX_COEFC * (float)ch0 ) - ( TSL2591_LUX_COEFD * (float)ch1 ) ) / cpl;

        // The highest value is the approximate lux equivalent
        lux = Math.max(lux1, lux2);


        return lux;
    }


    }
