# RoomDemo
Android project to demo usage of ROOM database.
The following are the basic general steps to setup a working support for ROOM database.


1. Module level build.gradle file:
a. add the KAPT plugins:
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
}

b. add dataBinding in the android section:
android {

    ...

    buildFeatures {
        dataBinding true
    }
}

c. add the following dependencies:
dependencies {
    ...

    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.4.1'
    implementation 'androidx.lifecycle:lifecycle-extensions:2.2.0'
    implementation 'androidx.room:room-runtime:2.4.2'
    implementation 'androidx.room:room-ktx:2.4.2'
    kapt 'androidx.room:room-compiler:2.4.2'

    ...
}


2. Create the abstract ROOM Database class:
@Database(entities = [Subscriber::class], version = 1)
abstract class SubscriberDatabase : RoomDatabase() {

    abstract val subscriberDAO: SubscriberDAO

    companion object {
        @Volatile
        private var INSTANCE : SubscriberDatabase? = null
            fun getInstance(context: Context): SubscriberDatabase {
                synchronized(this) {
                    var instance = INSTANCE
                        if(instance == null) {
                            instance = Room.databaseBuilder(
                                context.applicationContext,
                                SubscriberDatabase::class.java,
                                "subscriber_data_database"
                            ).build()
                        }
                    return instance
                }
            }
    }
}


3. Create the entity class:
Entity(tableName = "subscriber_data_table")
data class Subscriber(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="subscriber_id")
    val id: Int,

    @ColumnInfo(name="subscriber_name")
    val name: String,

    @ColumnInfo(name="subscriber_email")
    val email: String
)


4. Create the corresponding DAO class for the entity:
@Dao
interface SubscriberDAO {
    @Insert
    suspend fun insertSubscriber(subscriber: Subscriber): Long

    @Update
    suspend fun updateSubscriber(subscriber: Subscriber)

    @Delete
    suspend fun deleteSubscriber(subscriber: Subscriber)

    @Query("DELETE FROM subscriber_data_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM subscriber_data_table")
    fun getAllSubscribers(): LiveData<List<Subscriber>>  // no need for suspend fun since not going to run in background.
}


5. Create the corresponding Repository class for the DAO :
class SubscriberRepository(private val dao: SubscriberDAO) {

    val subscribers = dao.getAllSubscribers()

    suspend fun insert(subscriber: Subscriber) {
        dao.insertSubscriber(subscriber)
    }

    suspend fun update(subscriber: Subscriber) {
        dao.updateSubscriber(subscriber)
    }

    suspend fun delete(subscriber: Subscriber) {
        dao.deleteSubscriber(subscriber)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}


6. Create corresponding View Model Factory since our View Model needs to initialize a repository upon creation:
class SubscriberViewModelFactory(private val repository: SubscriberRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(SubscriberViewModel::class.java)) {
            return SubscriberViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown View Model class")
    }

}


7. Create the View Model for the Repository in association with the View Model Factory :
class SubscriberViewModel(private val repository: SubscriberRepository) : ViewModel(), Observable {

    val subscribers = repository.subscribers

    val inputName = MutableLiveData<String?>()
    val inputEmail = MutableLiveData<String?>()
    val saveOrUpdateButtonText = MutableLiveData<String>()
    val clearAllOrDeleteButtonText = MutableLiveData<String>()

    init {
        saveOrUpdateButtonText.value = "Save"
        clearAllOrDeleteButtonText.value = "Clear All"
    }

    fun saveOrUpdate() {
        val name = inputName.value!!
        val email = inputEmail.value!!
        insert(Subscriber(0,name,email))
        inputName.value = null
        inputEmail.value = null
    }

    fun clearAllOrDelete() {
        deleteAll()
    }

    fun insert(subscriber: Subscriber) =  viewModelScope.launch { repository.insert(subscriber) }
    fun update(subscriber: Subscriber) =  viewModelScope.launch { repository.update(subscriber) }
    fun delete(subscriber: Subscriber) =  viewModelScope.launch { repository.delete(subscriber) }
    fun deleteAll() =  viewModelScope.launch { repository.deleteAll() }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

}


8. Setup activity_main.xml layout to support Data Binding with View Model :
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <variable
            name="myViewModel"
            type="com.example.roomdemo.SubscriberViewModel" />
    </data>
<LinearLayout
    ...
    tools:context=".MainActivity" >

    <EditText
        android:id="@+id/name_text"
	...
        android:text="@={myViewModel.inputName}"
	...
        android:textStyle="bold" />

    <EditText
        android:id="@+id/email_text"
	...
        android:text="@={myViewModel.inputEmail}"
	...
        android:textStyle="bold" />

    <LinearLayout
	...
        android:orientation="horizontal">

        <Button
            android:id="@+id/save_or_update_button"
	    ...
            android:text="@={myViewModel.saveOrUpdateButtonText}"
            android:onClick="@{()->myViewModel.saveOrUpdate()}"
	    ...
            android:textStyle="bold" />

        <Button
            android:id="@+id/clear_all_or_delete_button"
	    ...
            android:text="@={myViewModel.clearAllOrDeleteButtonText}"
            android:onClick="@{()->myViewModel.clearAllOrDelete()}"
	    ...
            android:textStyle="bold" />

    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
	...
        android:id="@+id/subscriber_recyclerView"
        android:layout_margin="5dp"/>

</LinearLayout>
</layout>



9. Code the MainActivity class according to functionalities:
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var subscriberViewModel: SubscriberViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val dao = SubscriberDatabase.getInstance(application).subscriberDAO
        val repository = SubscriberRepository(dao)
        val factory = SubscriberViewModelFactory(repository)

        subscriberViewModel = ViewModelProvider(this, factory)[SubscriberViewModel::class.java]
        binding.myViewModel = subscriberViewModel
        binding.lifecycleOwner = this

        displaySubscribersList()
    }

    private fun displaySubscribersList() {
        subscriberViewModel.subscribers.observe(this) {
            Log.i("MyTag", it.toString())
        }
    }
}



10. The following is to setup the List Item view :
i. Create a new layout for list item for the List Item View.
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    
<LinearLayout
    ...
    android:orientation="vertical">

    <androidx.cardview.widget.CardView
        android:id="@+id/card_view"
	...
        app:cardElevation="10dp" >

        <LinearLayout
            android:id="@+id/list_item_layout"
	    ...
            android:orientation="vertical">

            <TextView
                android:id="@+id/name_text_view"
		...
                android:textStyle="bold" />
            <TextView
                android:id="@+id/email_text_view"
		...
                android:textStyle="bold" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>
</LinearLayout>
</layout>


ii. Create a new Recycler View Adapter class :
class MyRecyclerViewAdapter(private val subscibers: List<Subscriber>) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ListItemBinding>(layoutInflater, R.layout.list_item, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(subscibers[position])
    }

    override fun getItemCount(): Int {
        return subscibers.size
    }
}

class MyViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(subscriber: Subscriber) {
        binding.nameTextView.text = subscriber.name
        binding.emailTextView.text = subscriber.email
    }
}


iii. Update the Main Activity class to show the List Item View:
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var subscriberViewModel: SubscriberViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
	
	...

        binding.lifecycleOwner = this

        initRecyclerView()
    }

    private fun initRecyclerView() {
        binding.subscriberRecyclerView.layoutManager = LinearLayoutManager(this)
        displaySubscribersList()
    }

    private fun displaySubscribersList() {
        subscriberViewModel.subscribers.observe(this) {
            Log.i("MyTag", it.toString())
            binding.subscriberRecyclerView.adapter = MyRecyclerViewAdapter(it)
        }
    }
}

