package org.claudroide.app.feature.linux.remote.ssh

import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface KnownHostsStore {
    fun isTrusted(host: String, port: Int, fingerprint: String): Boolean
    fun trust(host: String, port: Int, fingerprint: String)
    fun revoke(host: String, port: Int)
    fun getFingerprint(host: String, port: Int): String?
}

class InMemoryKnownHostsStore : KnownHostsStore {
    private val store = ConcurrentHashMap<String, String>()

    private fun key(host: String, port: Int): String = "$host:$port"

    override fun isTrusted(host: String, port: Int, fingerprint: String): Boolean {
        val known = store[key(host, port)] ?: return false
        return known == fingerprint
    }

    override fun trust(host: String, port: Int, fingerprint: String) {
        store[key(host, port)] = fingerprint
    }

    override fun revoke(host: String, port: Int) {
        store.remove(key(host, port))
    }

    override fun getFingerprint(host: String, port: Int): String? {
        return store[key(host, port)]
    }
}

class FileKnownHostsStore(private val file: File) : KnownHostsStore {
    private val memoryStore = ConcurrentHashMap<String, String>()

    init {
        load()
    }

    private fun key(host: String, port: Int): String = "$host:$port"

    private fun load() {
        if (!file.exists()) return
        try {
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        memoryStore[parts[0]] = parts[1]
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun persist() {
        try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val content = buildString {
                appendLine("# MLL Known Hosts Store")
                memoryStore.forEach { (hostPort, fp) ->
                    appendLine("$hostPort $fp")
                }
            }
            file.writeText(content)
        } catch (_: Throwable) {
        }
    }

    override fun isTrusted(host: String, port: Int, fingerprint: String): Boolean {
        val known = memoryStore[key(host, port)] ?: return false
        return known == fingerprint
    }

    override fun trust(host: String, port: Int, fingerprint: String) {
        memoryStore[key(host, port)] = fingerprint
        persist()
    }

    override fun revoke(host: String, port: Int) {
        memoryStore.remove(key(host, port))
        persist()
    }

    override fun getFingerprint(host: String, port: Int): String? {
        return memoryStore[key(host, port)]
    }
}
