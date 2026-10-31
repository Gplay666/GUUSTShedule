# Предупреждение

Это приложение писал бесплатный агент в рамках моего тестирования их возможностей. Я не могу ручаться за качество этого приложения. Я не считаю себя правым считать себя автором этого приложения. Я считаю себя должным указать это явно прямо здесь.

## О приложении

Версия 0.2.0.

Нативное приложение расписания УУНиТ на Kotlin Multiplatform и Compose Multiplatform.
Собраны desktop-таргет для Linux и Android-приложение; общий UI и слой данных
лежат в `commonMain`.

## Возможности

- Поиск по группам и загрузка расписания выбранной группы
- Адаптивный интерфейс для любого масштаба экрана
- Светлая, тёмная и автоматическая темы, Material 3 Expressive, выбор цветовой схемы
- Кеширование расписания для доступа без интернета

## Запуск из исходников

Нужен JDK 17+.

```bash
./gradlew :run
```

## Сборка из исходников

Проверка компиляции:

```bash
./gradlew :desktopMainClasses
./gradlew :assembleDebug
```

Android APK:

```bash
ANDROID_HOME=/path/to/Android/Sdk ./gradlew :assembleDebug
# build/outputs/apk/debug/uust-schedule-debug.apk
```

Каталог с готовым desktop-приложением:

```bash
./gradlew createDistributable
# build/compose/binaries/main/app/uust-schedule/
```

AppImage:

```bash
./gradlew packageAppImage
# build/compose/binaries/main/appImage/
```
