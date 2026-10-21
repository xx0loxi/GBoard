# Цільові версії Gboard і fingerprint-и

## 1. Матриця версій

| Gboard | Статус у цьому проєкті | Примітки |
| --- | --- | --- |
| `18.0.3.954559732-release-arm64-v8a` | підтримується | базові bindings (`gboard-version-bindings.json`), тестовий профіль |
| `18.3.1.977415014-beta-arm64-v8a` | підтримується | окремі обфусковані target-и |
| `18.3.1.977415014-release-arm64-v8a` | підтримується | окремі target-и |
| `18.4.1.985164140-release-arm64-v8a` | **основна** | signature `Lryi;`, Rambler 18.4.1, flag getter `Lodx;` |
| `18.5.3.993608717-beta-arm64-v8a` | **підтримується, перевірено патчингом** | signature `Lakft;`, Rambler 18.5.3, flag getter `Ladbx;` |

Список версій, які Morphe Manager вважає сумісними, генерується з
`DEFAULT_ARTIFACT_IDENTITY` у
`patches/src/bindingCompiler/kotlin/com/akshaykadam/pixelboard/tools/bindings/GboardTargetBuildCompiler.kt`.
Він же потрапляє в `patches/src/main/resources/patches-list.json` і
`GboardTargetAdmission.versionNames`.

## 2. Fingerprint-и по версіях

Патчі PixelBoard — не універсальні regex-и, а точкові smali-трансформації з
перевіркою форми інструкцій. Нижче — реальні цілі, зняті з APK, а не припущення.

### 2.1 Signature bypass

| Версія | Клас | Метод |
| --- | --- | --- |
| 18.0.3 | `Lrpv;` | `a(Context,String)Z` |
| 18.3.1 beta | `Lajpz;` | `a(Context,String)Z` |
| 18.3.1 release | `Lrvl;` | `a(Context,String)Z` |
| 18.4.1 release | `Lryi;` | `a(Context,String)Z` |
| 18.5.3 beta | `Lakft;` | `a(Context,String)Z` |

18.5.3: поля `Lakft;->e/d/c:[B`, зовнішній `Lakeh;->b:Z`, digest
`Lakft;->c(Context,String)[B`, `.locals 6`, літерали 3/0/1/2.

### 2.2 Rambler / Voice settings

| Роль | 18.4.1 | 18.5.3 beta |
| --- | --- | --- |
| layout | `VoiceSettingsFragment->aC()I` | `VoiceSettingsFragment->aD()I` |
| setup | `ac()V` | `ac()V` |
| create | `f(Bundle)V` | `f(Bundle)V` |
| запис вибору | `aE(Z,Lqou;,…)V` | `aF(Z,Lahpn;,…)V` |
| читання вибору | `Lmvq;->a(Context)Z` | `Laaqf;->a(Context)Z` |
| default selection | `Lfde;->hT()V` | `Lkrl;->e()V` |

`Lkrl;->e()` пише `0x7f140957` (`enable_jetson`) у `true` — саме тому він є
ціллю «default selection». `Laaqf;->a(Context)` читає той самий ключ.

У 18.5.3 layout-аксесор `aD()I` збігається з 18.3.1, тому в `when`-ланцюжку
версія визначається за унікальною сигнатурою `aF(Z,Lahpn;,…)V`.

### 2.3 Геттер Phenotype-прапорців

| Версія | Клас | Поле з ім'ям прапорця |
| --- | --- | --- |
| 18.0.3 | `Lnxw;` | `Lnxw;->a:Ljava/lang/String;` |
| 18.3.1 beta | `Lacps;` | `a` |
| 18.3.1 release | `Lobw;` | `a` |
| 18.4.1 release | `Lodx;` | `a` |
| 18.5.3 beta | `Ladbx;` | `a` |

18.5.3: `Ladbx;->g()Ljava/lang/Object;`, значення матеріалізується з
`Ladck;->a:Ljava/lang/Object;`. Форма геттера та сама, що в 18.0.3
(3 регістри, `return-object p0`), тому `GboardFlagFamilyComposer` працює без
змін логіки.

