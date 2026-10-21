# Prebuilt patch bundle

`GBoard-1.3.3.mpp` — готовий бандл GBoard 1.3.3 для Gboard 18.5.3
(`18.5.3.993608717-beta-arm64-v8a`). Використовується лише як офлайн-фолбек
у `build.sh`, якщо Gradle-збірка недоступна.

Основний шлях — збирати бандл з джерел; результат публікується в `patches/`:

```bash
./gradlew buildAndroid
# patches/GBoard.mpp            <- для Morphe Manager
# patches/patches-1.3.3.mpp
```

`patches-bundle.json` і `patches-list.json` у цій папці — копія метаданих
бандла 1.3.3. Актуальні метадані лежать у `patches/`.
