package org.mrc.ide.serialization

// Pulls the head (first element) off the sequence and returns that
// and the remaining sequence elements.
fun <T> Sequence<T>.headAndTail(): Pair<T?, Sequence<T>>
{
    val iterator = this.iterator()
    val head = if (iterator.hasNext()) iterator.next() else null
    val tail = iterator.asSequence()
    return Pair(head, tail)
}