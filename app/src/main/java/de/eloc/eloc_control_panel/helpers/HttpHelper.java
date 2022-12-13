package de.eloc.eloc_control_panel.helpers;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

import de.eloc.eloc_control_panel.ng.App;
import de.eloc.eloc_control_panel.models.ElocDeviceInfo;

public class HttpHelper {
    public static ArrayList<ElocDeviceInfo> getElocDevicesAsync(  ) {
        // Volley will do request on background thread... no need for an executor.
        // But that also means remember to use the callback!
        ArrayList<ElocDeviceInfo> deviceInfos = new ArrayList<>();
        String address = "http://128.199.206.198/ELOC/map/appmap.php";
        StringRequest request = new StringRequest(address, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                String rangerName = App.Companion.getInstance().getSharedPrefs().getString("rangerName", Helper.DEFAULT_RANGER_NAME);
                ElocDeviceInfo.parseForRanger(response, rangerName);
                int t =45;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        App.Companion.getInstance().getRequestQueue().add(request);
        App.Companion.getInstance().getRequestQueue().start();
        return deviceInfos;
    }
}
