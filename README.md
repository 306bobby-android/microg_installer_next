# microG Installer Next

[![Trusted CI](https://img.shields.io/badge/Download-Trusted%20CI-238636?style=for-the-badge&logo=github&logoColor=white)](https://nightly.link/306bobby-android/microg_installer_next/workflows/trusted-ci/master?preview)

> **Notice**: This project officially only supports AOSP/Lineage-based ROMs with signature spoofing enabled. Other setups may work, but your mileage may vary (YMMV).

`microG Installer Next` is a Magisk / KernelSU / APatch module — based on Hieu Van's microG Installer and nift4's microG Installer Revived — that seamlessly installs microG GmsCore, GsfProxy, MapsV1, Companion (or real Play Store), and the microG Updater app directly to system privileged paths (`/system/priv-app` or `/system/product/priv-app`).

Currently, microG GmsCore up to `0.3.16.252432` (including Companion/FakeStore), Credential Manager framework permissions, GsfProxy `0.1.0`, and MapsV1 `0.1.0` are fully supported and granted system permissions.

The repository for this project is located at [https://github.com/306bobby-android/microg_installer_next](https://github.com/306bobby-android/microg_installer_next).

---

## How It Works

1. **System Priv-App Integration**: Flashing the module grants microG GmsCore, Companion, and the microG Updater app system privileged permissions (such as `FAKE_PACKAGE_SIGNATURE`, `INSTALL_PACKAGES`, `UPDATE_PACKAGES_WITHOUT_USER_ACTION`, and Android Credential Manager permissions).
2. **Existing microG Conversion**: If microG GmsCore or Companion are already installed on your user partition (`/data/app`), the module detects them during installation and automatically converts them to system privileged apps.
3. **Clean Installation & Seamless Updates**: If microG is not pre-installed on your ROM, the module installs the **microG Updater** system app. Launching the Updater app (or letting it run in the background) fetches and installs the latest official microG GmsCore & Companion APKs directly from GitHub releases.

---

## Installation

> **WARNING**: If you have official Google Play Services (GApps) currently installed on your ROM, **DO NOT INSTALL THIS MODULE**.

1. Choose a solution for [Signature Spoofing](https://github.com/microg/android_packages_apps_GmsCore/wiki/Signature-Spoofing) if your ROM does not have signature spoofing built-in (e.g. [whew-inc's FakeGApps fork](https://github.com/whew-inc/FakeGApps/releases)).
2. Flash `microG_Installer_Next.zip` in your root manager (Magisk, KernelSU, or APatch).
3. Reboot your device.
4. Open the **microG Updater** app to complete installing microG GmsCore and Companion, or to manage automatic background updates.

---

## microG Updater App

The module includes the built-in `microG Updater` system application (`org.microg.installer.updater`).

### Features:
- **Material 3 Interface**: Clean UI displaying installed vs. latest GitHub release versions for GmsCore (`com.google.android.gms`) and Companion (`com.android.vending`).
- **System Privileged Installer**: Uses Android's `PackageInstaller` with `INSTALL_PACKAGES` system privilege to perform silent initial installations and updates.
- **Background Worker & Settings**: Configurable background worker to periodically check for microG releases on GitHub, with options for silent auto-updates, check frequency, and Wi-Fi-only downloads.

---

## How do I get the real Play Store?

If you want real Play Store instead of microG Companion (FakeStore):
1. Download a non-bundle Play Store APK (e.g., from APKMirror — note `.apkm` bundles are not supported).
2. Install the Play Store APK as a normal user app before flashing the module.
3. Flash or reflash `microG Installer Next`. The installer script will detect the Play Store APK on `/data/app` and convert it into a system priv-app (`Phonesky.apk`).
4. Reboot and grant all requested permissions.

---

## Common Issues

- **Black screen / bootloop**: Don't use Magisk Delta's SuList. If you experience a bootloop, use [Magisk Safe Mode](https://topjohnwu.github.io/Magisk/faq.html#q-i-installed-a-module-and-it-bootlooped-my-device-help) to disable the module.
- **App misbehaves/crashes with missing microG overlay (e.g. Chromium-based browsers)**: Disable KSU Unmount modules from its app profile.
- **Location permissions**: Go to App Info > Permissions > Location > (press "Location access" in the warning dialog), then return to Self Check in microG settings to grant background location.

---

## Build Instructions

### Building microGUpdater APK
```bash
cd updater
./gradlew assembleDebug
```
The compiled APK will be at `updater/app/build/outputs/apk/debug/app-debug.apk`. Copy it to `system/priv-app/microGUpdater/microGUpdater.apk` and `system/product/priv-app/microGUpdater/microGUpdater.apk`.

### Packaging Module Zip
```bash
zip -9r microG_Installer_Next.zip META-INF system customize.sh module.prop README.md CHANGELOG androidacy-config.json LICENSE -x "*.git*" "updater/*"
```
Or build using Python:
```bash
python3 -c '
import zipfile, os
files_to_zip = ["META-INF", "system", "customize.sh", "module.prop", "README.md", "CHANGELOG", "androidacy-config.json", "LICENSE"]
with zipfile.ZipFile("microG_Installer_Next.zip", "w", zipfile.ZIP_DEFLATED) as z:
    for item in files_to_zip:
        if os.path.isfile(item):
            z.write(item)
        elif os.path.isdir(item):
            for root, dirs, files in os.walk(item):
                for f in files:
                    z.write(os.path.join(root, f))
'
```

---

## Credits

- **microG project** for their awesome work
- **306bobby-android** for `microG Installer Next`
- **nift4** for [microG Installer Revived](https://github.com/nift4/microg_installer_revived)
- **Hieu Van** for the original microG Installer
- **Fs00** for many bug fixes
- **chris42** and **FriendlyNeighborhoodShane** for privapp permission files
- **felinira**, **akaessens** and **soracqt** for contributing through pull requests
