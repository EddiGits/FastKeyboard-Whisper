# VoiceKeyboard - Voice-Only AI Keyboard

**Transformed from FastKeyboard**: All QWERTY keys removed, now 100% voice-powered with all VoiceOverlay features!

## ✨ What Changed

### Removed:
- ❌ All QWERTY letter keys
- ❌ Number keys
- ❌ Space, Enter, Delete buttons
- ❌ Traditional keyboard layout

### Added (All from VoiceOverlay):
- ✅ **Text Improvement** - ChatGPT-powered text enhancement
- ✅ **Voice Edit Mode** - Record voice instructions to edit text
- ✅ **Recording History** - Full history with audio file storage
- ✅ **History Search** - Search through past transcriptions
- ✅ **Audio Quality Settings** - Low/Medium/High (16kHz to 44kHz)
- ✅ **Multiple Whisper Models** - whisper-1, gpt-4o-audio-preview, gpt-4o-mini-audio-preview
- ✅ **Custom Prompts** - Customizable transcription instructions
- ✅ **Improve While Recording** - Click Improve/Voice Edit while recording
- ✅ **Clear/Paste Functionality** - Text manipulation
- ✅ **Modern UI** - Dark theme with cards

## 🎯 Features

### Voice Recording
- 🎤 Tap "Start Recording" to begin
- 🔴 Live recording timer
- ⏹ "Append" to add to existing text
- ⊗ "Cancel" to abort

### Text Enhancement
- ✨ **Improve**: AI-powered grammar and style improvements
- 🎙 **Voice Edit**: Record instructions like "make it more professional"
- 🗑 **Clear**: Wipe text area
- 📋 **Paste**: Insert text into active app

### History Management
- 📜 View all past transcriptions
- 🔍 Search through history
- 📁 Audio files saved for each recording
- 📋 Copy/Use/Delete individual entries
- 🗑 Clear all history

### Settings
- 🔑 API Configuration (OpenAI)
- 🎙️ Transcription prompt customization
- 🎚️ Audio quality (Low/Medium/High)
- 🤖 Whisper model selection

## 📦 Build Info

**APK Size**: 33KB
**Build Time**: ~4 seconds
**Min SDK**: Android 26+ (Android 8.0)
**Build Method**: Manual (aapt2 + javac + dx)

## 🚀 Quick Start

### 1. Build & Install
```bash
cd FastKeyboard
bash build.sh
cp final.apk ~/storage/downloads/
# Tap notification to install
```

### 2. Configure
1. Open **VoiceKeyboard** app
2. Tap "⚙️ Configure Voice Settings"
3. Enter OpenAI API URL and Key
4. Save settings

### 3. Enable Keyboard
1. Settings → System → Languages & input
2. On-screen keyboard → Manage keyboards
3. Enable "Voice Keyboard"
4. In any app, select Voice Keyboard

### 4. Use
1. Tap "🎤 Start Recording"
2. Speak your message
3. Tap "⏹ Append" to transcribe
4. Tap "📋 Paste" to insert into app

## 🎨 UI Layout

```
┌─────────────────────────────────────┐
│   ⚙ Settings  │ 📜 History │  Exit  │
├─────────────────────────────────────┤
│         ⚫ Ready / 🔴 Recording      │
├─────────────────────────────────────┤
│                                     │
│    [Transcription Text Area]        │
│                                     │
├─────────────────────────────────────┤
│  ⏳ Processing... (when active)      │
├─────────────────────────────────────┤
│  🎤 Start Recording  │  📋 Paste    │
│  ⊗ Cancel  │  ⏹ Append  │  📋 Paste │  (while recording)
├─────────────────────────────────────┤
│  🗑 Clear  │  ✨ Improve  │ 🎙 Edit  │
└─────────────────────────────────────┘
```

## 📂 File Structure

```
FastKeyboard/
├── src/com/fastkeyboard/
│   ├── MainActivity.java          # App launcher + settings button
│   ├── SettingsActivity.java      # Full settings UI
│   ├── VoiceKeyboard.java         # Main keyboard service (voice-only)
│   ├── WhisperAPI.java            # Whisper transcription
│   ├── ChatGPTAPI.java            # Text improvement
│   └── AudioRecorder.java         # Recording with quality settings
├── res/values/strings.xml
├── AndroidManifest.xml
├── build.sh
└── final.apk (33KB)
```

