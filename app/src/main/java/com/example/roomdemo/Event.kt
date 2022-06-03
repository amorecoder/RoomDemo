package com.example.roomdemo
/*
We use this Event wrapper class as a live communication channel for delivering messages
from View Model to the UI.
 */
class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set // disable write to this property

    fun getContentIfNotHandled(): T? {
        return if(hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}