### 2.4 Інші bytecode-цілі

| Патч | 18.4.1 | 18.5.3 beta |
| --- | --- | --- |
| Language download guard | `Lsqk;->c(String,Lsrc;,Consumer;)V` | `Lambj;->c(String,Lamco;,Consumer;)V` |
| Clipboard sync | `Lfml;->ft(...,Lobf;)Z` | `Lldx;->n(...,Lacxj;)Z` (поле `Lldx;->f:Lldo;`) |
| Spellchecker chips | структурний пошук | `Laieo;->a(Context,TextServicesManager)Z` (знайдено автоматично) |
| Emoji vibrancy | `AutoCenterScaleTextView->onDraw` | те саме, без змін |
| Features menu accessibility | `AccessPointsPanel*` | те саме, без змін |

## 3. Що перевірено на 18.5.3

Перевірка виконувалась не «на око», а патчингом реального stock APK
(`18.5.3.993608717-beta-arm64-v8a`, versionCode `176049654`) з наступним
baksmali-розбором результату.

```bash
./gradlew buildAndroid            # публікує patches/PixelBoard.mpp
java -jar tools/patcher.jar patch \
    --patches=patches/PixelBoard.mpp \
    --options-file=options.json --striplibs=arm64-v8a \
    -o=output/GBoard.apk -r=output/patching-result.json input/gboard.apk
```

Результат: `appliedPatches` — усі 4, `failedPatches` — порожній.

Перевірені сліди в патченому DEX:

| Що | Де шукали | Результат |
| --- | --- | --- |
| Signature bypass | `Lakft;->a(Context,String)Z` | кожен `return` → `const/4 …,0x1; return …` |
| Flag family | `Ladbx;->g()` | виклики `afterFlagValue` + `applyOverriddenFlagValue` |
| Rambler scope | `VoiceSettingsFragment`, `Laaqf`, `Lkrl` | `enter/exitVoiceSettingsScope`, `updateOfficialSelection`, `…DefaultSelectionSuppression` |
| Language guard | `Lambj;->c(...)` | `LanguageDownloadGuard` на початку методу |
| Clipboard | `Lldx;->n(...)` | виклик `ClipboardSyncRuntime` після `iget-object …->f` |
| Spellchecker | `Laieo;->a(Context,TextServicesManager)Z` | SDK-перевірка на початку |
| Emoji / accessibility | `AutoCenterScaleTextView`, `AccessPointsPanelV2` | по одному/двох викликах runtime |
| Package rename | `aapt2 dump badging` | `package: com.akshaykadam.pixelboard` |
| Settings entry | `resources.arsc` | `string/pixelboard_patches_settings_title` присутній |

### 3.1 Хуки, які на 18.5.3 свідомо не застосовуються

Ці target-и відсутні в 18.5.3, патчі самі пропускають їх
(`findMutableMethodOrNull == null`) — жодних помилок чи пошкодженого APK:

* `GboardAiWritingToolsFlagValuePatch` — 18.0.3-хуки (`Lrzb;->v()`,
  `Lgwf;->a(Lgwo;)`, `Lifn;->c/d`, `NgaInputManager->e()`). Writing Tools
  працюють через форс прапорців.
* `GboardAdvancedVoice1803ZhTwPatch` — `zh-TW` Advanced Voice.
* `suppressRamblerFeedbackAccessPoint()` — провайдер переїхав у `Lkja;` і
  резолвиться через `Lator;->i(String)`; хук лише прибирав би
  «compliance»-іконку зворотного зв'язку.

## 4. Процедура порту на нову версію

### Крок 0. Підготувати APK і зняти smali

```bash
aapt2 dump badging input/gboard.apk | head -1
apktool d -f -o /tmp/gboard-new input/gboard.apk     # smali + res
```

Записати `versionName` у форматі `<versionName>-<release|beta>-arm64-v8a`.

### Крок 1. Додати версію до admission

