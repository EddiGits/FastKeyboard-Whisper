#!/bin/bash

echo "🚀 Building FastKeyboard APK..."
start_time=$(date +%s)

# Get current directory
dir=$(pwd)

# Clean previous build
echo "🧹 Cleaning previous build..."
rm -rf build
mkdir -p build/classes

# Step 1: Compile resources
echo "📦 Compiling resources..."
aapt2 compile --dir res -o build/resources.zip
if [ $? -ne 0 ]; then
    echo "❌ Resource compilation failed"
    exit 1
fi

# Step 2: Link resources
echo "🔗 Linking resources..."
aapt2 link -I $dir/toolz/android.jar \
  --manifest AndroidManifest.xml \
  --java build/ \
  -o build/link.apk \
  build/resources.zip \
  --auto-add-overlay
if [ $? -ne 0 ]; then
    echo "❌ Resource linking failed"
    exit 1
fi

# Step 3: Compile Java code
echo "☕ Compiling Java code..."
$JAVA_HOME/bin/javac --release=8 \
  -d build/classes \
  --class-path $dir/toolz/android.jar \
  src/com/fastkeyboard/MainActivity.java \
  src/com/fastkeyboard/SettingsActivity.java \
  src/com/fastkeyboard/FastKeyboard.java \
  src/com/fastkeyboard/WhisperAPI.java \
  src/com/fastkeyboard/AudioRecorder.java \
  build/com/fastkeyboard/R.java
if [ $? -ne 0 ]; then
    echo "❌ Java compilation failed"
    exit 1
fi

# Step 4: Convert to DEX
echo "🔄 Creating DEX file..."
cd build/classes
dx --dex --output=../../classes.dex com/fastkeyboard/*.class
if [ $? -ne 0 ]; then
    echo "❌ DEX creation failed"
    cd ../..
    exit 1
fi
cd ../..

# Step 5: Add DEX to APK
echo "📦 Packaging APK..."
zip -u build/link.apk classes.dex > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ APK packaging failed"
    exit 1
fi

# Step 6: Align APK
echo "⚡ Aligning APK..."
zipalign -f -p 4 build/link.apk build/aligned.apk
if [ $? -ne 0 ]; then
    echo "❌ APK alignment failed"
    exit 1
fi

# Step 7: Sign APK
echo "🔐 Signing APK..."
apksigner sign \
  --ks key.keystore \
  --ks-pass pass:password \
  --out final.apk \
  build/aligned.apk
if [ $? -ne 0 ]; then
    echo "❌ APK signing failed"
    exit 1
fi

# Calculate build time
end_time=$(date +%s)
build_time=$((end_time - start_time))

echo ""
echo "✅ Build successful!"
echo "📱 APK: $dir/final.apk"
echo "⏱️  Build time: ${build_time} seconds"
echo ""
echo "To install:"
echo "  cp final.apk ~/storage/downloads/"
echo "  Then tap the notification to install"
echo ""
echo "After installing:"
echo "  1. Go to Settings → System → Languages & input → On-screen keyboard"
echo "  2. Enable 'Fast Keyboard'"
echo "  3. Tap any text field"
echo "  4. Select Fast Keyboard from keyboard picker"
