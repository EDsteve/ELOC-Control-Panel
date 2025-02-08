package de.eloc.eloc_control_panel.data.helpers

import de.eloc.eloc_control_panel.BuildConfig
import de.eloc.eloc_control_panel.data.util.Preferences

enum class TrafficDirection {
    FromEloc,
    ToEloc
}

class Logger {
    companion object {
//        fun d(message: String) {
//            if (BuildConfig.DEBUG) {
//                val msg = message.trim()
//                if (msg.isEmpty()) {
//                    return
//                }
//                val decor = "-------------------------"
//                println("\n\n$decor ELOC APP LOG $decor")
//                println(msg)
//                println("$decor    END LOG   $decor\n\n")
//            }
//        }

        fun t(message: String, direction: TrafficDirection) {
            if (!Preferences.logBtTraffic) {
                return
            }

            val msg = message.trim()
            if (msg.isEmpty()) {
                return
            }
            val decor = "--------------------------------"
            when (direction) {
                TrafficDirection.ToEloc -> println("\n\n$decor ELOC APP SENDING BT MESSAGE $decor")
                TrafficDirection.FromEloc -> println("\n\n$decor ELOC APP RECEIVING BT MESSAGE $decor")
            }
            println(msg)
            println("$decor    END BT MESSAGE   $decor\n\n")
        }
    }
}