package su.redbyte.cleaneroflegacy.ui

import android.util.Log
import su.redbyte.cleaneroflegacy.annotation.RemoveAfter

object MuStub {

    @RemoveAfter("1.0.0")
    fun r() {
        Log.d("TAG", "b: ")
    }

    fun a(): Int = 10

    @RemoveAfter("1.0.3")
    fun b() {
        Log.d("TAG", "b: ")
    }

}