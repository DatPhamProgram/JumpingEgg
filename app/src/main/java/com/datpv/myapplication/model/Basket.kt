package com.datpv.myapplication.model

data class Basket(
    val x: Float,        // world X (dp)
    val y: Float,        // world Y (dp)
    val dir: Float,      // +1 / -1
    val speed: Float     // dp per frame
)
