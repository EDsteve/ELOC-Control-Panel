package de.eloc.eloc_control_panel;

import de.eloc.eloc_control_panel.ng3.App;

class Constants {

    // values have to be globally unique

    static final String NOTIFICATION_CHANNEL = App.Companion.getApplicationId() + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = App.Companion.getApplicationId() + ".MainActivity";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    private Constants() {
    }
}
