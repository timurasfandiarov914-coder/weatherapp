# Building and downloading SOLEN for Android

The workflow in `.github/workflows/android.yml` builds the debug APK on pushes to `main` and pull requests targeting `main`.

To download a build, open **Actions → Build SOLEN Android APK**, select the latest successful run, and download the `SOLEN-Android-APK` artifact. The archive contains `SOLEN-preview.apk` and `SHA256SUMS.txt`.


The APK artifact is tied to the source revision used by the corresponding successful workflow run. After UI changes, use the newest successful run and verify its SHA-256 manifest before installing.
