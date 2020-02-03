package com.example.swampapp;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap<>();

    //Services
    public static String BLUETOOTH_LE_CCCD            = "00002902-0000-1000-8000-00805f9b34fb";
    public static String BLUETOOTH_LE_CC254X_SERVICE  = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String BLUETOOTH_LE_CC254X_CHAR_RW  = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String BLUETOOTH_LE_NRF_SERVICE     = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static String BLUETOOTH_LE_NRF_CHAR_RW2    = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static String BLUETOOTH_LE_NRF_CHAR_RW3    = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static String BLUETOOTH_LE_RN4870_SERVICE  = "49535343-FE7D-4AE5-8FA9-9FAFD205E455";
    public static String BLUETOOTH_LE_RN4870_CHAR_RW  = "49535343-1E4D-4BD9-BA61-23C647249616";

    static {
        //Serial Services and Characteristics of different vendors
        attributes.put(BLUETOOTH_LE_CC254X_SERVICE, BLUETOOTH_LE_CC254X_CHAR_RW);
        attributes.put(BLUETOOTH_LE_NRF_SERVICE, BLUETOOTH_LE_NRF_CHAR_RW2);
        attributes.put(BLUETOOTH_LE_NRF_SERVICE, BLUETOOTH_LE_NRF_CHAR_RW3);
        attributes.put(BLUETOOTH_LE_RN4870_SERVICE, BLUETOOTH_LE_RN4870_CHAR_RW);
    }

    public static boolean isOnSampleAttributes(String uuid) {
        if(attributes.containsKey(uuid)) return true;
        else return false;
    }

    public static String getCharacteristic(String uuid) {
        return attributes.get(uuid);
    }
}

