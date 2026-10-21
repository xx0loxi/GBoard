# Аналіз PixelBoard 1.2.0 → інтеграція в GBoard

Документ описує, як саме працюють патчі, перенесені з
[PixelBoard](https://github.com/Akshayykadam/PixelBoard) (commit `9f7dcb0`, версія
патчів 1.2.0), які прапорці та сховища форсуються, і що потрібно для переходу на
Gboard 18.5.3.

---

## 1. Архітектура репозиторію

Три рівні, які не можна змішувати:

```
patches/src/main/kotlin/...        Kotlin: опис патчів (smali-трансформації)
        │  dependsOn(...)
        ▼
extensions/extension/src/main/java Java: runtime-код, що вшивається в APK
        │  ABI: RuntimeAbiCatalog (рядки-дескриптори)
        ▼
Gboard DEX                       виклики extension-методів з патченого коду
```

1. **Реєстр** — `gboard/registry/GboardPatchRegistry.kt`. Публічні патчі
   (`Advanced Voice Typing`, `AI Writing Tools`, `Add Gboard Signature Bypass`,
   `Package Rename`) створюються через `gboardPublicResourcePatch()`, який бере
   список «допущених» contribution-ів із згенерованого `GboardTargetAdmission`.
   Якщо contribution не допущено — патч отримує `name = null` і не публікується.
2. **Wiring** — `GboardContributionWiring.kt` + `verifyGboardCapabilityWiring`.
   Кожен contribution-ID має бути підключений до конкретних patch-об'єктів, інакше
   збірка падає. Це захищає від «мертвих» фіч.
3. **Генерація** — `patches/src/bindingCompiler` читає
   `gboard-port-product-catalog.json`, `gboard-version-bindings.json` і генерує
   `GboardVersionBindings.kt` + `GboardTargetAdmission.kt` (версії, підписи, список
   contribution-ів). `gboard-port-product-catalog.sha256` фіксує вміст каталогу.
4. **Runtime ABI** — `shared/runtimeabi/RuntimeAbi.kt`: кожен виклик extension-у
   описаний повним smali-дескриптором. `RuntimeAbiVerifier` перевіряє, що сигнатури
   в DEX extension збігаються з патч-кодом, тому перейменування методів у Java
   без оновлення ABI ламає збірку (це добре).
5. **Вшивання extension** — `GboardPatchesExtensionCarrierPatch` читає
   `extensions/pixelboard-patches.rve` (DEX), злиливає класи в APK через
   `ClassMerger`. Окремих сервісів немає: `PatchedServiceBridge` — заглушка.

---

## 2. Gemini Rambler / Advanced Voice Typing

### 2.1 Що патчиться (`GboardRambler1803OfficialSelectorPatch`)

Ціль — `com.google.android.apps.inputmethod.latin.preference.VoiceSettingsFragment`.
Для кожної версії задані обфусковані імена (18.4.1):

| Роль | Ціль 18.4.1 |
| --- | --- |
| layout / setup / create | `VoiceSettingsFragment->aC()I`, `ac()V`, `f(Bundle)V` |
| запис вибору | `VoiceSettingsFragment->aE(Z,Lqou;,…)V` |
| читання вибору | `Lmvq;->a(Landroid/content/Context;)Z` |
| default selection | `Lfde;->hT()V` |

Патч вставляє виклики runtime:

* `enterVoiceSettingsScope()` / `exitVoiceSettingsScope()` — щоб форс долався лише
  всередині офіційного екрана голосового вводу;
* `updateOfficialSelection(Z)` — на кожному `return` методів читання/запису;
* `enterDefaultSelectionSuppression()` — блокує скидання вибору на дефолт.

Додатково `suppressRamblerFeedbackAccessPoint()` знаходить провайдер
`AgenticDictationFeedbackAccessPointProvider` і повертає `false`, прибираючи
«compliance»-іконку.

### 2.2 Прапорці

`GboardAdvancedVoice1803Policy` форсує в `true` (якщо значення `Boolean`):

```
enable_nga
enable_advanced_features_in_consolidated_sd_stack
enable_dictation_feature_split_install
handle_fallback_inside_sd_stack
immediately_end_dictation_on_keyboard_hidden
enable_soda_longform_experiment
```

`GboardRambler1803StockPolicy`:

