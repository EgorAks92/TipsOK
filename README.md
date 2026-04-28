# ЧайОК (Android POS)

Современное Android-приложение для POS-терминала ресторана, реализованное на Kotlin + Jetpack Compose c Clean Architecture.

## Что реализовано
- Вход официанта по PIN (mock-проверка: `1234`).
- Главный экран ввода суммы с кастомной цифровой клавиатурой.
- Проверка привязки карты после входа и диалог с переходом.
- Экран настроек с разделами: привязка карты, статус, мои чаевые, интеграционный режим и фон профиля.
- Экран привязки карты с mock card reader сервисом и состояниями ожидания/чтения/ошибки/успеха.
- Экран выбора статуса с локальным сохранением.
- Экран истории чаевых с mock-данными (8 записей) и сводкой.
- Экран интеграционных настроек (integration mode + table mode) и поддержка вариантов фоновой плитки профиля (`default/sunset/forest`).
- Сессия пользователя с logout и возвратом на login.

## Стек
- Kotlin
- Jetpack Compose + Material 3
- MVVM
- Coroutines / StateFlow
- Navigation Compose
- DataStore (настройки и несекретные флаги)
- EncryptedSharedPreferences + Android Keystore (чувствительные данные)
- minSdk 26, target/compileSdk 35

## Архитектура
Слои:
- `domain`: модели, интерфейсы репозиториев, use case.
- `data`: mock-реализации API/сервисов, DataStore/Encrypted storage, реализации репозиториев.
- `presentation`: Compose-экраны, ViewModel, ui-state/events, навигация.

`presentation` -> `domain` через UseCase.
`data` предоставляет реализации интерфейсов `domain`.

## Запуск
1. Откройте проект в Android Studio (JDK 17).
2. Выполните Gradle sync.
3. Запустите `app` на Android-устройстве/эмуляторе с API 26+.

## Где находятся mock API / заглушки
- PIN auth: `MockAuthRepository`.
- Профиль официанта: `MockWaiterRepository`.
- История чаевых: `MockTipsRepository`.
- Чтение карты: `MockCardReaderRepository` (по умолчанию `AlwaysSuccess`, можно переключить на `Random`/`AlwaysError`).

## Как заменить mock на реальные интеграции
1. Оставьте интерфейсы `domain/repository/*` и замените только реализации в `data/repository/*`.
2. Замените `MockCardReaderRepository` на реализацию реального POS/Card Reader SDK через `CardReaderRepository`.
3. Подключите backend API-клиент в `data` и мапперы DTO -> domain модели.
4. Обновите `AppContainer`, подставив production-реализации.

## TODO для следующего этапа
- Подключить backend API проверки PIN.
- Подключить backend API загрузки профиля официанта.
- Подключить реальный SDK чтения карты POS-терминала.
- Отправлять результат привязки карты на backend.
- Получать историю чаевых с сервера.
- Реализовать интеграцию с кассовой/POS-системой в integration mode.


## Тесты
- Добавлены unit tests для LoginWithPinUseCase, LinkCardUseCase, HomeViewModel и TipsViewModel.
