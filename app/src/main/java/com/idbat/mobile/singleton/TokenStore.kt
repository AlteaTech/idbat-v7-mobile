package com.idbat.mobile.singleton

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor() {
    var token: String = ""
}
