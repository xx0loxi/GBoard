# Prebuilt patch bundle (upstream fallback)

`GBoard-1.2.0.mpp` — готовий бандл з upstream-релізу PixelBoard 1.2.0
(цільова версія Gboard 18.4.1, без підтримки 18.5.3). Використовується лише як
офлайн-фолбек у `build.sh`, якщо Gradle-збірка недоступна.

Основний шлях — збирати бандл з джерел; результат публікується в `patches/`:

```bash
./gradlew buildAndroid
# patches/GBoard.mpp            <- для Morphe Manager
# patches/patches-1.2.0.mpp
```

`patches-bundle.json` і `patches-list.json` у цій папці — метадані, що
відповідають upstream-бандлу 1.2.0 (не нашій збірці). Актуальні метадані
лежать у `patches/`.
