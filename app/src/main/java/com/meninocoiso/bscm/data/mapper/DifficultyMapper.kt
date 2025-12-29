package com.meninocoiso.bscm.data.mapper

import com.meninocoiso.bscm.domain.enums.Difficulty
import javax.inject.Inject

/**
 * Maps external difficulty values to domain Difficulty enum
 */
class DifficultyMapper @Inject constructor() {

    fun map(value: Int?): Difficulty {
        return when (value) {
            3 -> Difficulty.HARD
            1 -> Difficulty.EXTREME
            else -> Difficulty.NORMAL
        }
    }
}

