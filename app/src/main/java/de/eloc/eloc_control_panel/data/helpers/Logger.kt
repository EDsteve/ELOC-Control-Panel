package de.eloc.eloc_control_panel.data.helpers

class Logger {
    companion object {
        fun d(message: String) {
           val msg = message.trim()
            if (msg .isEmpty()) {
                return
            }
            val decor = "-------------------------"
            print("$decor ELOC APP LOG $decor")
            print(msg)
            print("$decor    END LOG   $decor")
        }
    }
}