package com.example.roomdemo.db

class SubscriberRepository(private val dao: SubscriberDAO) {

    val subscribers = dao.getAllSubscribers()

    suspend fun insert(subscriber: Subscriber): Long {  // will return row id
        return dao.insertSubscriber(subscriber)
    }

    suspend fun update(subscriber: Subscriber): Int { // return number of row updated
        return dao.updateSubscriber(subscriber)
    }

    suspend fun delete(subscriber: Subscriber): Int { // return number of row deleted
        return dao.deleteSubscriber(subscriber)
    }

    suspend fun deleteAll(): Int { // return number of rows deleted
        return dao.deleteAll()
    }
}