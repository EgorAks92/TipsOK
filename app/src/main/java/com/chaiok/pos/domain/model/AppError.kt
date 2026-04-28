package com.chaiok.pos.domain.model

sealed class AppError(val message: String) {
    data object InvalidPin : AppError("Неверный PIN-код. Проверьте код и попробуйте снова.")
    data object CardReadError : AppError("Не удалось прочитать карту. Попробуйте еще раз.")
    data object Unknown : AppError("Произошла ошибка. Повторите попытку.")
}
