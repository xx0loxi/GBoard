#!/usr/bin/env bash
# ==============================================================================
# GBoard patch project - build & patch automation
#
# Upstream patch sources: PixelBoard by Akshay Kadam (@Akshayykadam)
#   https://github.com/Akshayykadam/PixelBoard (GPLv3)
#
# Usage:
#   ./build.sh            Build the .mpp (if missing) and patch input/gboard.apk
#   ./build.sh --rebuild  Force a Gradle rebuild of the patch bundle
#   ./build.sh --help     Show help
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD_PATCH=false
for arg in "$@"; do
    case "$arg" in
        -r|--rebuild|--compile|-c)
            REBUILD_PATCH=true
            ;;
        -h|--help)
            echo "Usage: ./build.sh [options]"
            echo ""
            echo "Options:"
            echo "  -r, --rebuild   Rebuild the patch bundle with Gradle before patching"
            echo "  -h, --help      Show this help message"
            echo ""
            echo "The patch bundle is written to patches/build/libs/patches-<version>.mpp."
            echo "To only compile the bundle run: ./gradlew buildAndroid"
            exit 0
            ;;
    esac
done

PATCH_VERSION=$(grep -E "^version\s*=" gradle.properties | cut -d'=' -f2 | tr -d ' ')
BUILT_MPP="patches/build/libs/patches-${PATCH_VERSION}.mpp"
PUBLISHED_MPP="patches/GBoard.mpp"
PREBUILT_MPP="prebuilt/GBoard-${PATCH_VERSION}.mpp"

if [[ "$REBUILD_PATCH" == true ]] || [[ ! -f "$BUILT_MPP" ]]; then
    echo "=== Compiling patch bundle from source ==="
    ./gradlew buildAndroid
fi

if [[ ! -f "$BUILT_MPP" ]]; then
    if [[ -f "$PUBLISHED_MPP" ]]; then
        echo "Note: using published patch bundle: $PUBLISHED_MPP"
        BUILT_MPP="$PUBLISHED_MPP"
    elif [[ -f "$PREBUILT_MPP" ]]; then
        echo "Note: falling back to bundled patch bundle: $PREBUILT_MPP"
        BUILT_MPP="$PREBUILT_MPP"
    else
        echo "Error: no patch bundle found. Run ./gradlew buildAndroid first."
        exit 1
    fi
fi

echo "Patch bundle: $BUILT_MPP"

JAVA_BIN=""
for candidate in \
    "${JAVA_HOME:-}/bin/java" \
    "$(command -v java 2>/dev/null || true)"; do
    if [[ -n "$candidate" && -x "$candidate" ]]; then
        candidate_ver=$("$candidate" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [[ "${candidate_ver:-0}" -ge 21 ]]; then
            JAVA_BIN="$candidate"
            break
        fi
    fi
done

if [[ -z "$JAVA_BIN" ]]; then
    echo "Error: patcher.jar requires Java 21 or newer. Set JAVA_HOME to a JDK 21+."
    exit 1
fi

if [[ ! -f "input/gboard.apk" ]]; then
    echo "Note: place a stock Gboard APK at input/gboard.apk to produce a patched APK."
    echo "      Supported base versions: 18.0.3, 18.3.1 (release/beta), 18.4.1 (release), 18.5.3 (beta)."
    exit 0
fi

mkdir -p output tools/patcher-data

KEYSTORE_PATH="tools/patcher-data/gboard.keystore"
if [[ ! -f "$KEYSTORE_PATH" ]]; then
    echo "Generating local signing keystore..."
    keytool -genkeypair \
        -keystore "$KEYSTORE_PATH" \
        -alias gboard \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass gboard \
        -keypass gboard \
        -dname "CN=GBoard Patches, OU=GBoard, O=GBoard, L=Unknown, ST=Unknown, C=US" \
        >/dev/null 2>&1
fi

echo "=== Applying patches to input/gboard.apk ==="
# Morphe's built-in signer converts the keystore to BKS through BouncyCastle,
# which JDK 25+ refuses to authenticate (unsigned shaded provider). Patch
# unsigned and sign with the SDK's apksigner against the JKS keystore instead.
"$JAVA_BIN" -jar tools/patcher.jar patch \
    --patches="$BUILT_MPP" \
    --options-file=options.json \
    --striplibs=arm64-v8a \
    --unsigned \
    --temporary-files-path="tools/patcher-data/tmp" \
    -o=output/GBoard-unsigned.apk \
    -r=output/patching-result.json \
    input/gboard.apk

SDK_DIR=""
if [[ -f local.properties ]]; then
    SDK_DIR=$(sed -n 's/^sdk\.dir=//p' local.properties | tr -d '\r' | tail -n 1)
fi
if [[ -z "$SDK_DIR" || ! -d "$SDK_DIR" ]]; then
    SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/AppData/Local/Android/Sdk}}"
fi
if command -v cygpath >/dev/null 2>&1; then
    SDK_DIR=$(cygpath -u "$SDK_DIR")
fi

APKSIGNER=""
if [[ -d "$SDK_DIR/build-tools" ]]; then
    BUILD_TOOLS=$(ls -1 "$SDK_DIR/build-tools" | sort -V | tail -n 1)
    if [[ -f "$SDK_DIR/build-tools/$BUILD_TOOLS/apksigner.bat" ]]; then
        APKSIGNER="$SDK_DIR/build-tools/$BUILD_TOOLS/apksigner.bat"
    elif [[ -f "$SDK_DIR/build-tools/$BUILD_TOOLS/apksigner" ]]; then
        APKSIGNER="$SDK_DIR/build-tools/$BUILD_TOOLS/apksigner"
    fi
fi
if [[ -z "$APKSIGNER" ]]; then
    echo "Error: apksigner not found. Set sdk.dir in local.properties or ANDROID_HOME."
    exit 1
fi

echo "=== Signing patched APK ==="
"$APKSIGNER" sign \
    --ks "$KEYSTORE_PATH" \
    --ks-pass pass:gboard \
    --ks-key-alias gboard \
    --key-pass pass:gboard \
    --out output/GBoard.apk \
    output/GBoard-unsigned.apk
rm -f output/GBoard-unsigned.apk

cp output/GBoard.apk "output/GBoard-${PATCH_VERSION}.apk"
echo "Done: output/GBoard.apk and output/GBoard-${PATCH_VERSION}.apk"
