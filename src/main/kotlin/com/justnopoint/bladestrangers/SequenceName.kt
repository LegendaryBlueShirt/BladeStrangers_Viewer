package com.justnopoint.bladestrangers

fun getSequenceName(label: Int): String {
    return when(label) {
        100 -> "Standing (Normal)"
        105 -> "Standing (Damaged)"
        110 -> "Stand to Crouch"
        120 -> "Turnaround (Standing)"
        200 -> "Crouching (Normal)"
        205 -> "Crouching (Damaged)"
        210 -> "Crouch to Stand"
        220 -> "Turnaround (Crouching)"
        300 -> "Walk (Forward)"
        310 -> "Walking (Forward)"
        400 -> "Walk (Backwards)"
        410 -> "Walking (Backwards)"
        420 -> "Walk Stop"
        500 -> "Run Start"
        510 -> "Running"
        520 -> "Run Stop"
        530 -> "Run Stopping"
        600 -> "Backdash"
        610 -> "Backdash Airborne"
        620 -> "Backdash Land"
        630 -> "Backdash Landing"
        700 -> "Jump Start"
        710 -> "Jumping"
        720 -> "Jump Apex"
        730 -> "Jump Fall"
        740 -> "Jump Falling"
        750 -> "Jump Land"
        1000 -> "Down to Grounded"
        1010 -> "Grounded"
        2000 -> "5L"
        2010 -> "6L"
        2020 -> "6LL"
        2200 -> "5H"
        2210 -> "6H (Start)"
        2211 -> "6H (Holding)"
        2250 -> "4H"
        2300 -> "2L"
        2310 -> "2H"
        2320 -> "3H"
        2400 -> "j.L"
        2410 -> "j.8L"
        2600 -> "j.H"
        2610 -> "j.8H"
        2700 -> "6E"
        2710 -> "2E"
        2720 -> "3E"
        2760 -> "5E"
        2770 -> "j.E"
        2780 -> "4E"
        2830 -> "Wakeup Attack"
        2900 -> "Throw Attempt"
        2910 -> "Throw Success"
        2920 -> "Throw Whiff"
        7100 -> "LE"
        7150 -> "8LE/j.LE"
        7200 -> "4LE"
        else -> label.toString()
    }
}