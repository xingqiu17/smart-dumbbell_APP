package com.example.dumb_app.core.util

import java.security.MessageDigest

/**
 * 密码工具：把盐值与明文拼接后做 MD5 → 32 位小写 hex 字符串
 *
 * 用法：
 *     val md5 = PasswordUtil.md5WithSalt("123456", "dumb")
 */
object PasswordUtil {

    fun md5WithSalt(password: String, salt: String): String {
        val input = salt + password
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
