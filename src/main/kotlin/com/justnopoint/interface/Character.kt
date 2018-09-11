package com.justnopoint.`interface`

abstract class Character {
    abstract fun getFullName(): String

    override fun toString(): String {
        return getFullName()
    }
}