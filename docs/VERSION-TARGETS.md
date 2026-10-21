# Цільові версії Gboard і порт на 18.5.3

## 1. Поточна матриця

| Gboard | Статус у цьому проєкті | Примітки |
| --- | --- | --- |
| `18.0.3.954559732-release-arm64-v8a` | підтримується | базові bindings (`gboard-version-bindings.json`), повний набір хуків |
| `18.3.1.977415014-beta-arm64-v8a` | підтримується | окремі обфусковані target-и |
| `18.3.1.977415014-release-arm64-v8a` | підтримується | окремі target-и |
| `18.4.1.985164140-release-arm64-v8a` | **основна / перевірена** | signature `Lryi;`, Rambler 18.4.1, flag getter `Lodx;` |
| `18.5.3.*` | **не додано** | немає fingerprint-ів/обфускованих імен; див. §2 |

Список версій, які Morphe Manager вважає сумісними, генерується з
`DEFAULT_ARTIFACT_IDENTITY` у
`patches/src/bindingCompiler/kotlin/com/akshaykadam/pixelboard/tools/bindings/GboardTargetBuildCompiler.kt`.
Він же потрапляє в `patches-list.json` і `GboardTargetAdmission.versionNames`.

## 2. Чому 18.5.3 не додана «зараз»

Патчі PixelBoard — не універсальні regex-и, а точкові smali-трансформації з
перевіркою форми інструкцій. Для нової версії Gboard потрібно знати:

1. точний `versionName` (наприклад, `18.5.3.xxxxxxx-release-arm64-v8a`) — без нього
   патч не потрапить у `compatiblePackages`;
2. нові обфусковані імена класів/методів для:
   * перевірки підпису (`Lryi;` у 18.4.1),
   * `VoiceSettingsFragment`-селектора Rambler,
   * геттера Phenotype-прапорців (`Lodx;->g()`),
   * `LanguageDownloadQueue`,
   * п'яти 18.0.3-хуків Writing Tools (можуть зникнути/переїхати);
3. збіг сигнатур і регістрів (патчі падають, якщо форма не збігається).

Додавання версії без цих даних дає або «тихо не застосовано», або помилку
верифікації під час патчингу. Тому 18.5.3 винесена в окрему процедуру.

## 3. Процедура порту на 18.5.3

### Крок 0. Підготувати APK

```bash
# точний versionName
aapt2 dump badging input/gboard.apk | head -1
# або
adb shell dumpsys package com.google.android.inputmethod.latin | grep versionName
```

Записати рядок у форматі `<versionName>-<release|beta>-arm64-v8a` (саме так його
очікує Morphe).

### Крок 1. Додати версію до admission

`GboardTargetBuildCompiler.kt` → `DEFAULT_ARTIFACT_IDENTITY.versionNames`:

```kotlin
versionNames = listOf(
    "18.0.3.954559732-release-arm64-v8a",
    "18.3.1.977415014-beta-arm64-v8a",
    "18.3.1.977415014-release-arm64-v8a",
    "18.4.1.985164140-release-arm64-v8a",
    "18.5.3.XXXXXXX-release-arm64-v8a",   // ← додати
),
```

Оновити очікування в
`patches/src/test/kotlin/.../registry/GboardPublishedPatchCatalogContractTest.kt`.

### Крок 2. Signature bypass

`features/signaturebypass/GboardSignatureBypassBytecodePatch.kt`:

1. додати `SIGNATURE_UTILS_CLASS_1853` (новий obfuscated-клас);
2. додати `DIGEST_METHOD_DESCRIPTOR_1853`, `TARGET_FIELD_DESCRIPTORS_1853`;
3. додати гілку в `injectSignatureBypass()`, `classifyGboardSignatureBypass()`,
   `isExactGboardSignatureBypassTarget()`;
4. перевірити кількість `RETURN` (у 18.4.1 — 3), `registerCount` (8) і літерали.

Якщо форма методу інша — оновити `TARGET_*` константи, а не послаблювати
перевірки.

### Крок 3. Rambler / Voice Settings

`features/rambler/GboardRambler1803OfficialSelectorPatch.kt`:

1. додати `RAMBLER_1853_TARGETS` (layout/setup/create/selectionWrite/selectionRead/
   defaultSelection);
