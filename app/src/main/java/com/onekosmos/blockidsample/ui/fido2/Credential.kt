package com.onekosmos.blockidsample.ui.fido2

data class Credential(
    val sub: String,
    val status: String,
    val error: String?
)
