package de.eloc.eloc_control_panel.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ElocDeviceInfo {
    public final String plusCode;
    public final String name;
    public final double batteryVolts;
    public final String ranger;
    public final String time;
    public final double recTime;
    public final double accuracy;

    public ElocDeviceInfo(String plusCode, String name, double batteryVolts, String ranger, String time, double recTime, double accuracy) {
        this.plusCode = plusCode;
        this.name = name;
        this.batteryVolts = batteryVolts;
        this.ranger = ranger;
        this.time = time;
        this.recTime = recTime;
        this.accuracy = accuracy;
    }

    public static ArrayList<ElocDeviceInfo> parseForRanger(String json, String rangerName) {
        ArrayList<ElocDeviceInfo> deviceInfos = new ArrayList<>();

        try {
            JSONObject obj = new JSONObject(json);
            JSONArray deviceArray = obj.getJSONArray("device_infos");
            for (int i = 0; i < deviceArray.length(); i++) {
                JSONObject jsonInfo = deviceArray.getJSONObject(i);
                String ranger = jsonInfo.getString("ranger_name");
                if (!ranger.equals(rangerName)) {
                    // Only get devices for ranger with matched name.
                    continue;
                }

                String plusCode = jsonInfo.getString("plus_code");
                String deviceName = jsonInfo.getString("device_name");

                String time = jsonInfo.getString("time");
                double batteryVolts = 0.0;
                try {
                    String tmp = jsonInfo.getString("battery_volts");
                    batteryVolts = Double.parseDouble(tmp);
                } catch (NumberFormatException ignore) {

                }
                double recTime = 0.0;
                try {
                    String tmp = jsonInfo.getString("record_time_since_boot");
                    recTime = Double.parseDouble(tmp);
                } catch (NumberFormatException ignore) {

                }

                double accuracy = 0.0;
                try {
                    String tmp = jsonInfo.getString("gps_accuracy");
                    accuracy = Double.parseDouble(tmp);
                } catch (NumberFormatException ignore) {

                }

                ElocDeviceInfo deviceInfo = new ElocDeviceInfo(plusCode, deviceName, batteryVolts, ranger, time, recTime, accuracy);
                deviceInfos.add(deviceInfo);
            }
            Log.d("TAG", "parse: " + obj.getClass().getName());
        } catch (JSONException ignore) {
            ignore.printStackTrace();
        }

        return deviceInfos;
    }
}
