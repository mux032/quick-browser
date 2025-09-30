package com.quick.browser.domain.service

interface IEncryptedPreferencesService {
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}