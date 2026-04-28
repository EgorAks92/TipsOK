package com.chaiok.pos.data.repository

import com.chaiok.pos.domain.model.TipRecord
import com.chaiok.pos.domain.repository.TipsRepository
import java.time.LocalDateTime

class MockTipsRepository : TipsRepository {
    override suspend fun getTips(): Result<List<TipRecord>> {
        // TODO: Replace with backend tips history API.
        val now = LocalDateTime.now()
        val items = listOf(
            TipRecord("1", now.minusHours(1), 1450.0, 10, 145.0),
            TipRecord("2", now.minusHours(3), 2100.0, 12, 252.0),
            TipRecord("3", now.minusDays(1), 1700.0, 8, 136.0),
            TipRecord("4", now.minusDays(1).minusHours(4), 2900.0, 10, 290.0),
            TipRecord("5", now.minusDays(2), 3200.0, 15, 480.0),
            TipRecord("6", now.minusDays(3), 980.0, 7, 68.6),
            TipRecord("7", now.minusDays(4), 4100.0, 10, 410.0),
            TipRecord("8", now.minusDays(5), 2500.0, 9, 225.0)
        )
        return Result.success(items)
    }
}
