package su.redbyte.cleaneroflegacy

import su.redbyte.cleaneroflegacy.annotation.RemoveAfter

@RemoveAfter("1.0.0")
interface KillBill {
    fun a(): String
    fun functor(): Boolean
}