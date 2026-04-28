package com.chaiok.pos.data.storage

interface SensitiveStorage {
    fun saveEncryptedCardToken(token: String)
    fun readEncryptedCardToken(): String?
}
