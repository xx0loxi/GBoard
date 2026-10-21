# GBoard — модуль патчів для Gboard (Morphe)

Робочий простір для збірки патч-бандла **GBoard** (Gboard + Gemini Rambler, AI Writing
Tools / Writing Tools V2, сучасні підказки перевірки орфографії, кастомна палітра
клавіатури, нескінченний та зашифрований буфер обміну, перейменування пакета для
співіснування зі стоковим Gboard та обхід перевірки підпису).

Патч-модуль інтегровано з відкритого проєкту
[PixelBoard](https://github.com/Akshayykadam/PixelBoard) (Akshay Kadam, GPLv3).
Структура модуля збережена 1:1 з upstream, щоб полегшити майбутні оновлення.

## Базова версія

| Параметр | Значення |
| --- | --- |
| Цільовий Gboard | **18.5.3.993608717-beta-arm64-v8a** (перевірено патчингом stock APK) |
| Додатково підтримується | 18.4.1.985164140 (release), 18.0.3.954559732, 18.3.1.977415014 (release / beta) |
| Формат | Morphe `.mpp` (Morphe Manager / `patcher.jar`) |
| Java | JDK 21+ (збірка перевірена на JDK 21 і 25; тести — на JDK 21) |
| Android SDK | потрібен для модуля `:extensions:extension` |

> Публічний `patches-list.json` заявляє сумісність лише з 18.5.3 — саме цей
> перелік бачить Morphe Manager. Усередині бандла патчі лишаються сумісними з
> усіма версіями з таблиці (`COMPATIBILITY_GBOARD`), тож старіші APK теж
> патчаться, якщо застосувати `.mpp` напряму.

> **18.5.3:** fingerprint-и зняті з реального APK
> `18.5.3.993608717-beta-arm64-v8a` (versionCode `176049654`), а не вгадані:
> signature bypass `Lakft;`, геттер прапорців `Ladbx;`, Rambler
> `VoiceSettingsFragment->aD/ac/f/aF` + `Laaqf;->a` + `Lkrl;->e`, черга мов
> `Lambj;->c`, кліпборд `Lldo;` (сховище, очищення, ліміт історії), теми
> `Lvjr;->p`. Патчинг прогнано end-to-end, усі 10 патчів застосовуються.
> Деталі та процедура для наступних версій:
> [`docs/VERSION-TARGETS.md`](docs/VERSION-TARGETS.md).

## Швидкий старт

```bash
# 1. Збірка патч-бандла (з кореня проєкту)
./gradlew buildAndroid

# Результат (публікується автоматично):
#   patches/GBoard.mpp          <- стабільна назва для Morphe Manager / посилань
#   patches/patches-1.3.1.mpp       <- версійна копія
#   patches/patches-bundle.json     <- метадані репозиторію Morphe
#   patches/patches-list.json       <- інвентар патчів
```

Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.5"
.\gradlew.bat buildAndroid
```

Локальні налаштування:

* `local.properties` → `sdk.dir=C:/Users/<user>/AppData/Local/Android/Sdk`
  (файл у `.gitignore`).
* Для онлайн-збірки потрібні credentials GitHub Packages для плагіна Morphe
  (`gpr.user` / `gpr.key` у `~/.gradle/gradle.properties` або `GITHUB_ACTOR` /
  `GITHUB_TOKEN`). Якщо всі залежності вже в кеші Gradle, збірка працює з `--offline`.

## Як патчити Gboard

APK у репозиторії не збирається і не публікується — продукт це `.mpp`-бандл,
який застосовується до стокового Gboard у Morphe Manager.

1. Завантажити стоковий Gboard APK (APKMirror), підтримувані версії — у таблиці
   вище. Для 18.5.3 прямий лінк:
   [`gboard-the-google-keyboard-18-5-3-993608717-beta-arm64-v8a`](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/gboard-the-google-keyboard-18-5-3-993608717-beta-arm64-v8a-android-apk-download/).
2. Відкрити **Morphe Manager** → **Storage** → вибрати Gboard APK.
3. Як джерело патчів вказати [`patches/GBoard.mpp`](patches/GBoard.mpp)
   (або URL репозиторію з `patches-bundle.json`).
4. Увімкнути потрібні патчі → **Patch** → зберегти підписаний APK → встановити.

> **Кнопка «Download» у Morphe Manager для Gboard веде в Google-пошук.** Це не
> налаштування бандла: менеджер бере URL зі свого API
> (`api.morphe.software/v2/web-search/<package>~<version>~<abi>`), а в базі API
> немає Gboard 18.5.3 beta, тому він віддає fallback-пошук. Лінк зі `.mpp` не
> змінюється — відкривайте сторінку APKMirror вище (або посилання з налаштувань
> GBoard → Advanced Voice Typing).

## Яка AI використовується

Патч не додає власної моделі — він вмикає штатні AI-функції Gboard, а модель
надає Google:

| Функція | Модель / бекенд |
| --- | --- |
| AI Writing Tools (proofread, rewrite, tone, «My Style») на Pixel 8 Pro / Pixel 9 / Pixel 10 | **Gemini Nano** через Android **AICore** (Private Inference, `PRIVATE_INFERENCE_AICORE`) або Astrea |
| AI Writing Tools на інших пристроях | хмара Google (`GBOARD_SERVER`), model config `202408051448_prod_sd_config` |
| Gemini Rambler voice typing | серверна модель Google Rambler (не on-device) |

**Gemini Nano v2 окремо не вибирається.** Версію Nano визначає системний модуль
AICore (оновлюється через Play), а не Gboard і не патч: Gboard лише запитує
функцію. Якщо на пристрої стоїть AICore з Nano v2, Gboard використає її
автоматично; зашити чи примусово перемкнути модель зі сторони `.mpp`
неможливо. Єдиний «модельний» важіль у патчі — cloud-конфіги вище, і їх зміна
на вгадані значення лише зламає запити до сервера.

Локальна альтернатива без GUI (патчить APK через `tools/patcher.jar`):

```bash
# Покласти стоковий Gboard APK у input/gboard.apk
./build.sh                 # або ./build.sh --rebuild
# Результат: output/GBoard.apk, output/GBoard-1.3.1.apk
```

## Що всередині

Публічні патчі (реєстр `GboardPatchRegistry.kt`):

| Патч | Що робить |
| --- | --- |
| `Advanced Voice Typing` | Вмикає розширений голосовий ввід Gemini Rambler: форсує `enable_nga`, `enable_advanced_features_in_consolidated_sd_stack`, `enable_soda_longform_experiment` тощо, хуки в `VoiceSettingsFragment`, `enable_jetson` у device-protected prefs |
| `AI Writing Tools` | Writing Tools / Writing Tools V2: форсує `writing_helper`, `writing_tools`, `enable_writing_tools_v2*`, бекенд (`GBOARD_SERVER` / `PRIVATE_INFERENCE_AICORE` / `ASTREA`), хуки GenAI init/refresh, auto-fix route, modeless smart edit |
| `Add Gboard Signature Bypass` | Примусово повертає `true` з перевірки підпису (`Lryi;->a(Context,String)Z` для 18.4.1 + fallback-и для 18.3.1/18.0.3) |
| `Package Rename` | Змінює пакет на `com.akshaykadam.pixelboard`, перейменовує authorities/permissions/Phenotype-registration та дозволяє власну назву застосунку (типово `Gboard`) |
| `Performance & Privacy` | Глушить фонові ресивери Primes/AppDoctor/Phenotype, нативний crash-reporter, daily-ping планувальник і рекламний reporting-widget — менше wake-up'ів, мережі та батареї |
| `Disable Cloud Backup` | Прибирає `allowBackup`/`backupAgent`/`dataExtractionRules` з манифеста, щоб словник і налаштування не потрапляли в Google Drive |
| `Unlock All Themes` | Розгортає кожну категорію тем повністю: замість одного ряду + картки «Show more» показує всі теми одразу (хук `Lvjr;->p`, заміна ліміту на 10000) |
| `Theme Studio` | Кастомна палітра з живим прев'ю: фон клавіатури, клавіші, підписи та цифри. У розділі «Тема» Gboard з'являється плаваюча кнопка **Theme Studio**, яка відкриває панель налаштувань; кольори примусово застосовуються хуками `SoftKeyboardView.onLayout` і `SoftKeyView.onDraw` |
| `Infinite Clipboard` | Прибирає годинний TTL буфера (`Llcs;->a(Context)` → `Long.MAX_VALUE`), періодичне очищення (`Lldo;->g()`) і обрізання історії до 100 записів (`Llcs;->k`) |
| `Secure Clipboard Vault` | Шифрує історію буфера (AES-GCM, ключ в Android Keystore) у `getNoBackupFilesDir()`; переживає перезавантаження й оновлення застосунку, не потрапляє в бекапи |
| `GboardSpellCheckerChipsPatch` (внутрішній) | Форсує inline spell-checker chips та add-to-dictionary на Android 12+ |

> **Про «преміум» теми.** У Gboard 18.5.3 немає платних/заблокованих вбудованих
> тем: усі теми з каталогу доступні одразу, а платні теми з інтернету — це
> сторонні Rboard-пакети, які патч не додає. `Unlock All Themes` прибирає лише
> UI-скорочення списку («Show more»), а довільні кольори дає `Theme Studio`.

Детальний розбір хуків, прапорців Phenotype, SharedPreferences і Device Protected
Storage: [`docs/ANALYSIS-UK.md`](docs/ANALYSIS-UK.md).

## Структура

```
GBoard/
├── patches/                     # Kotlin-джерела патчів + готовий бандл
│   ├── GBoard.mpp               # зібраний патч-бандл (для Morphe Manager)
│   ├── patches-1.3.1.mpp        # версійна копія бандла
│   ├── patches-bundle.json      # метадані репозиторію Morphe
│   ├── patches-list.json        # інвентар патчів
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
├── patches-bundle.json          # ті самі метадані (генеруються в корінь)
├── patches-list.json            # той самий інвентар (генерується в корінь)
├── options.json                 # пресети патчів для patcher.jar
├── prebuilt/                    # готовий .mpp 1.3.1 (офлайн-фолбек)
├── tools/patcher.jar            # Morphe CLI patcher
├── input/                       # сюди кладеться gboard.apk (локально, не в git)
├── output/                      # локальний результат патчингу
├── docs/                        # аналіз, версії, upstream-документація
└── build.sh                     # збірка бандла + локальний патчинг
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
