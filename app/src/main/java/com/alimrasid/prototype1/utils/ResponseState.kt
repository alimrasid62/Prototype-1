package com.alimrasid.prototype1.utils

class ResponseState<T> {
    var status: Status? = null
        private set
    var data: T? = null
        private set
    var message: T? = null
        private set

    enum class Status {
        SUCCESS,
        FAILURE
    }

    constructor() {
        this.status = null
        this.data = null
        this.message = null
    }

    fun success(data: T): ResponseState<T> {
        status = Status.SUCCESS
        this.data = data
        return this
    }

    fun failure(message: T): ResponseState<T> {
        status = Status.FAILURE
        this.message = message
        return this
    }
}
