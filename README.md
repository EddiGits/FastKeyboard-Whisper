# FastKeyboard - Android Keyboard with Whisper AI Voice Typing

A lightweight, fast-building Android keyboard with integrated OpenAI Whisper voice transcription.

## ✨ Features

- 🎹 **Full QWERTY Layout** - All letters, space, enter, delete
- 🎤 **Voice Typing** - OpenAI Whisper API integration
- ⏸️ **Pause/Resume** - Control your voice recording
- ⚙️ **In-App Settings** - Configure API credentials directly in the app
- ⚡ **Lightning Fast** - 5-10 second build time
- 📦 **Minimal Size** - ~20KB APK
- 🏗️ **Manual Build** - No Gradle, no Android Studio required

## 🎯 Voice Button Features

- **Top-right placement** - Easy to reach
- **Tap to record** - Button turns red
- **Pause button appears** - Tap ⏸ to pause, ▶ to resume
- **Tap stop (⏹)** to transcribe
- **Automatic text insertion** - Transcribed text appears instantly

## 📱 Keyboard Layout

```
                                    [⏸] [🎤]  ← Top-right
Q W E R T Y U I O P
A S D F G H J K L
Z X C V B N M ⌫
[      SPACE      ] ↵
```

## 🚀 Quick Start

### Prerequisites

- Termux on Android
- Basic Termux packages installed

### Build & Install

```bash
cd FastKeyboard
./build.sh
adb install -r final.apk
```

**Build time**: 5-10 seconds!

### Configure Voice Typing

1. Open **FastKeyboard** app from launcher
2. Tap **⚙️ Configure Voice Typing**
3. Enter:
   - **API URL**: `https://api.openai.com/v1/audio/transcriptions`
   - **API Key**: Your OpenAI API key (`sk-...`)
4. Tap **Save Settings**

### Enable Keyboard

1. Settings → System → Languages & input
2. On-screen keyboard → Manage keyboards
3. Enable **Fast Keyboard**
4. In any app, tap text field → Select **Fast Keyboard**

## 🎤 Using Voice Typing

1. Tap **🎤** (top-right corner)
2. Speak your message
3. Tap **⏸** to pause (optional)
4. Tap **▶** to resume (if paused)
5. Tap **⏹** to stop and transcribe
6. Wait 2-3 seconds → Text appears!

## 📁 Project Structure

```
FastKeyboard/
├── src/com/fastkeyboard/
│   ├── MainActivity.java          # App launcher with settings button
│   ├── SettingsActivity.java      # API configuration screen
│   ├── FastKeyboard.java          # Main keyboard service
│   ├── WhisperAPI.java            # Whisper API client
│   └── AudioRecorder.java         # Audio recording wrapper
├── res/
│   ├── values/strings.xml         # App strings
│   ├── xml/method.xml             # Keyboard settings
│   └── drawable/ic_launcher.xml   # App icon
├── AndroidManifest.xml            # App configuration
├── build.sh                       # Fast build script
└── toolz/android.jar              # Android SDK classes
```

## 🔧 Build Process

The `build.sh` script performs these steps:

1. **aapt2 compile** - Compile resources to binary
2. **aapt2 link** - Generate R.java
3. **javac** - Compile Java source files
4. **dx** - Convert to DEX bytecode
5. **zip** - Package into APK
6. **zipalign** - Optimize APK
7. **apksigner** - Sign for installation

**Total time**: 5-10 seconds

## 🛠️ Development

### Prerequisites in Termux

```bash
pkg install openjdk-17 aapt apksigner dx zipalign
```

### Make Changes

```bash
# Edit source code
nano src/com/fastkeyboard/FastKeyboard.java

# Build (5-10 seconds)
./build.sh

# Install
adb install -r final.apk
```

### Customization Ideas

- Add numbers row (0-9)
- Add shift key for uppercase
- Add punctuation keys
- Custom themes/colors
- Multiple language support
- Word predictions

## 📊 Performance

| Metric | Value |
|--------|-------|
| Build time | 5-10 seconds |
| APK size | ~20 KB |
| Java files | 5 classes |
| Features | Full keyboard + Voice |

Compare to:
- Expo: 15-20 minutes build time
- Gradle: 2-5 minutes build time

## 🔐 Permissions

- **RECORD_AUDIO** - Voice input
- **INTERNET** - Whisper API calls
- **READ/WRITE_EXTERNAL_STORAGE** - Audio file handling

## 🎓 How It Works

### Voice Recording
1. Uses Android `MediaRecorder`
2. Records in M4A format (AAC codec, 44.1kHz)
3. Saves to app cache directory
4. Auto-deletes after transcription

### Whisper API Call
1. Reads API URL and key from SharedPreferences
2. Creates multipart/form-data request
3. Uploads audio file
4. Receives JSON response with transcription
5. Extracts text and inserts via InputConnection

### Keyboard Architecture
- Extends `InputMethodService`
- Implements `KeyboardView.OnKeyboardActionListener`
- Uses `LinearLayout` for button grid
- `InputConnection` for text insertion

## 📝 API Configuration

### OpenAI Whisper

**URL**: `https://api.openai.com/v1/audio/transcriptions`

**Get API Key**: https://platform.openai.com/api-keys

**Cost**: ~$0.006 per minute of audio

### Custom Whisper Server

You can use any Whisper-compatible API:

```
URL: YOUR_SERVER_URL/v1/audio/transcriptions
Key: YOUR_API_KEY
```

## 🐛 Troubleshooting

### Voice button doesn't respond
- Check microphone permission in Settings → Apps → FastKeyboard
- Grant via: `adb shell pm grant com.fastkeyboard android.permission.RECORD_AUDIO`

### "HTTP Error 400"
- Check API URL is correct
- Verify API key is valid
- Check audio file format is supported

### "API not configured"
- Open FastKeyboard app
- Tap "Configure Voice Typing"
- Save your credentials

### Toast messages blocked
- Android may suppress frequent toasts
- Check logcat for actual errors: `adb logcat | grep FastKeyboard`

## 📚 Related Guides

- `VOICE-TYPING-GUIDE.md` - Detailed voice setup guide
- `SETUP-GUIDE.md` - Configuration instructions
- `FAST-BUILD-OPTIONS.md` - Build speed comparison

## 🚀 Future Enhancements

- [ ] Numbers row
- [ ] Shift key (uppercase)
- [ ] Punctuation keys
- [ ] Custom themes
- [ ] Settings from keyboard
- [ ] On-device Whisper (offline)
- [ ] Auto-capitalization
- [ ] Word suggestions
- [ ] Swipe typing

## 🤝 Contributing

Contributions welcome! This is a learning project built to demonstrate:
- Fast Android development without Android Studio
- Manual APK building in Termux
- Voice AI integration
- Minimal dependencies

## 📄 License

MIT License - See LICENSE file

## 🙏 Acknowledgments

- Built entirely in Termux on Android
- Inspired by the need for fast iteration during development
- Uses OpenAI Whisper for transcription
- Based on proven build pipeline from various Android projects

## 📞 Support

For issues or questions:
- Check the documentation files in the repository
- Review logcat output: `adb logcat | grep FastKeyboard`
- Open an issue on GitHub

---

**Built with ❤️ in Termux**

Build time: 5-10 seconds | APK size: ~20KB | Pure Java, no dependencies
