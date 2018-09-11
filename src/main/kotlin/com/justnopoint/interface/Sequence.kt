package com.justnopoint.`interface`

abstract class Sequence {
    abstract fun getName(): String
    abstract fun getFramecount(): Int
    abstract fun getFrames(): List<Frame>

    override fun toString(): String {
        return getName()
    }
}