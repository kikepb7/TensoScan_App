package com.example.tensoscan.domain.common

sealed class Either<out L, out R> {
    data class Error<out L>(val error: L) : Either<L, Nothing>()
    data class Success<out R>(val data: R) : Either<Nothing, R>()
}