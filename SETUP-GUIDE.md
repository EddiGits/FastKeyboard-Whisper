# FastKeyboard Setup Guide

## 🎤 Voice Button Not Working? Here's Why

The voice button needs **two things** to work:

### 1. Microphone Permission ✅ (Just Granted)

I just granted it via ADB. You can also grant it manually:

**Settings** → **Apps** → **FastKeyboard** → **Permissions** → **Microphone** → **Allow**

### 2. Whisper API Configuration ⚠️ (REQUIRED)

The voice button records your voice, but **needs Whisper API credentials to transcribe it**.

Currently the API URL and key are set to:
```
API_URL = "YOUR_WHISPER_API_URL/v1/audio/transcriptions"
API_KEY = "YOUR_API_KEY"
```

These are **placeholders** - they won't work until you configure them!

## 🔧 How to Configure Whisper API

### Option A: Edit Code Directly (5 minutes)

1. **Open the file**:
```bash
cd ~/FastKeyboard
nano src/com/fastkeyboard/WhisperAPI.java
```

2. **Find lines 11-12** and edit:

For **OpenAI Whisper**:
```java
private static final String API_URL = "https://api.openai.com/v1/audio/transcriptions";
private static final String API_KEY = "sk-proj-YOUR_ACTUAL_OPENAI_KEY_HERE";
```

For **Your transcribe-anywhere server**:
```java
private static final String API_URL = "https://your-server.com/v1/audio/transcriptions";
private static final String API_KEY = "your-forge-api-key";
```

3. **Save** (Ctrl+X, Y, Enter)

4. **Rebuild** (10 seconds):
```bash
./build.sh
```

5. **Reinstall**:
```bash
adb install -r final.apk
```

### Option B: Get OpenAI API Key

1. Go to https://platform.openai.com/api-keys
2. Click **"Create new secret key"**
3. Copy the key (starts with `sk-proj-...`)
4. Use it in Option A above

**Cost**: ~$0.006 per minute of audio (very cheap!)

### Option C: Use Your Existing Server

From your `transcribe-anywhere-mobile` repo, check your `.env` file:

```bash
cd ~/transcribe-anywhere-mobile
cat .env | grep FORGE
```

Look for:
- `BUILT_IN_FORGE_API_URL`
- `BUILT_IN_FORGE_API_KEY`

Use those values in Option A.

## 🧪 Testing After Setup

Once configured and rebuilt:

1. **Tap 🎤** - Should turn RED and show ⏹
2. **Speak** - "Testing one two three"
3. **Tap ⏹** - Should show "Transcribing..."
4. **Wait 2-3 seconds** - Text should appear!

## 🔍 Debugging

### Check if button responds at all:

```bash
adb logcat | grep -i "fastkeyboard\|recording\|whisper"
```

Then tap the button. You should see:
- "Recording..." when you tap 🎤
- "Transcribing..." when you tap ⏹
- Either success or error message

### Check permissions:

```bash
adb shell dumpsys package com.fastkeyboard | grep "RECORD_AUDIO"
```

Should show `granted=true`

### Test API configuration:

The keyboard will show toast messages:
- ✅ "Recording..." = Button works, permission granted
- ✅ "Transcribing..." = Audio recorded successfully
- ❌ "HTTP Error: 401" = Wrong API key
- ❌ "HTTP Error: 404" = Wrong API URL
- ✅ "Transcribed!" = SUCCESS!

## 📱 Current Keyboard Features

**Working Now**:
- ✅ QWERTY layout (all letters)
- ✅ Space bar
- ✅ Enter key
- ✅ Delete key (⌫)
- ✅ Voice button (🎤) - records audio
- ✅ Microphone permission - granted

**Needs Setup**:
- ⚠️ Voice transcription - needs Whisper API config

**Not Yet Implemented**:
- ⏳ Numbers (0-9)
- ⏳ Shift key (uppercase)
- ⏳ Punctuation (. , ! ?)
- ⏳ Emoji picker
- ⏳ Settings screen

## 🎯 Quick Setup (Copy-Paste)

If you have OpenAI API key:

```bash
cd ~/FastKeyboard

# Replace YOUR_KEY_HERE with your actual key
sed -i 's|YOUR_WHISPER_API_URL/v1/audio/transcriptions|https://api.openai.com/v1/audio/transcriptions|g' src/com/fastkeyboard/WhisperAPI.java
sed -i 's|YOUR_API_KEY|sk-proj-YOUR_KEY_HERE|g' src/com/fastkeyboard/WhisperAPI.java

# Rebuild and install
./build.sh
adb install -r final.apk
```

Just replace `sk-proj-YOUR_KEY_HERE` with your actual OpenAI API key!

## 📚 Files to Know

- **Configuration**: `~/FastKeyboard/src/com/fastkeyboard/WhisperAPI.java`
- **Build script**: `~/FastKeyboard/build.sh`
- **APK output**: `~/FastKeyboard/final.apk`
- **Full guide**: `~/FastKeyboard/VOICE-TYPING-GUIDE.md`

---

**Need help?** Let me know if:
- Voice button still doesn't respond
- You get specific error messages
- You want to use a different Whisper service
- You want me to add more features!
