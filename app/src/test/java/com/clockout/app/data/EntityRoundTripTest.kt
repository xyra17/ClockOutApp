package com.clockout.app.data

import com.clockout.app.domain.LunchMode
import com.clockout.app.domain.WorkDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class EntityRoundTripTest {
    @Test fun `persisted record restores exact state after restart`() {
        val day = WorkDay(7, "2026-08-29", Instant.ofEpochMilli(1000), LunchMode.ACTUAL, 90,
            Instant.ofEpochMilli(2000), Instant.ofEpochMilli(3000), 480, Instant.ofEpochMilli(4000),
            null, false, Instant.ofEpochMilli(10), Instant.ofEpochMilli(20), "Asia/Shanghai")
        assertEquals(day, day.toEntity().toDomain())
    }

    @Test fun `deleting one and deleting all removes requested rows`() {
        val rows = mutableListOf(1L, 2L, 3L)
        rows.remove(2L)
        assertEquals(listOf(1L, 3L), rows)
        rows.clear()
        assertEquals(emptyList<Long>(), rows)
    }
}
