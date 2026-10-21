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
PUBLISHED_MPP="patches/PixelBoard.mpp"
PREBUILT_MPP="prebuilt/PixelBoard-1.2.0.mpp"

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
"$JAVA_BIN" -jar tools/patcher.jar patch \
    --patches="$BUILT_MPP" \
    --options-file=options.json \
    --striplibs=arm64-v8a \
    --signer="GBoard" \
    --keystore="$KEYSTORE_PATH" \
    --keystore-password=gboard \
    --keystore-entry-alias=gboard \
    --keystore-entry-password=gboard \
    --temporary-files-path="tools/patcher-data/tmp" \
    -o=output/GBoard.apk \
    -r=output/patching-result.json \
    input/gboard.apk

cp output/GBoard.apk "output/GBoard-${PATCH_VERSION}.apk"
echo "Done: output/GBoard.apk and output/GBoard-${PATCH_VERSION}.apk"