* `show_rambler_dict_settings` → `true`;
* `ad_activation_type` → `2L` (Rambler-режим);
* агентні прапорці (`enable_agentic_dictation`, `config_agentic_dictation`,
  `enable_jetson_in_toolbar`, `enable_rambler_al_toolbar`, анімації, S3 quota,
  seamless fallback, parallel ASR тощо) → `true`, але **лише** якщо
  `GboardRambler1803OfficialSelectionRuntime.shouldEnableAgenticDictation()`
  підтверджує офіційний вибір.

### 2.3 `enable_jetson` і Device Protected Storage

`GboardRambler1803OfficialSelectionRuntime.resolveStockPreferences()`:

1. бере `context.createDeviceProtectedStorageContext()`, якщо це не вже DE-контекст;
2. читає `PreferenceManager.getDefaultSharedPreferences(deContext)`;
3. якщо ключа `enable_jetson` немає — **записує `true`**;
4. якщо ключ є зі значенням `false` — поважає вибір користувача
   (`hasExplicitlyDisabledPreference()`), Rambler не форсується.

Навіщо DE-storage: Gboard читає частину налаштувань до розблокування
(Direct Boot), а перейменований пакет не має синхронізації Phenotype з Play
Services. Запис у звичайний credential-protected storage там просто не видно.

### 2.4 Підміна значення прапорця

`GboardFlagFamilyComposer` + `GboardVersionBindings` перехоплюють **геттер
Phenotype-прапорця** і повертають підмінений результат:

| Версія | Клас геттера | Поле з ім'ям прапорця |
| --- | --- | --- |
| 18.0.3 | `Lnxw;` | `Lnxw;->a:Ljava/lang/String;` |
| 18.3.1 beta | `Lacps;` | `a` |
| 18.3.1 release | `Lobw;` | `a` |
| 18.4.1 release | `Lodx;` | `a` |

Патч вставляє після stock-результату виклик
`GboardAdvancedVoice1803Runtime.afterFlagValue(receiver, result)` /
`GboardAiWritingToolsRuntime.applyOverriddenFlagValue(...)`, які за іменем
прапорця вирішують, що повернути. Це **не** запис у базу Phenotype GMS — значення
підміняються в пам'яті, тому перезавантаження/оновлення GMS їх не скидає.

### 2.5 Інші хуки Advanced Voice

* `GboardAdvancedVoice1831LanguageDownloadGuardPatch` — `LanguageDownloadQueue`
  (`Lsqk;->c(String,Lsrc;,Consumer;)V` у 18.4.1): кожен мовний пакет ставиться в
  чергу один раз за процес (`LanguageDownloadGuard.shouldSkip`), інакше
  «тихе» завантаження нескінченно перезапускається.
* `GboardAdvancedVoice1803ZhTwPatch` — додає `zh-TW` до списку підтримуваних
  локалей і коригує formatter.
* `GboardAdvancedVoiceFeatureMarkerPatch` — маркер фічі в manifest +
  `<queries>` для `com.google.android.tts` і Scribe.

---

## 3. AI Writing Tools / Writing Tools V2

### 3.1 Прапорці (`GboardAiWritingToolsRuntime.computeOverrideValue`)

Безумовно → `true`:

```
writing_helper
writing_tools
config_proofread
writing_helper_on_selected_text
writing_helper_enable_text_stylization_internal
enable_writing_tools_cooperative_mode
enable_writing_tools_for_minors
enable_nga_lab_modeless_smartedit
enable_writing_tools_voice_commands
```

Залежно від налаштувань (`gboard_ai_writing_tools`):

* `enable_writing_tools_v2`, `enable_writing_tools_v2_on_toolbar`,
  `enable_writing_tools_suggest_style`, `writing_tools_v2`,
  `writing_tools_v2_enable_suggested_instructions*`,
  `writing_tools_v2_show_predefined_prompts`,
  `enable_writing_tools_my_style[ _plus]` → `true` тільки якщо
  `v2Enabled && GboardAiWritingToolsDeviceSupport.isSupportedDevice()`;
* `writing_tools_v2_backend_type` → `1` (`GBOARD_SERVER`), `2`
  (`PRIVATE_INFERENCE_AICORE`), `3` (`PRIVATE_INFERENCE_ASTREA`);
* `writing_tools_enable_hybrid`, `enable_on_device_proofread` — похідні від бекенду;
* `writing_helper_supported_language_tags` / `llm_pc_supported_language_tags` →
  `*` (усі мови), якщо увімкнено «all keyboards»;
