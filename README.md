# Wolf, a Wake-on-LAN controller for Android

Wake devices on your network, check if they are up, schedule wakes, scan for hosts, and put wake shortcuts on your home screen.

## Build it (no Android Studio needed)

1. Unzip the file. Open the unzipped folder until you see `app`, `build.gradle.kts` and `settings.gradle.kts`. (If you see another folder with the same name inside, go into it.)
2. Go to github.com/new, type a name, click **Create repository**.
3. Click the **uploading an existing file** link. Select everything INSIDE the folder (not the folder itself), drag it into the page, wait until it finishes, then click **Commit changes**.
4. Click the **Actions** tab. Wait 3 to 6 minutes for a green check. (If nothing starts, click the workflow name, then **Run workflow**.)
5. When the check is green, open the repo's **Releases** page (`github.com/<user>/<repo>/releases/latest`) and download `Wolf.apk`. Best: open that page in the browser on your phone. Install it and allow "install unknown apps" if Android asks.

## Updating later

Upload everything inside the new folder to the same repo (same names are replaced), then download the new build from Releases. The APK is signed with a fixed key (`app/debug.keystore`), so it installs over the old one and keeps your devices.

## Using it

- Your phone must be on the same Wi-Fi as the PC. Wake-on-LAN also needs to be switched on in the PC's BIOS and network adapter settings.
- Add a device: MAC address is required. Add a Host/IP to see online status.
- Tap a card to open all its actions.
