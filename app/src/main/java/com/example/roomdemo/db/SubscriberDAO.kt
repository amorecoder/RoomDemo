package com.example.roomdemo.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SubscriberDAO {
    @Insert
    suspend fun insertSubscriber(subscriber: Subscriber): Long  //will return row id

    @Update
    suspend fun updateSubscriber(subscriber: Subscriber): Int // indicate number of row updated

    @Delete
    suspend fun deleteSubscriber(subscriber: Subscriber): Int // number of row deleted

    @Query("DELETE FROM subscriber_data_table")
    suspend fun deleteAll() : Int  // return number of rows deleted.

    @Query("SELECT * FROM subscriber_data_table")
    fun getAllSubscribers(): LiveData<List<Subscriber>>  // no need for suspend fun since not going to run in background.
}