package com.chaiok.pos.data.storage

interface SensitiveStorage {
    fun saveCardToken(token: String): Result<Unit>
    fun readCardToken(): Result<String?>
}