2. додати перевірку в `when` **перед** 18.4.1;
3. перевірити `suppressRamblerFeedbackAccessPoint()` — провайдер міг переїхати;
4. оновити runtime-фолбеки в
   `extensions/.../rambler/GboardRambler1803OfficialSelectionRuntime.java`
   (`Class.forName("mvq")` тощо) — додати новий клас перед 18.4.1.

`GboardAdvancedVoice1831LanguageDownloadGuardPatch.kt` — додати новий
`GboardMethodTarget` для `LanguageDownloadQueue.enqueue` у список
`languageDownloadEnqueueTargets`.

### Крок 4. Геттер Phenotype-прапорців

`shared/GboardFlagFamilyComposer.kt`:

1. додати `targetMethod1853` / `targetField1853`;
2. додати їх у `when` найпершими;
3. якщо змінилася сигнатура геттера — оновити `GboardVersionBindings`
   (`gboard-version-bindings.json`) і `required_bindings` у каталозі.

### Крок 5. Writing Tools V2

Спочатку перевірити, чи існують цілі 18.0.3-хуків у 18.5.3:

| Файл | Ціль |
| --- | --- |
| `GboardAiWritingToolsFlagValuePatch.kt` | `Lrzb;->v()Lvvw;` |
| те саме | `Lgwf;->a(Lgwo;)Llyg;` |
| те саме | `Lifn;->c(NgaInputManager,Lhmi;)V`, `Lifn;->d()V` |
| те саме | `NgaInputManager->e()V` |

Якщо класи переїхали — оновити `GboardMethodTarget` і внутрішні посилання на
поля (`Lzxi;->c`, `Lidr;->j`, `Lunb;->cG/cH`, `Lvvl;`, `Lvvr;`).
Якщо їх немає — хуки просто пропустяться; основна логіка Writing Tools все одно
працює через форс прапорців у `GboardAiWritingToolsRuntime`.

### Крок 6. Spellchecker / Package Rename

* `GboardSpellCheckerChipsPatch` — структурний пошук, змін зазвичай не потребує.
  Перевірити, що в APK є метод `(Context, TextServicesManager)Z`.
* `GboardPackageRenameResourcePatch` — generic, але перевірити нові
  `ContentProvider`/`receiver` authorities у manifest і додати їх до
  `GBOARD_PACKAGE_RENAME_MAPPINGS` / `GBOARD_OPTIONAL_PACKAGE_RENAME_MAPPINGS`.

### Крок 7. (Опційно) Port bundle

Якщо для 18.5.3 знімаються нові bindings:

```bash
./gradlew buildAndroid \
  -PgboardPortBundleFile=path/to/port-bundle.json \
  -PgboardReviewedBindingsFile=path/to/gboard-version-bindings.json
```

Обидва параметри обов'язкові разом. Перевірте `target_version`, `profile_version`
та SHA-256 у bindings — компілятор звірить їх із port bundle.

### Крок 8. Верифікація

```bash
# Тести (рекомендовано JDK 21: ASM 9.7.1 / Robolectric 4.14.1 не читають
# class-файли новіших JDK)
./gradlew test

# Бандл
./gradlew buildAndroid

# Патчинг
cp /path/to/gboard-18.5.3.apk input/gboard.apk
./build.sh --rebuild
```

Смоук-тест на пристрої:

- [ ] застосунок встановлюється поруч зі стоковим Gboard;
- [ ] `Налаштування → Голосовий ввід` показує Rambler, перемикач зберігається
      після перезапуску;
- [ ] довге натискання на текст → Writing Tools відкриваються, V2-інструкції
      працюють;
- [ ] перевірка орфографії показує chips на Android 12+;
- [ ] у логах немає `Expected exactly one signature bypass target` та
      `Malformed ... scope`.

## 4. Чому тести краще запускати на JDK 21

`./gradlew buildAndroid` успішно працює на JDK 25 (перевірено). Але тести
використовують ASM 9.7.1 і Robolectric 4.14.1, які падають на class-файлах,
скомпільованих новішим JDK (`IllegalArgumentException at ClassReader.java:200`,
NPE в Robolectric). CI upstream використовує Temurin 21 — цей проєкт
дотримується того ж підходу.

## 5. Куди оновлювати документацію

Після успішного порту:

1. `README.md` — таблиця базових версій;
2. `gradle.properties` — комментар про цільову версію;
3. `docs/ANALYSIS-UK.md` — таблиці обфускованих класів;
4. `patches-bundle.json` / `patches-list.json` — регенеруються
   `./gradlew generatePatchesList`.
