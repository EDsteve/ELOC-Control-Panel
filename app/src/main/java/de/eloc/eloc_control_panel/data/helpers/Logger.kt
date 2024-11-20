package de.eloc.eloc_control_panel.data.helpers

import de.eloc.eloc_control_panel.BuildConfig

class Logger {
    companion object {
        fun d(message: String) {
            if (BuildConfig.DEBUG) {
                val msg = message.trim()
                if (msg.isEmpty()) {
                    return
                }
                val decor = "-------------------------"
                println("\n\n$decor ELOC APP LOG $decor")
                println(msg)
                println("$decor    END LOG   $decor\n\n")
            }
        }
    }
}