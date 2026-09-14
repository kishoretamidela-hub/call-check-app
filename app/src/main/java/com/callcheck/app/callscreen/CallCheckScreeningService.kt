package com.callcheck.app.callscreen

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * REAL Android API — this is genuinely how Truecaller/Hiya-style spam
 * blocking works. Once the user sets CallCheck as their default
 * "Caller ID & spam blocking" app (Settings > Apps > Default apps),
 * every incoming call is routed through onScreenCall() before it rings.
 *
 * LIMITATION: this callback gives you the phone number and basic call
 * details — never audio. There is no Android API that hands a
 * third-party app the live audio of an ongoing call. That restriction
 * is why the "voice authenticity" feature lives in a separate,
 * user-initiated flow (VoiceCheckService) instead of here.
 */
class CallCheckScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: ""

        val isKnownScamPattern = KnownScamNumbers.isFlagged(number)

        val response = CallResponse.Builder()
        if (isKnownScamPattern) {
            response
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(false) // still tell the user it was blocked
        } else {
            response.setDisallowCall(false)
        }
        respondToCall(callDetails, response.build())
    }
}

/**
 * Placeholder for a real scam-number list. In production, back this with
 * either a community-reported number database (many exist commercially)
 * or your own backend — this local stub is intentionally minimal.
 */
object KnownScamNumbers {
    private val flagged = setOf<String>() // populate from a real data source
    fun isFlagged(number: String): Boolean = number in flagged
}
