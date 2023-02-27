package de.eloc.eloc_control_panel.ng.interfaces;

import java.util.ArrayList;

import de.eloc.eloc_control_panel.models.ElocDeviceInfo;

public interface ElocDeviceInfoListCallback {
    void handler(ArrayList<ElocDeviceInfo> list);
}
