# OPSI B - Build APK Tanpa Android Studio (Pakai HP saja bisa)

## Langkah 1: Buat Akun GitHub (1 menit)
1. Buka github.com di HP/komputer
2. Daftar gratis

## Langkah 2: Buat Repository Baru
1. Klik + (pojok kanan atas) -> New repository
2. Nama: hkpool2026-apk
3. Pilih Public
4. Centang Add a README file
5. Create repository

## Langkah 3: Upload File Project
1. Di repo baru, klik Add file -> Upload files
2. Buka file HKPoolAppFull.zip di komputer/HP kamu, extract dulu
3. Drag SEMUA file & folder dari dalam HKPoolAppFull ke GitHub upload area:
   - app/ folder
   - .github/ folder (PENTING! ini yang build APK)
   - settings.gradle
   - build.gradle
   - README_APK.md
4. Commit changes

## Langkah 4: Jalankan Build APK
1. Di repo kamu, klik tab Actions (di atas)
2. Jika diminta enable Actions, klik Enable
3. Di kiri, klik workflow "Build APK - HK Pool 2026"
4. Klik Run workflow -> Run workflow (tombol hijau)
5. Tunggu 3-5 menit (ada icon kuning berputar)

## Langkah 5: Download APK
1. Setelah selesai, icon jadi hijau centang
2. Klik pada run yang sukses
3. Scroll bawah, ada bagian Artifacts
4. Klik HKPool2026-debug-apk -> download zip
5. Extract zip -> dapat app-debug.apk

## Langkah 6: Install di HP Android
1. Kirim app-debug.apk ke HP (via WA, Telegram, atau kabel)
2. Buka file di HP -> Izinkan install dari sumber tidak dikenal
3. Install
