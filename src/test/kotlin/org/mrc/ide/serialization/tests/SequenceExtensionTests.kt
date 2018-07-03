package org.mrc.ide.serialization.tests

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mrc.ide.serialization.headAndTail

class SequenceExtensionTests
{
    @Test
    fun `headAndTail returns head and tail`()
    {
        val sequence = sequenceOf(1, 2, 3, 4, 5)
        val (head, tail) = sequence.headAndTail()
        assertThat(head).isEqualTo(1)
        assertThat(tail.toList()).hasSameElementsAs(listOf(2, 3, 4, 5))
    }
}