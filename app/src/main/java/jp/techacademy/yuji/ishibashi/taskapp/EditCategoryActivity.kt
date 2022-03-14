package jp.techacademy.yuji.ishibashi.taskapp

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import jp.techacademy.yuji.ishibashi.taskapp.databinding.ActivityEditCategoryBinding
import jp.techacademy.yuji.ishibashi.taskapp.databinding.ActivityMainBinding
import java.util.*

class EditCategoryActivity : AppCompatActivity(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private val TAG: String = "EditCategoryActivity"

    private lateinit var binding: ActivityEditCategoryBinding

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private lateinit var mCategoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate start")
        //setContentView(R.layout.activity_edit_category)
        binding = ActivityEditCategoryBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)

        // ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        binding.fabCategory.setOnClickListener { view ->
            val category = Category()
            category.categoryName = ""
            showEditCategoryDialog(category,true)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mCategoryAdapter = CategoryAdapter(this)

        // ListViewをタップしたときの処理
        binding.listViewCategory.setOnItemClickListener(this)

        // ListViewを長押ししたときの処理
        binding.listViewCategory.setOnItemLongClickListener(this)

        reloadListView()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy start")

        mRealm.close()
    }

    /**
     * カテゴリー名を選択した場合の関数
     * カテゴリー名を編集します。
     */
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // 編集する
        val category = parent?.adapter?.getItem(position) as Category
        showEditCategoryDialog(category, false)
    }

    /**
     * カテゴリー名を長押しした場合の関数
     * カテゴリーを削除します。
     * タスクに利用中のカテゴリーは削除できません。
     */
    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        // カテゴリーを削除する
        val category = parent?.adapter?.getItem(position) as Category

        // ダイアログを表示する
        val builder = AlertDialog.Builder(this)

        builder.setTitle("削除")
        builder.setMessage(category.categoryName + "を削除しますか")

        builder.setPositiveButton("OK"){_, _ ->
            val task = mRealm.where(Task::class.java).equalTo("category", category.categoryName).findAll()
            if(task.isNullOrEmpty()) {
                val results = mRealm.where(Category::class.java).equalTo("id", category.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                reloadListView()
            } else {
                Toast.makeText(this, "使用中のタスクがあるため削除できません。", Toast.LENGTH_LONG).show()
            }
        }

        builder.setNegativeButton("CANCEL", null)

        val dialog = builder.create()
        dialog.show()

        return true
    }

    private fun reloadListView() {
        // Realmデータベースから、「すべてのデータを取得してID順に並べた結果」を取得
        val categoryRealmResults: RealmResults<Category> = mRealm.where(Category::class.java).findAll().sort("id", Sort.ASCENDING)

        // 上記の結果を、CategoryListとしてセットする
        mCategoryAdapter.mCategoryList = mRealm.copyFromRealm(categoryRealmResults)

        // CategoryのListView用のアダプタに渡す
        binding.listViewCategory.adapter = mCategoryAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mCategoryAdapter.notifyDataSetChanged()
    }

    /**
     * カテゴリーを作成・編集するダイアログを表示する関数
     * カテゴリー名が未入力、もしくは既に存在するカテゴリー名の場合はその旨を通知して処理を行わずに終了します。
     */
    private fun showEditCategoryDialog(category: Category, isCreate: Boolean) {
        val editText = AppCompatEditText(this)
        editText.setText(category.categoryName)
        AlertDialog.Builder(this)
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                if(editText.text.isNullOrEmpty()) {
                    Toast.makeText(this, "カテゴリー名が未入力です。", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val exist = mRealm.where(Category::class.java).equalTo("categoryName", editText.text.toString()).findFirst()
                if(exist != null) {
                    Toast.makeText(this, "すでに存在しているカテゴリーです。", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                category.categoryName = editText.text.toString()
                addCategory(category, isCreate)
                reloadListView()
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    /**
     * カテゴリーを保存する関数
     */
    private fun addCategory(category: Category, isCreate: Boolean) {
        Log.d(TAG, "addCategory start")

        if (isCreate) {
            // 新規作成の場合
            val categoryRealmResults = mRealm.where(Category::class.java).findAll()
            val identifier: Int =
                if (categoryRealmResults.max("id") != null) {
                    categoryRealmResults.max("id")!!.toInt() + 1
                } else {
                    0
                }
            category!!.id = identifier
        }

        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(category!!)
        mRealm.commitTransaction()
    }
}