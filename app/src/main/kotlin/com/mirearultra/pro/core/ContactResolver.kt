package com.mirearultra.pro.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** Gerçek rehber ve arama geçmişinden en sık aranan kişileri çözer. */
object ContactResolver {

    private val cache = HashMap<String, String?>()

    fun lookupName(context: Context, number: String): String? {
        if (number.isBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
        cache[number]?.let { return it }
        val name = runCatching {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        if (name != null) cache[number] = name
        return name
    }

    /**
     * Kullanıcının telefonundaki en çok aranan gerçek kişileri çeker.
     * Asla sahte veya örnek kişi eklemez.
     */
    fun getFrequentContacts(context: Context): List<SpeedDialContact> {
        val list = mutableListOf<SpeedDialContact>()
        val seenNumbers = mutableSetOf<String>()

        // 1. Rehberdeki en çok aranan veya favori kişiler (READ_CONTACTS)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                    ContactsContract.CommonDataKinds.Phone.STARRED
                )
                val sortOrder = "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC, " +
                        "${ContactsContract.CommonDataKinds.Phone.STARRED} DESC, " +
                        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 20"

                context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val timesIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)
                    val starredIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx)?.trim().orEmpty()
                        val rawNum = cursor.getString(numIdx)?.trim().orEmpty()
                        val normalized = rawNum.replace("[^0-9+]".toRegex(), "")
                        if (name.isNotBlank() && normalized.isNotBlank() && seenNumbers.add(normalized)) {
                            val times = if (timesIdx >= 0) cursor.getInt(timesIdx) else 0
                            val isStarred = if (starredIdx >= 0) cursor.getInt(starredIdx) == 1 else false
                            val role = when {
                                isStarred -> "★ Favori"
                                times > 0 -> "$times arama"
                                else -> "Rehber"
                            }
                            list.add(SpeedDialContact(name = name, phone = rawNum, roleLabel = role))
                        }
                    }
                }
            }
        }

        // 2. Arama geçmişinden sık arananlar (READ_CALL_LOG)
        if (list.size < 8 && ContextCompat.checkSelfPermission(context, "android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                val callUri = CallLog.Calls.CONTENT_URI
                val callProj = arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME
                )
                val callSort = "${CallLog.Calls.DATE} DESC LIMIT 40"
                context.contentResolver.query(callUri, callProj, null, null, callSort)?.use { c ->
                    val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    while (c.moveToNext()) {
                        val rawNum = c.getString(numIdx)?.trim().orEmpty()
                        val cachedName = c.getString(nameIdx)?.trim().orEmpty()
                        val normalized = rawNum.replace("[^0-9+]".toRegex(), "")
                        if (normalized.isNotBlank() && seenNumbers.add(normalized)) {
                            val displayName = cachedName.ifBlank { lookupName(context, rawNum) ?: rawNum }
                            list.add(SpeedDialContact(name = displayName, phone = rawNum, roleLabel = "Son Görüşme"))
                        }
                    }
                }
            }
        }

        return list
    }
}
