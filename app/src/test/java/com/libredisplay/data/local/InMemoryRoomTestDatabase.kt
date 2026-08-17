package com.libredisplay.data.local

import androidx.room.Room
import org.robolectric.RuntimeEnvironment

internal object InMemoryRoomTestDatabase {
    fun create(): LibreDisplayDatabase {
        return Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            LibreDisplayDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }
}