* для перейменованого пакета додатково підставляються
  `writing_helper_server_target`, `gboard_imagen_api_host`, версії моделей
  `proofread` / `text_stylization`.

> У PixelBoard 1.2.0 **немає** прапорців з назвами `writing_tools_force_enable`,
> `enable_writing_helper`, `private_inference_aicore_enabled`. Їхні аналоги:
> `writing_tools` / `writing_helper` (форсуються в `true`) та
> `writing_tools_v2_backend_type = 2` для AiCore.

### 3.2 Bytecode-хуки

Додаткові хуки в `GboardAiWritingToolsFlagValuePatch.kt` прив'язані до **18.0.3**
і на інших версіях просто пропускаються (`findMutableMethodOrNull == null`):

| Хук | Ціль 18.0.3 |
| --- | --- |
| Auto-fix route | `Lrzb;->v()Lvvw;` → додає `INTENT_AUTO_FIX` |
| Auto-fix acceptance | `Lgwf;->a(Lgwo;)Llyg;` |
| GenAI voice-edit init | `Lifn;->c(NgaInputManager,Lhmi;)V` |
| SmartEdit init | `Lifn;->d()V` |
| GenAI client refresh | `NgaInputManager->e()V` |

Тобто на 18.4.1 Writing Tools працюють переважно через форс прапорців, а не через
ці п'ять ін'єкцій.

### 3.3 Налаштування та prefs

* `GboardAiWritingToolsSettings.PREF_FILE = "gboard_ai_writing_tools"`
  (`pref_ai_writing_tools_enabled`, `..._all_keyboards_enabled`,
  `..._backend_type`, `..._v2_enabled`).
* `GboardAiWritingToolsOfficialPreferences` — читає «офіційні» prefs Gboard для
  узгодження стану перемикачів.
* `GboardAiWritingToolsRuntime.initializeDefaultPreferences()` пише дефолти у
  **два** контексти: звичайний і `createDeviceProtectedStorageContext()`
  (`pref_key_enable_emoji_suggestion`, `spellchecker_enable_chips_ui`,
  `enable_mark_misspelled_words`, `pref_key_enable_grammar_checker`,
  `pref_key_auto_correction`, `show_suggestion_strip`, auto-paste/screenshot
  chips тощо).

---

## 4. GboardSpellCheckerChipsPatch

Клас заздалегідь невідомий (обфускація), тому пошук структурний:

1. Знайти клас, у якому є метод `(Context, TextServicesManager) -> boolean`.
2. У цьому методі (перевірка сумісності системного spell checker) вставити на
   початок:
   ```
   sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
   const/16 v1, 0x1f
   if-lt v0, v1, :continue
   const/4 v0, 0x1
   return v0
   ```
   Тобто на Android 12+ перевірка завжди `true`.
3. Знайти метод `(Context) -> boolean`, який викликає інший статичний
   `(Context) -> boolean` того ж класу (inline chips enablement), і замінити його
   тіло на делегування цьому методу.
4. Захист від повторного патчингу: перевірка, що третя інструкція вже `if-*` /
   `return`.

Патч залежить від extension-carrier, бо runtime пише
`spellchecker_enable_chips_ui = true` у prefs.

---

## 5. Package Rename (співіснування зі стоковим Gboard)

`GboardPackageRenameResourcePatch` (resource patch):

1. `AndroidManifest.xml`: `package="com.google.android.inputmethod.latin"` →
   `com.akshaykadam.pixelboard` (`Constants.GBOARD_PATCHED_PACKAGE_NAME`).
2. Заміна рядків у ресурсах/XML за списком `GBOARD_PACKAGE_RENAME_MAPPINGS`:
   * `applicationId`, `deeplink.*`, `.clipboard_content`, `.inputactionprovider`,
     `.swissarmyknifefileprovider`, `.fileprovider`, `.pixelbundle.RECEIVER`,
     `.tracing`, `.wdb`, `.mlkitinitprovider`, `.androidx-startup`;
   * дозволи `*.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`;
   * **Phenotype registration**:
     `com.google.android.gms.phenotype.registration.binarypb:<pkg>` і
     `...registration.xml:<pkg>`.
3. Опційні mapping-и (`.androidx-startup`, `.train.androidx-startup`) —
   застосовуються, якщо є в APK.
