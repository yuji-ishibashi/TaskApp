package jp.techacademy.yuji.ishibashi.taskapp

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import jp.techacademy.yuji.ishibashi.taskapp.databinding.ActivityInputBinding
import java.util.*

class InputActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG: String = "InputActivity"

    private lateinit var binding: ActivityInputBinding

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadSpinner()
        }
    }

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0

    private var mTaskId = -1
    private var mTask: Task? = null

    private lateinit var mCategoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate start")
        //setContentView(R.layout.activity_input)

        binding = ActivityInputBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)

        // ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        mCategoryAdapter = CategoryAdapter(this)

        // UI部品の設定
        binding.contentInput.dateButton.setOnClickListener(this)
        binding.contentInput.timesButton.setOnClickListener(this)
        binding.contentInput.categoryAddButton.setOnClickListener(this)
        binding.contentInput.doneButton.setOnClickListener(this)

        // EXTRA_TASKからTaskのidを取得して、 idからTaskのインスタンスを取得する
        val intent = intent
        if(mTaskId == -1) {
            mTaskId = intent.getIntExtra(EXTRA_TASK, -1)
        }
        mTask = mRealm.where(Task::class.java).equalTo("id", mTaskId).findFirst()

        if (mTask == null) {
            // 新規作成の場合
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)
        } else {
            // 更新の場合
            binding.contentInput.titleEditText.setText(mTask!!.title)
            binding.contentInput.contentEditText.setText(mTask!!.contents)

            val calendar = Calendar.getInstance()
            calendar.time = mTask!!.date
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

            val dateString = mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
            val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)

            binding.contentInput.dateButton.text = dateString
            binding.contentInput.timesButton.text = timeString
        }

        reloadSpinner()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy start")

        mRealm.close()
    }

    override fun onClick(v: View) {
        Log.d(TAG, "onClick start")
        when(v.id){
            R.id.date_button -> showDatePickerDialog()
            R.id.times_button -> showTimePickerDialog()
            R.id.category_add_button -> transitionEditCategory()
            R.id.done_button -> finishTaskInput()
        }
    }

    /**
     * Spinnerを更新する関数
     * タスクが設定されている場合はspinnerの選択箇所をタスクに設定されているものにします。
     */
    private fun reloadSpinner() {
        Log.d(TAG, "reloadSpinner start")
        // Realmデータベースから、「すべてのデータを取得してID順に並べた結果」を取得
        val categoryRealmResults: RealmResults<Category> = mRealm.where(Category::class.java).findAll().sort("id", Sort.ASCENDING)

        // 上記の結果を、CategoryListとしてセットする
        mCategoryAdapter.mCategoryList = mRealm.copyFromRealm(categoryRealmResults)

        // Categoryのspinner用のアダプタに渡す
        binding.contentInput.categorySpinner.adapter = mCategoryAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mCategoryAdapter.notifyDataSetChanged()

        //タスク編集の場合はspinnerの初期値をタスクに設定されている値に変更
        if(mTask != null) {
            Log.d(TAG, "category adapter setting")
            val results: Category? = mRealm.where(Category::class.java).equalTo("categoryName", mTask!!.category).findFirst()
            val position: Int = mCategoryAdapter.getPosition(results!!)
            binding.contentInput.categorySpinner.setSelection(position)
        }
    }

    /**
     * 日付を設定するダイアログを表示する関数
     */
    private fun showDatePickerDialog(){
        val datePickerDialog = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                Log.d(TAG, "OnDateClickListener start")
                mYear = year
                mMonth = month
                mDay = dayOfMonth
                val dateString = mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
                binding.contentInput.dateButton.text = dateString
            }, mYear, mMonth, mDay)
        datePickerDialog.show()
    }

    /**
     * 時刻を設定するダイアログを表示する関数
     */
    private fun showTimePickerDialog(){
        val timePickerDialog = TimePickerDialog(this,
            TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                Log.d(TAG, "OnTimeClickListener start")
                mHour = hour
                mMinute = minute
                val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)
                binding.contentInput.timesButton.text = timeString
            }, mHour, mMinute, false)
        timePickerDialog.show()
    }

    /**
     * カテゴリー編集画面に遷移する関数
     */
    private fun transitionEditCategory(){
        // 入力・編集する画面に遷移させる
        val intent = Intent(this, EditCategoryActivity::class.java)
        startActivity(intent)
    }

    /**
     * 入力されたタスク情報を保存し、画面を終了する関数
     * 入力情報に不足がある場合はその旨を通知して画面終了処理をキャンセルします。
     */
    private fun finishTaskInput(){
        Log.d(TAG, "finishTaskInput start")

        when {
            binding.contentInput.titleEditText.text.isNullOrEmpty() -> {
                Toast.makeText(this, "タスクのタイトルが未入力です。", Toast.LENGTH_LONG).show()
                return
            }
            binding.contentInput.contentEditText.text.isNullOrEmpty() -> {
                Toast.makeText(this, "タスクの内容が未入力です。", Toast.LENGTH_LONG).show()
                return
            }
            binding.contentInput.categorySpinner.selectedItem == null -> {
                Toast.makeText(this, "カテゴリーが未設定です。", Toast.LENGTH_LONG).show()
                return
            }

            else -> {
                addTask()
                finish()
            }
        }

    }

    /**
     * タスクを保存する関数
     */
    private fun addTask() {
        Log.d(TAG, "create task process")
        if (mTask == null) {
            // 新規作成の場合
            mTask = Task()

            val taskRealmResults = mRealm.where(Task::class.java).findAll()

            val identifier: Int =
                if (taskRealmResults.max("id") != null) {
                    taskRealmResults.max("id")!!.toInt() + 1
                } else {
                    0
                }
            mTask!!.id = identifier
        }

        val title = binding.contentInput.titleEditText.text.toString()
        val content = binding.contentInput.contentEditText.text.toString()
        val category: Category = binding.contentInput.categorySpinner.selectedItem as Category
        val categoryName = category.categoryName

        mTask!!.title = title
        mTask!!.contents = content
        val calendar = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
        val date = calendar.time
        mTask!!.date = date

        mTask!!.category = categoryName

        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(mTask!!)
        mRealm.commitTransaction()

        Log.d(TAG, "intent process")
        val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        resultIntent.putExtra(EXTRA_TASK, mTask!!.id)
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, resultPendingIntent)
    }
}

