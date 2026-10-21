# Prebuilt patch bundle

`PixelBoard-1.2.0.mpp` — готовий бандл з upstream-релізу PixelBoard 1.2.0
(цільова версія Gboard 18.4.1). Використовується як офлайн-фолбек у `build.sh`,
якщо Gradle-збірка недоступна.

Основний шлях — збирати бандл з джерел:

```bash
./gradlew buildAndroid
# patches/build/libs/patches-1.2.0.mpp
```

`patches-bundle.json` і `patches-list.json` у цій папці — метадані, що
відповідають upstream-бандлу 1.2.0.
