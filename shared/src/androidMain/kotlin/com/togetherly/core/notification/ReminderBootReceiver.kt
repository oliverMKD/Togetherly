package com.togetherly.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.repository.FamilyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * `AlarmManager` alarms don't survive a device reboot — this restores them from whatever
 * [com.togetherly.domain.family.ReminderPreference] is currently persisted, the same source of
 * truth the Reminder screen itself reads/writes. Registered for `ACTION_BOOT_COMPLETED`
 * (`AndroidManifest.xml`) with `RECEIVE_BOOT_COMPLETED` — Android guarantees the app's
 * [android.app.Application.onCreate] (where Koin starts) has already run before any of the app's
 * own manifest-registered receivers are invoked, so [GlobalContext.get] resolving here is safe.
 * A missing/disabled reminder preference is a normal, silent no-op — most families never enable
 * reminders at all.
 */
internal class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val koin = GlobalContext.getOrNull() ?: return@launch
                val familyRepository = koin.get<FamilyRepository>()
                val scheduler = koin.get<ReminderScheduler>()
                val profile = (familyRepository.getProfile() as? DataResult.Success)?.value
                profile?.reminderPreference?.let { scheduler.refresh(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
