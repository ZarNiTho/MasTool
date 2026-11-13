# FastDeck OTG — Android 10–16 ADB & Fastboot Service Tool

**FastDeck OTG** is a native Android USB Host (`UsbManager`) service utility for mobile technicians and developers, supporting **Android 10 to Android 16** devices over USB OTG.

## Key Features

- **Native USB OTG ADB & Fastboot Engine**:
  - Direct USB Bulk endpoint communication (`FF:42:01` ADB & `FF:42:03` Fastboot).
  - 2048-bit RSA keypair authentication (`A_CNXN`, `A_AUTH`, `A_OPEN`, `A_WRTE`, `A_CLSE`) for USB OTG ADB shell and reboot commands.
  - Fastboot protocol (`getvar`, `download`, `flash`, `erase`, `set_active`, `snapshot-update`, `reboot-fastboot`).
- **70+ Universal & Brand-Specific OEM Commands**:
  - **Universal (Android 10–16)**: Dynamic Partitions (`super`), Userspace `fastbootd`, Virtual A/B (`snapshot-update cancel/merge`), Logical Partitions (`create/resize/delete-logical-partition`), Active Slot `A/B` switching, and `init_boot` / `vendor_boot` / `vbmeta` support.
  - **Brand OEM Modules**: Xiaomi / Redmi / Poco, Huawei / Honor (`UPDATE.APP`), Vivo / iQOO (`bbk` / `vivo_bsp`), Oppo / Realme / OnePlus, Samsung, Google Pixel / Motorola, Infinix / Tecno / Itel, and Asus / Sony.
- **Direct Storage Firmware & Script Loader**:
  - Select Huawei `UPDATE.APP` (O(1) `FileChannel` header parser), `flash_all.bat` / `.sh` scripts, `.img` (Raw & Android Sparse Image `0xED26FF3A`), and `.zip` archives directly from device storage.
  - Automatic `AVB0` header flag patching (`--disable-verity --disable-verification`) for `vbmeta` images.
- **Workbench UI**:
  - Compact top dropdown selector with single **START** execution button and expanded **Live Log Console** (~75% screen height) with Light and Dark themes.

## GitHub Actions Automated Build

This repository includes `.github/workflows/android-build.yml` which automatically:
1. Configures **JDK 21**, **Android SDK**, and **Gradle 9.3.1**.
2. Decodes `debug.keystore.base64` and prepares release signing credentials.
3. Runs Robolectric unit tests (`gradle :app:testDebugUnitTest`).
4. Builds and uploads **Debug APK**, **Signed Release APK**, and **Release AAB** artifacts using `actions/upload-artifact@v4`.
