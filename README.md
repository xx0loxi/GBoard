# GBoard — модуль патчів для Gboard (Morphe)

Робочий простір для збірки патч-бандла **GBoard** (Gboard + Gemini Rambler, AI Writing
Tools / Writing Tools V2, сучасні підказки перевірки орфографії, перейменування пакета
для співіснування зі стоковим Gboard та обхід перевірки підпису).

Патч-модуль інтегровано з відкритого проєкту
[PixelBoard](https://github.com/Akshayykadam/PixelBoard) (Akshay Kadam, GPLv3).
Структура модуля збережена 1:1 з upstream, щоб полегшити майбутні оновлення.

## Базова версія

| Параметр | Значення |
| --- | --- |
| Цільовий Gboard | **18.4.1.985164140-release-arm64-v8a** (основна, перевірена) |
| Додатково підтримується | 18.0.3.954559732, 18.3.1.977415014 (release / beta) |
| Формат | Morphe `.mpp` (Morphe Manager / `patcher.jar`) |
| Java | JDK 21+ (перевірено на JDK 25) |
| Android SDK | потрібен для модуля `:extensions:extension` |

> **18.5.3:** upstream PixelBoard 1.2.0 не містить fingerprint-ів і обфускованих імен
> класів для 18.5.3, тому додавати її «наосліп» небезпечно — патчі або тихо не
> застосуються, або впадуть на верифікації. Підготовлено процедуру порту:
> [`docs/VERSION-TARGETS.md`](docs/VERSION-TARGETS.md). Після появи APK 18.5.3
> достатньо додати нові target-и та оновити список версій.

## Швидкий старт

```bash
# 1. Збірка патч-бандла (з кореня проєкту)
./gradlew buildAndroid

# Результат:
#   patches/build/libs/patches-1.2.0.mpp
```

Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.5"
.\gradlew.bat buildAndroid
```

Локальні налаштування:

* `local.properties` → `sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk`
  (файл у `.gitignore`).
* Для онлайн-збірки потрібні credentials GitHub Packages для плагіна Morphe
  (`gpr.user` / `gpr.key` у `~/.gradle/gradle.properties` або `GITHUB_ACTOR` /
  `GITHUB_TOKEN`). Якщо всі залежності вже в кеші Gradle, збірка працює з `--offline`.

Повний цикл «зібрати + пропатчити APK»:

```bash
# Покласти стоковий Gboard APK у input/gboard.apk
./build.sh            # або ./build.sh --rebuild
# Результат: output/GBoard.apk, output/GBoard-1.2.0.apk
```

## Що всередині

Публічні патчі (реєстр `GboardPatchRegistry.kt`):

| Патч | Що робить |
| --- | --- |
| `Advanced Voice Typing` | Вмикає розширений голосовий ввід Gemini Rambler: форсує `enable_nga`, `enable_advanced_features_in_consolidated_sd_stack`, `enable_soda_longform_experiment` тощо, хуки в `VoiceSettingsFragment`, `enable_jetson` у device-protected prefs |
| `AI Writing Tools` | Writing Tools / Writing Tools V2: форсує `writing_helper`, `writing_tools`, `enable_writing_tools_v2*`, бекенд (`GBOARD_SERVER` / `PRIVATE_INFERENCE_AICORE` / `ASTREA`), хуки GenAI init/refresh, auto-fix route, modeless smart edit |
| `Add Gboard Signature Bypass` | Примусово повертає `true` з перевірки підпису (`Lryi;->a(Context,String)Z` для 18.4.1 + fallback-и для 18.3.1/18.0.3) |
| `Package Rename` | Змінює пакет на `com.akshaykadam.pixelboard`, перейменовує authorities/permissions/Phenotype-registration та дозволяє власну назву застосунку |
| `GboardSpellCheckerChipsPatch` (внутрішній) | Форсує inline spell-checker chips та add-to-dictionary на Android 12+ |

Детальний розбір хуків, прапорців Phenotype, SharedPreferences і Device Protected
Storage: [`docs/ANALYSIS-UK.md`](docs/ANALYSIS-UK.md).

## Структура

```
GBoard/
├── patches/                     # Kotlin-джерела патчів + JSON-конфігурація версій
│   ├── src/main/kotlin/...      # features/*, registry/*, shared/*
│   ├── src/bindingCompiler/     # генератор version bindings з JSON
│   ├── src/main/resources/gboard/
│   │   ├── gboard-version-bindings.json       # прив'язки для 18.0.3
│   │   ├── gboard-port-product-catalog.json   # склад/контракти фіч
│   │   └── gboard-port-product-catalog.sha256
│   └── src/test/                # контрактні тести реєстру/патчів
├── extensions/extension/        # Java runtime (хуки, налаштування, ресурси)
├── gradle/, gradlew, gradlew.bat
├── settings.gradle.kts
├── gradle.properties            # версія патч-бандла
├── patches-bundle.json          # метадані бандла
├── patches-list.json            # інвентар патчів для Morphe Manager
├── options.json                 # пресети патчів для patcher.jar
├── prebuilt/                    # готовий .mpp 1.2.0 (офлайн-фолбек)
├── tools/patcher.jar            # Morphe CLI patcher
├── input/                       # сюди кладеться gboard.apk
├── output/                      # результат патчингу
├── docs/                        # аналіз, версії, upstream-документація
└── build.sh                     # збірка + патчинг одною командою
```

## Діагностика

```bash
./gradlew buildAndroid --offline       # швидка перевірка збірки з кешу
./gradlew test                         # контрактні тести (JDK 21; інтернет для ASM/Robolectric)
./gradlew verifyGboardCapabilityWiring # перевірка, що фічі підключені до реєстру
```

Тести extension-модуля використовують Robolectric 4.14.1 і ASM 9.7.1, які не
читають class-файли JDK 25+, тому для `test` рекомендовано JDK 21 (як у CI).
`buildAndroid` при цьому успішно працює і на JDK 25. Мова тестів зафіксована як
`en-US`, щоб результати не залежали від локалі хоста.

Якщо Gradle не може знайти плагін `app.morphe.patches:1.3.3`, перевірте
`gpr.user`/`gpr.key` — репозиторій Morphe хоститься на GitHub Packages.

## Ліцензія та атрибуція

Код патчів і runtime походять з PixelBoard (GPLv3). Будь-яке розповсюдження похідних
бінарників має супроводжуватися джерельним кодом під GPLv3. Upstream-документація
збережена в `docs/upstream/`.