## 🔧 Key Classes

### VoiceKeyboard.java (Main Service)
- Extends `InputMethodService`
- No QWERTY layout - voice-only UI
- All features from VoiceOverlay integrated
- ~960 lines of code

### SettingsActivity.java
- Modern card-based UI
- API configuration
- Transcription settings
- Quality & model selection

### ChatGPTAPI.java
- Text improvement via gpt-4o-mini
- Voice edit instruction processing
- Async network calls

### WhisperAPI.java
- Audio transcription
- Supports custom prompts
- Multiple model support
- Multipart form data upload

### AudioRecorder.java
- Quality presets (Low/Medium/High)
- AAC encoding with M4A format
- Pause/resume support (API 24+)

## 📊 Comparison: Before vs After

| Feature | FastKeyboard (Before) | VoiceKeyboard (After) |
|---------|----------------------|----------------------|
| QWERTY Layout | ✅ Full keyboard | ❌ Removed |
| Voice Recording | ✅ Basic | ✅ Advanced |
| Text Improvement | ❌ No | ✅ ChatGPT |
| Voice Edit | ❌ No | ✅ Yes |
| History | ❌ No | ✅ Full with search |
| Audio Quality | ❌ Fixed | ✅ 3 levels |
| Model Selection | ❌ No | ✅ 3 models |
| Custom Prompts | ❌ No | ✅ Yes |
| APK Size | ~20KB | 33KB |

## 🎓 Technical Details

### Build Process
1. **aapt2 compile** - Resources to binary
2. **aapt2 link** - Generate R.java
3. **javac** - Compile to .class
4. **dx** - Convert to DEX (with lambda support)
5. **zip** - Package APK
6. **zipalign** - Optimize
7. **apksigner** - Sign

### Lambda Support
- Uses Java 8 lambda expressions
- Requires dx with `--min-sdk-version=26`
- D8 had issues, using dx instead

### Android SDK Compatibility
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 30 (Android 11)
- Required for lambda expressions in dx

## 🔐 Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 💡 Usage Examples

### Basic Transcription
1. Tap "🎤 Start Recording"
2. Say: "Hello world, this is a test"
3. Tap "⏹ Append"
4. Wait 2-3 seconds
5. Text appears!
6. Tap "📋 Paste"

### Text Improvement
1. Record: "hey how r u doing"
2. Tap "✨ Improve"
3. Result: "Hey, how are you doing?"

### Voice Edit
1. Have text: "The cat is big"
2. Tap "🎙 Voice Edit"
3. Tap "🎤 Record Instructions"
4. Say: "Replace cat with dog and big with small"
5. Tap "⏹ Stop & Apply"
6. Result: "The dog is small"

## 🐛 Troubleshooting

### "API not configured"
- Open VoiceKeyboard app
- Configure API settings
- Save and retry

### Microphone permission denied
- Settings → Apps → VoiceKeyboard
- Permissions → Microphone → Allow

### Text doesn't paste
- Make sure text field is active
- Check InputConnection is available
- Try tapping paste again

### Improve/Voice Edit fails
- Check API key is valid
- Ensure internet connection
- Check OpenAI API credits

## 📈 Future Enhancements

- [ ] Offline Whisper model
- [ ] Firebase backend integration
- [ ] Custom wake word detection
- [ ] Language selection
- [ ] Voice shortcuts
- [ ] Cloud sync of history

## 🙏 Credits

Built entirely in Termux on Android, combining:
- **VoiceOverlay** features (all recording/improvement/history features)
- **FastKeyboard** architecture (InputMethodService base)
- **OpenAI Whisper** for transcription
- **ChatGPT** for text improvements

---

**VoiceKeyboard**: The world's first 100% voice-only keyboard with AI! 🎤✨

No typing, just talking. Perfect for hands-free operation, accessibility, and the future of mobile input!