4. `applyGboardApplicationDisplayName()` змінює `android:label` (опція
   `app_display_name`, за замовчуванням `PixelBoard`).

Чому саме так: Gboard реєструє багато `ContentProvider`/`BroadcastReceiver` з
authority, похідними від пакета. Якщо змінити лише `package`, Android видасть
`INSTALL_FAILED_CONFLICTING_PROVIDER` або застосунок впаде на старті. Phenotype-
registration теж містить ім'я пакета, інакше GMS не знайде конфігурацію.

---

## 6. Add Gboard Signature Bypass

`GboardSignatureBypassBytecodePatch` шукає клас утиліт підпису за версією:

| Версія | Клас | Метод |
| --- | --- | --- |
| 18.0.3 | `Lrpv;` | `a(Context,String)Z` |
| 18.3.1 beta | `Lajpz;` | `a(Context,String)Z` |
| 18.3.1 release | `Lrvl;` | `a(Context,String)Z` |
| 18.4.1 release | `Lryi;` | `a(Context,String)Z` |

Далі `applyVerified` робить перевірку форми методу (8 регістрів, рівно 3
`RETURN`, один виклик `MessageDigest`/`Arrays.equals`, конкретні поля
`e/d/c:[B` + `Lrxk;->b:Z`, літерали `(v0,3) (v3,0) (v4,1) (v5,2)`) і лише потім
замінює кожен `return vX` на `const/4 vX, 0x1; return vX`. Якщо форма не збігається
— патч падає з помилкою, а не псує APK. Це і є «обхід перевірок підпису».

---

## 7. Флаги, SharedPreferences, Device Protected Storage — зведення

| Що | Де | Навіщо |
| --- | --- | --- |
| Phenotype flag getter | `Lodx;->g()` (18.4.1) + `afterFlagValue` | підміна значень у пам'яті, без запису в GMS |
| `enable_jetson` | stock default prefs у **DE storage** | Rambler вмикається до/без синку Phenotype |
| `gboard_patches_settings` | `GboardPatchesSettings`, DE storage + `moveSharedPreferencesFrom` | налаштування патчів доступні під час Direct Boot |
| `gboard_ai_writing_tools` | звичайний storage | вибір бекенду/V2/мов |
| дефолти Gboard | `initializeDefaultPreferences()` у звичайному **і** DE контексті | spellchecker chips, emoji, autocorrect, clipboard |
| Phenotype registration | manifest/resources rename | GMS має знайти конфіг для нового пакета |

Важливо: жоден патч не пише в базу Phenotype (`content://com.google.android.gms.phenotype/...`).
Усе форсування — на рівні геттера та SharedPreferences, тому воно ізольоване від
оновлень GMS і не потребує root.

---

## 8. Що змінено під час інтеграції

* Репозиторій `pixelboard-patches/` «піднято» в корінь проєкту, щоб
  `./gradlew buildAndroid` працював з робочої директорії.
* `already prebuilt` бандл перенесено в `prebuilt/` (не конфліктує з модулем
  `patches/`).
* Оновлено CI workflow, `.gitignore`, `build.sh` під нову структуру.
* Додано `docs/VERSION-TARGETS.md` з процедурою порту на 18.5.3.
* Код патчів і runtime не перейменовувались: пакет `com.akshaykadam.pixelboard`
  залишено як перевірений ідентифікатор (його зміна — окрема задача, див. §9).

## 9. Якщо потрібен власний package ID

Зміна `GBOARD_PATCHED_PACKAGE_NAME` у `Constants.kt` недостатня: від нього залежать
очікування тестів (`GboardPackageRenamePatchContractTest`), ABI extension-у,
`GboardAiWritingToolsRuntime.RENAMED_GBOARD_PACKAGE` та `PATCHED_PACKAGE`.
Порядок:

1. `Constants.kt` → `GBOARD_PATCHED_PACKAGE_NAME`.
2. `GboardAiWritingToolsRuntime` → `RENAMED_GBOARD_PACKAGE`.
3. Тести `*PackageRename*` і `GboardPublishedPatchCatalogContractTest`.
4. `options.json` / README / назва застосунку.
5. Повний `./gradlew test` + патчинг реального APK.

---

## 10. Джерела

* PixelBoard: <https://github.com/Akshayykadam/PixelBoard> (GPLv3).
* Morphe patcher: <https://github.com/MorpheApp>.
* Upstream-документація: `docs/upstream/`.
