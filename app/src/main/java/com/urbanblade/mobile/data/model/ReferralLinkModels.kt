package com.urbanblade.mobile.data.model

/** `POST referrals/link`: registra el código de quien invitó al cliente (una sola vez por cliente). */
data class LinkReferralRequest(val codigo: String)
