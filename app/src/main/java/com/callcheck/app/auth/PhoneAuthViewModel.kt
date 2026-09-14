package com.callcheck.app.auth

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * Real Firebase Phone Auth. This actually sends a live SMS OTP —
 * NOT a simulated one — once you've:
 *   1. Created a Firebase project at console.firebase.google.com
 *   2. Enabled "Phone" under Authentication > Sign-in method
 *   3. Dropped your own google-services.json into app/
 *   4. Added a SHA-1 fingerprint (needed for Play Integrity / reCAPTCHA fallback)
 *
 * Firebase's free tier includes a limited number of real SMS OTPs/month;
 * beyond that it bills per verification.
 */
class PhoneAuthViewModel : ViewModel() {

    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val auth = FirebaseAuth.getInstance()

    fun sendOtp(
        activity: Activity,
        e164Phone: String, // e.g. "+919876543210"
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: () -> Unit
    ) {
        isLoading = true
        errorMessage = null

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Android auto-read the SMS — sign in immediately, no manual OTP entry needed.
                signIn(credential, onSuccess = onAutoVerified)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                errorMessage = e.message ?: "Verification failed."
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                isLoading = false
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(verificationId: String, code: String, onSuccess: () -> Unit) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signIn(credential, onSuccess)
    }

    private fun signIn(credential: PhoneAuthCredential, onSuccess: () -> Unit) {
        isLoading = true
        auth.signInWithCredential(credential)
            .addOnSuccessListener { isLoading = false; onSuccess() }
            .addOnFailureListener {
                isLoading = false
                errorMessage = it.message ?: "That code didn't match."
            }
    }
}
