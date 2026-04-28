package com.chaiok.pos.domain.error

sealed class DomainError(message: String) : Exception(message) {
    data object InvalidPin : DomainError("Неверный PIN-код. Проверьте код и попробуйте снова.")
    data object CardReadFailed : DomainError("Не удалось прочитать карту. Попробуйте еще раз.")
    data object CardLinkFailed : DomainError("Не удалось сохранить привязку карты. Попробуйте еще раз.")
    data object StorageFailed : DomainError("Ошибка локального хранения данных.")
    data object ProfileNotFound : DomainError("Профиль официанта не найден.")
}
