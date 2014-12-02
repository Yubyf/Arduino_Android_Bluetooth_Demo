package com.flounder.ConnBT;

public class Protocol {

    public static final byte[] REQ_SET_HUMIDITY = {0x57, 0x55, 0x02};
    public static final byte[] REQ_GET_HUMIDITY = {0x57, 0x55, 0x01, (byte) 0xFF};
    public static final byte[] REQ_START_WATERING = {0x57, 0x55, 0x03, (byte) 0xFF};
    public static final byte[] REQ_STOP_WATERING = {0x57, 0x55, 0x04, (byte) 0xFF};
    public static final byte[] RES_SET_HUMIDITY = {0x53, 0x53, 0x02, (byte) 0xFF};
    public static final byte[] RES_START_WATERING = {0x53, 0x53, 0x03, (byte) 0xFF};
    public static final byte[] RES_STOP_WATERING = {0x53, 0x53, 0x04, (byte) 0xFF};

    private static final String GET_HUMIDITY = "535301"; // hex

    public static String getHumidity(String response) {
        String ret = "";
        if (response != null && response.length() >= GET_HUMIDITY.length() + 12) {
            String curr = response.substring(GET_HUMIDITY.length() + 2, GET_HUMIDITY.length() + 4);
            String high = response.substring(GET_HUMIDITY.length() + 6, GET_HUMIDITY.length() + 8);
            String low = response.substring(GET_HUMIDITY.length() + 10, GET_HUMIDITY.length() + 12);
            ret = Integer.parseInt(curr, 16) + "/" + Integer.parseInt(high, 16) + "/" + Integer.parseInt(low, 16);
        }
        return ret;
    }
}
