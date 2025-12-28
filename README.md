<div align="center">

# SpotifyAlarm

</div>

A simple alarm application that uses Spotify to play music when triggered. If Spotify is unavailable, it falls back to a local phone alarm.

---

## 🎵 Features

- 🎧 Plays music from Spotify using the Spotify SDK  
- 🎁 Fetches user's Spotify library using Spotify API to select alarm tracks  
- 🔄 Fallback to local phone alarm if no internet/Spotify connection  
- 📱 Java-based Android app (Android Studio compatible)

## 🚀 Getting Started

### Prerequisites
- Android Studio (for Java development)  
- Spotify Developer Account (to obtain API credentials)  
- Spotify app installed on the device  

### Steps
1. **Clone the Repository**  
     ```bash
     git clone https://github.com/tangvdv/SpotifyAlarmApp.git
     ```
2. **Set Up Spotify SDK**

    Download the [Spotify SDK release](https://github.com/spotify/android-sdk/releases)
   
    Add Spotify dependencies to your build.gradle file:

    ```bash
    implementation files('libs/spotify-app-remote-release-<SDK_VERSION>.aar')
    ```
    ```bash
    implementation 'com.spotify.android:auth:<DEPENDENCY_VERSION>'
    ```
  
4. **Configure Spotify Credentials**

   Register your app on [Spotify Developer Dashboard⁠](https://developer.spotify.com/dashboard)

   Obtain ```Client ID```, ```Client Secret``` and add a ```Redirect URI```

   Replace placeholders in ```SpotifyConstants.java``` with your credentials
   
6. **Build and Run**
   
    Open the project in Android Studio and run it on a device with Spotify installed.

## 📱 Usage
1. **Select a playlist**

    Click on the button 'Select music' to select a playlist, album or artist from your Spotify library.

2. **Set an alarm**

4. **Trigger Alarm**
    When the alarm goes off:
      - It will attempt to play the selected playlist via Spotify.
      - If Spotify is unavailable (no internet or app crash) or if no playlist is found, it will play a local alarm sound.
  
## Overview

<img width="193" height="432.5" alt="spotify_alarm_main" src="https://github.com/user-attachments/assets/b7d7b609-04ab-4eb4-bb96-0c4825c28c74" />
<img width="193" height="432.5" alt="spotify_alarm_library" src="https://github.com/user-attachments/assets/6479b0c1-f17a-4d08-a2f0-d9be7b6ec68a" />


## License

This project is Licensed under the [MIT](/LICENSE) License.

#SpotifyAlarm #AlarmApp #MusicAlarm #AndroidDev
