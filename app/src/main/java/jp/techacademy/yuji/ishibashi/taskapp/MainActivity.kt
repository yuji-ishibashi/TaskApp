package jp.techacademy.yuji.ishibashi.taskapp

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import jp.techacademy.yuji.ishibashi.taskapp.databinding.ActivityMainBinding

const val EXTRA_TASK = "jp.techacademy.yuji.ishibashi.taskapp.TASK"

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter

    private lateinit var mFilterCategory: Category

    private lateinit var mAllCategory: Category

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)

        // ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }

        mAllCategory = Category()
        mAllCategory.id = -1
        mAllCategory.categoryName = "すべて"

        mFilterCategory = mAllCategory

        binding.fab.setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this)

        // ListViewをタップしたときの処理
        binding.listView1.setOnItemClickListener(this)

        // ListViewを長押ししたときの処理
        binding.listView1.setOnItemLongClickListener(this)

        reloadListView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // レイアウトファイルのインフレート
        menuInflater.inflate(R.menu.menu_main, menu)
        // onCreateOptionsMenu()のオーバーライド時は、常にtrueを返却
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var returnVal: Boolean = true

        when(item.itemId) {
            R.id.action_filter ->
                filterCategory()
            else ->
                // オプションメニューのItem以外が選択された場合は、
                // 親クラス(super)のonOptionsItemSelected(item:)メソッドの
                // 返り値(デフォルトではfalse)を返却
                returnVal = super.onOptionsItemSelected(item)
        }

        return returnVal
    }

    private fun filterCategory(){
        var categoryAdapter = CategoryAdapter(this)

        val categoryRealmResults: RealmResults<Category> = mRealm.where(Category::class.java).findAll().sort("id", Sort.ASCENDING)
        categoryAdapter.mCategoryList = mRealm.copyFromRealm(categoryRealmResults)

        //フィルター解除用の選択肢を追加
        var all: Category
        //先頭にセット
        categoryAdapter.mCategoryList.add(0, mAllCategory)

        AlertDialog.Builder(this)
            .setSingleChoiceItems(categoryAdapter, 0) { dialog, which ->
                mFilterCategory = categoryAdapter.getItem(which) as Category
                reloadListView()
                dialog.dismiss()
            }
            .show()

    }

    private fun reloadListView() {
        // Realmデータベースから、「すべてのデータを取得して新しい日時順に並べた結果」を取得
        var taskRealmResults: RealmResults<Task>? = null
        if(mFilterCategory == mAllCategory) {
            taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
        } else {
            taskRealmResults = mRealm.where(Task::class.java).equalTo("category",mFilterCategory.categoryName).findAll().sort("date", Sort.DESCENDING)
        }

        // 上記の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        binding.listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // 入力・編集する画面に遷移させる
        val task = parent?.adapter?.getItem(position) as Task
        val intent = Intent(this, InputActivity::class.java)
        intent.putExtra(EXTRA_TASK, task.id)
        startActivity(intent)
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        // タスクを削除する
        val task = parent?.adapter?.getItem(position) as Task

        // ダイアログを表示する
        val builder = AlertDialog.Builder(this)

        builder.setTitle("削除")
        builder.setMessage(task.title + "を削除しますか")

        builder.setPositiveButton("OK"){_, _ ->
            val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

            mRealm.beginTransaction()
            results.deleteAllFromRealm()
            mRealm.commitTransaction()

            val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
            val resultPendingIntent = PendingIntent.getBroadcast(
                this,
                task.id,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(resultPendingIntent)

            reloadListView()
        }

        builder.setNegativeButton("CANCEL", null)

        val dialog = builder.create()
        dialog.show()

        return true
    }
}