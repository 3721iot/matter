package com.dsh.matter.model.device

enum class FanMode(val mode: Int) {
    /**
     * Off will turn off the fan and set speed and percentage to 0
     */
    Off(0),

    /**
     * Low will set the fan to manufacture's idea of taking things slow
     */
    Low(1),

    /**
     * Medium will set the fan to manufacture's choice of medium speed
     */
    Medium(2),

    /**
     * You get the gist of it
     */
    High(3),

    /**
     * On will set the device to High mode
     */
    On(4),

    /**
     * Auto will automatically set mode according to manufacturer's algorithm
     */
    Auto(5)
}