`GboardTargetBuildCompiler.kt` → `DEFAULT_ARTIFACT_IDENTITY.versionNames`,
потім `patches/src/main/resources/patches-list.json` (4 записи) і контрактні
тести в `patches/src/test/kotlin/.../registry/`.

### Крок 2. Знайти реальні цілі

Шукати **за сигнатурою + тілом**, а не за іменем класу: у новій версії стара
обфускована назва часто належить зовсім іншому класу. Мінімум, що потрібно:

1. signature bypass — метод `(Context,String)Z`, що викликає
   `MessageDigest`/`Arrays.equals` і має 3 `return`;
2. `VoiceSettingsFragment` — layout/setup/create/selection write/read/default;
3. геттер прапорців — `g()Ljava/lang/Object;` з полем-іменем і полем-значенням;
4. `LanguageDownloadQueue.enqueue` — метод із рядком `"languageTag"`;
5. clipboard — `onStartInputView`-метод з полем-обробником кліпборда.

### Крок 3. Додати target-и

* `GboardSignatureBypassBytecodePatch.kt` — `SIGNATURE_UTILS_CLASS_*`,
  `DIGEST_METHOD_DESCRIPTOR_*`, `TARGET_FIELD_DESCRIPTORS_*`, гілки в
  `injectSignatureBypass()`, `classifyGboardSignatureBypass()`,
  `isExactGboardSignatureBypassTarget()`;
* `GboardRambler1803OfficialSelectorPatch.kt` — `RAMBLER_*_TARGETS` + перевірка
  в `when` **перед** старішими версіями;
* `GboardFlagFamilyComposer.kt` — `targetMethod*` / `targetField*` першими в
  `when`, за потреби `isFormal1803ResultMaterialization`;
* `GboardAdvancedVoice1831LanguageDownloadGuardPatch.kt`,
  `GboardClipboardSyncPatch.kt` — нові `GboardMethodTarget`;
* runtime-фолбеки в
  `extensions/.../rambler/GboardRambler1803OfficialSelectionRuntime.java`.

Не послаблювати перевірки форми: краще «тихо не застосовано», ніж зламаний APK.

### Крок 4. Верифікація

```bash
# Тести (JDK 21: ASM 9.7.1 / Robolectric 4.14.1 не читають новіші class-файли)
./gradlew test

# Бандл
./gradlew buildAndroid

# Патчинг + розбір результату
cp /path/to/gboard.apk input/gboard.apk
./build.sh --rebuild
apktool d -r -f -o /tmp/patched output/GBoard.apk
# і grep по слідах runtime-викликів, як у §3
```

Смоук-тест на пристрої:

- [ ] застосунок встановлюється поруч зі стоковим Gboard;
- [ ] `Налаштування → Голосовий ввід` показує Rambler, перемикач зберігається
      після перезапуску;
- [ ] довге натискання на текст → Writing Tools відкриваються;
- [ ] перевірка орфографії показує chips на Android 12+;
- [ ] у логах немає `Expected exactly one signature bypass target` та
      `Malformed ... scope`.

## 5. Чому тести краще запускати на JDK 21

`./gradlew buildAndroid` працює на JDK 21 і 25. На JDK 27 `jlink` падає на
`core-for-system-modules.jar` (проблема середовища, не проєкту).
Тести використовують ASM 9.7.1 і Robolectric 4.14.1, які падають на
class-файлах новіших JDK — CI upstream використовує Temurin 21, цей проєкт
дотримується того ж підходу.

## 6. Куди оновлювати документацію

1. `README.md` — таблиця базових версій;
2. `gradle.properties` — комментар про цільову версію;
3. `docs/ANALYSIS-UK.md` — таблиці обфускованих класів;
4. `patches/PixelBoard.mpp` + `patches/patches-<version>.mpp` — публікуються
   автоматично після `./gradlew buildAndroid` (задача `syncPublishedBundle`);
   разом з ними синхронізуються `patches/patches-bundle.json` і
   `patches/patches-list.json`.
