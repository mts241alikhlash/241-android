# Kiosk / Device Owner Setup

## Kenapa perlu Device Owner

Tanpa Device Owner, Android **selalu** menampilkan dialog konfirmasi saat aplikasi memanggil
`startLockTask()`, dan siswa dapat menolaknya lalu keluar. Ini perilaku OS, bukan bug aplikasi.
Dengan Device Owner, pinning berjalan senyap dan tombol Back/Home/Recents diblokir.

## Menjadikan perangkat Device Owner

Perangkat harus **belum punya akun** (factory reset) — Android hanya mengizinkan provisioning
Device Owner pada perangkat bersih.

```bash
adb install app-debug.apk
adb shell dpm set-device-owner \
  id.sch.mts241alikhlash.exam/.kiosk.ExamDeviceAdminReceiver
```

Verifikasi:

```bash
adb shell dpm list-owners
# Device Owner: id.sch.mts241alikhlash.exam
adb shell dumpsys device_policy | grep -i "device owner"
```

## Perilaku saat Device Owner aktif

- `startLockTask()` berjalan tanpa dialog; tidak ada tombol "Unpin".
- Status bar dinonaktifkan selama ujian, keyguard dimatikan.
- Hanya paket aplikasi ini yang boleh masuk lock task (`setLockTaskPackages`).
- Saat ujian selesai, status bar dan keyguard dikembalikan.

## Tanpa Device Owner

Aplikasi tetap berjalan, tetapi:

- dialog pinning muncul dan bisa ditolak;
- siswa dapat keluar dan mencari jawaban.

Gunakan hanya untuk demo/uji internal.

## Menghapus Device Owner

```bash
adb shell dpm remove-active-admin \
  id.sch.mts241alikhlash.exam/.kiosk.ExamDeviceAdminReceiver
```

## Keluar darurat oleh pengawas

Tombol keluar darurat meminta akun guru dan kata sandi (role `TEACHER`) yang diperiksa ke
server. Perangkat harus online; tanpa koneksi kunci tidak bisa dilepas lewat aplikasi.
Percobaan yang ditolak dicatat sebagai `supervisor_exit_denied`, yang berhasil sebagai
`supervisor_exit`. PIN statis `SUPERVISOR_PIN` sudah dihapus dari build.

## Kamera ujian tanpa Device Owner

Build `proctor` menyalakan kamera depan selama sesi ujian berjalan. Uji coba memakai HP siswa
sendiri, yang **tidak** Device Owner. Akibatnya:

- status bar tetap terlihat, jadi notifikasi "Kamera ujian aktif" bisa dibaca siswa;
- Android 13 ke atas mengizinkan siswa menghentikan aplikasi dari notifikasi itu — kamera mati,
  dan kursi di layar pengawas memerah dalam 60 detik;
- HP yang di-reboot tidak menyalakan kamera lagi sampai aplikasi dibuka.

Yang **tetap** berlaku di HP biasa: menekan Home, membuka aplikasi lain, atau mematikan layar
tidak menghentikan kamera. Android 11 hanya melarang *memulai* foreground service kamera dari
background, bukan menjalankan yang sudah aktif.

Kamera hanya hidup selama sesi ujian; tidak ada perekaman, dan tidak ada kamera di luar ujian.
Lihat `TRIAL-CAMERA.md` untuk prosedur uji coba.
