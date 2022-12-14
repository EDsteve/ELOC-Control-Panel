package de.eloc.eloc_control_panel.helpers;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

import de.eloc.eloc_control_panel.ng.App;
import de.eloc.eloc_control_panel.models.ElocDeviceInfo;
import de.eloc.eloc_control_panel.ng.interfaces.ElocDeviceInfoListCallback;

public class HttpHelper {
    public static void getElocDevicesAsync(ElocDeviceInfoListCallback callback) {
        // Volley will do request on background thread... no need for an executor.
        // But that also means remember to use the callback!
        String address = "http://128.199.206.198/ELOC/map/appmap.php";
        StringRequest request = new StringRequest(address, response -> {
            String rangerName = App.Companion.getInstance().getSharedPrefs().getString("rangerName", Helper.DEFAULT_RANGER_NAME);
            ArrayList<ElocDeviceInfo> deviceInfos = ElocDeviceInfo.parseForRanger(response, rangerName);
            if (callback != null) {
                callback.handler(deviceInfos);
            }
        }, error -> {
            // Return empty list on error
            if (callback != null) {
                callback.handler(new ArrayList<>());
            }
        });

        App.Companion.getInstance().getRequestQueue().add(request);
        App.Companion.getInstance().getRequestQueue().start();
    }
}
