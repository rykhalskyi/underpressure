package com.otakeessen.underpressure.util

/**
 * Global constants for the application.
 */
object Constants {
    /**
     * The time window (in minutes) around a slot time where measurements can be added.
     */
    const val SLOT_WINDOW_MINUTES = 15.0

    /**
     * The minimum difference (in minutes) required between adjacent timeslots.
     */
    const val MIN_SLOT_DIFFERENCE_MINUTES = 30.0
}
