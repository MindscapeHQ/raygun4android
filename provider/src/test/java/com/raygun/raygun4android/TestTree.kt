package com.raygun.raygun4android

import timber.log.Timber

class TestTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        // Do nothing
    }
}
