package org.jellyfin.mobile.player.mpv

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.jdtech.mpv.MPVLib
import dev.jdtech.mpv.MPVLib.EventObserver
fun createMPVFuture(@MPVLib.Event eventId: Int, cmd: Array<String?>): ListenableFuture<Void?> {
    val future = SettableFuture.create<Void?>()
    val eventObserver: EventObserver = generateObserver(eventId, future)
    MPVLib.addObserver(eventObserver)
    MPVLib.command(cmd)
    return future
}

fun createMPVFuture(propertyName: String, targetValue: Any): ListenableFuture<Void?> {
    val future = SettableFuture.create<Void?>()
    val eventObserver: EventObserver? = generateObserver(propertyName, targetValue, future)
    MPVLib.addObserver(eventObserver)
    when (targetValue) {
        is Boolean -> MPVLib.setPropertyBoolean(propertyName, targetValue)
        is Long -> MPVLib.setPropertyInt(propertyName, Math.toIntExact(targetValue))
        is Double -> MPVLib.setPropertyDouble(propertyName, targetValue)
        is String -> MPVLib.setPropertyString(propertyName, targetValue)
        else -> throw IllegalArgumentException("Unsupported type")
    }
    return future
}

private fun generateObserver(@MPVLib.Event targebtEventId: Int, future: SettableFuture<Void?>): EventObserver {
    return object : EventObserver {
        override fun event(eventId: Int) {
            if (eventId == targebtEventId) {
                    MPVLib.removeObserver(this)
                    future.set(null)
            }
        }

        override fun eventProperty(property: String) {
        }

        override fun eventProperty(property: String, value: Long) {
        }

        override fun eventProperty(property: String, value: Double) {
        }

        override fun eventProperty(property: String, value: Boolean) {
        }

        override fun eventProperty(property: String, value: String) {
        }
    }
}


private fun generateObserver(propertyName: String?, targetValue: Any?, future: SettableFuture<Void?>): EventObserver? {
    var observer: EventObserver? = null
    if (targetValue is String) {
        observer = object : EventObserver {
            override fun event(eventId: Int) {
            }

            override fun eventProperty(property: String) {
            }

            override fun eventProperty(property: String, value: Long) {
            }

            override fun eventProperty(property: String, value: Double) {
            }

            override fun eventProperty(property: String, value: Boolean) {
            }

            override fun eventProperty(property: String, value: String) {
                if (property == propertyName && value == targetValue) {
                        MPVLib.removeObserver(this)
                        future.set(null)
                }
            }
        }
    }

    if (targetValue is Long) {
        observer = object : EventObserver {
            override fun event(eventId: Int) {
            }

            override fun eventProperty(property: String) {
            }

            override fun eventProperty(property: String, value: Long) {
                if (property == propertyName && targetValue == value) {
                        MPVLib.removeObserver(this)
                        future.set(null)
                }
            }

            override fun eventProperty(property: String, value: Double) {
            }

            override fun eventProperty(property: String, value: Boolean) {
            }

            override fun eventProperty(property: String, value: String) {
            }
        }
    }


    if (targetValue is Double) {
        observer = object : EventObserver {
            override fun event(eventId: Int) {
            }

            override fun eventProperty(property: String) {
            }

            override fun eventProperty(property: String, value: Long) {
            }

            override fun eventProperty(property: String, value: Double) {
                if (property == propertyName && targetValue.equals(value)) {
                        MPVLib.removeObserver(this)
                        future.set(null)
                }
            }

            override fun eventProperty(property: String, value: Boolean) {
            }

            override fun eventProperty(property: String, value: String) {
            }
        }
    }


    if (targetValue is Boolean) {
        observer = object : EventObserver {
            override fun event(eventId: Int) {
            }

            override fun eventProperty(property: String) {
            }

            override fun eventProperty(property: String, value: Long) {
            }

            override fun eventProperty(property: String, value: Double) {
            }

            override fun eventProperty(property: String, value: Boolean) {
                if (property == propertyName && targetValue == value) {
                        MPVLib.removeObserver(this)
                        future.set(null)
                }
            }

            override fun eventProperty(property: String, value: String) {
            }
        }
    }

    return observer
}
