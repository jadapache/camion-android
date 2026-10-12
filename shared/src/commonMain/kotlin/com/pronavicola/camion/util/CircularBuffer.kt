package com.pronavicola.camion.util

/**
 * Fixed-capacity thread-safe circular buffer for tracking recently published message IDs
 * to prevent duplicates (Idempotency Guard).
 */
class CircularBuffer<T>(val capacity: Int = 1000) {
    private val buffer = ArrayList<T>(capacity)
    private val set = HashSet<T>(capacity)
    private var head = 0
    private val lock = Any()

    fun add(element: T): Boolean = synchronized(lock) {
        if (buffer.size < capacity) {
            buffer.add(element)
            set.add(element)
        } else {
            val oldElement = buffer[head]
            set.remove(oldElement)
            buffer[head] = element
            set.add(element)
            head = (head + 1) % capacity
        }
        true
    }

    fun contains(element: T): Boolean = synchronized(lock) {
        set.contains(element)
    }

    val size: Int
        get() = synchronized(lock) { buffer.size }

    fun toList(): List<T> = synchronized(lock) {
        buffer.toList()
    }

    fun clear() = synchronized(lock) {
        buffer.clear()
        set.clear()
        head = 0
    }
}
