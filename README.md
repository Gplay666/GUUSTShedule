# UUST Schedule

Версия 0.2.0.

Нативное приложение расписания УУНиТ на Kotlin Multiplatform и Compose Multiplatform.
Собраны desktop-таргет для Linux и Android-приложение; общий UI и слой данных
лежат в `commonMain`.

## Возможности

- поиск по 3078 группам и загрузка расписания выбранной группы;
- фильтр по учебной неделе; по умолчанию открывается текущая неделя, можно выбрать «Все»;
- desktop/landscape: таблица на всю неделю, столбцы — дни, строки — пары;
  пустые пары остаются в таблице и имеют ту же высоту, что и заполненные;
- mobile/portrait: выбор дня недели и расписание только выбранного дня;
- на компактных экранах, включая 4:3 без постоянной боковой панели, верхняя панель
  компактная, как в портретной ориентации; боковая панель открывается только
  кнопкой-гамбургером и закрывается крестиком;
- на Android системная кнопка или жест «Назад» при открытой боковой панели закрывают её,
  а не приложение;
- в портретной ориентации кнопка меню остаётся на месте, а название группы при прокрутке
  сворачивается в верхнюю строку; дни недели показаны компактными кнопками без счётчика пар,
  а свайп влево/вправо по списку занятий переключает день недели;
- несколько занятий в одной ячейке показываются стопкой;
- светлая, тёмная и автоматическая темы, Material 3 Expressive-стилистика;
- выбранная тема и цветовая палитра сохраняются локально и не сбрасываются при перезапуске;
- кэш справочника групп и расписаний, ручное обновление.

## Запуск

Нужен JDK 17+.

```bash
./gradlew :run
```

## Сборка

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

## API

Приложение обращается к API напрямую, без прокси:

- база: `https://dev.uust-time.ru/api/v/852972/`;
- обязательный параметр: `site=schedule`;
- расписание: `schedule/0/{group_id}/semester/241`;
- заглушки с пустым предметом или `schedule_weekday_id = 0` отфильтровываются.

Подробное описание API — в [`API_docs.md`](API_docs.md).

## Структура

- `src/commonMain/kotlin/ru/uust/schedule/ui/` — общий Compose UI;
- `src/commonMain/kotlin/ru/uust/schedule/data/` — DTO, репозиторий, HTTP-клиент;
- `src/desktopMain/kotlin/ru/uust/schedule/Main.kt` — точка входа desktop;
- `src/androidMain/kotlin/ru/uust/schedule/MainActivity.kt` — точка входа Android;
- `src/desktopMain/kotlin/ru/uust/schedule/util/` и
  `src/androidMain/kotlin/ru/uust/schedule/util/` — платформенные actual-функции.

Настройки темы хранятся отдельно от кэша расписания:

- Android — `SharedPreferences` (`uust-schedule-settings`);
- desktop — `$XDG_CONFIG_HOME/uust-schedule/settings.properties`
  (по умолчанию `~/.config/uust-schedule/settings.properties`).

## Текущая неделя

Начало первой учебной недели для семестра `241` — понедельник `2026-08-31`
(см. `API_docs.md`). Номер текущей недели вычисляется в
`src/desktopMain/kotlin/ru/uust/schedule/util/Platform.desktop.kt` и
`src/androidMain/kotlin/ru/uust/schedule/util/Platform.android.kt`.
При смене семестра это значение нужно обновить.
