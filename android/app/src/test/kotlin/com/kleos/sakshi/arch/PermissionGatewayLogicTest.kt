package com.kleos.sakshi.arch

import com.kleos.sakshi.host.PermissionGateway
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionGatewayLogicTest {
    @Test
    fun restrictedSettingsIsSuspectedOnlyWhenTheUserWentThereAndAccessIsStillOff() {
        assertEquals(true, PermissionGateway.restrictedSuspected(openedSettingsPage = true, granted = false))
        assertEquals(false, PermissionGateway.restrictedSuspected(openedSettingsPage = true, granted = true))
        assertEquals(false, PermissionGateway.restrictedSuspected(openedSettingsPage = false, granted = false))
        assertEquals(false, PermissionGateway.restrictedSuspected(openedSettingsPage = false, granted = true))
    }
}
