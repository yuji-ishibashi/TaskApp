package jp.techacademy.yuji.ishibashi.taskapp

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable

open class Category : RealmObject(), Serializable {
    var categoryName: String = ""

    @PrimaryKey
    var id: Int = 0
}