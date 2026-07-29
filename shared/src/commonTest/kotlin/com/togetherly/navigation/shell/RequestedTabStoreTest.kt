package com.togetherly.navigation.shell

import com.togetherly.navigation.destination.MainDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestedTabStoreTest {

    @Test
    fun requestingATabUpdatesTheStateFlow() {
        val store = RequestedTabStore()

        store.request(MainDestination.Journey)

        assertEquals(MainDestination.Journey, store.requestedTab.value)
    }

    @Test
    fun consumingClearsTheRequestedTab() {
        val store = RequestedTabStore()
        store.request(MainDestination.Journey)

        store.consume()

        assertNull(store.requestedTab.value)
    }

    @Test
    fun startsWithNoRequestedTab() {
        assertNull(RequestedTabStore().requestedTab.value)
    }
}
