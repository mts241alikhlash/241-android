# Mode Kunci (Screen Pinning)

Aplikasi tidak memakai Device Owner, device admin, atau provisioning perangkat. Selama sesi
ujian aplikasi memanggil `startLockTask()` biasa sehingga Android masuk ke **screen pinning**
dan menampilkan pemberitahuan pinning. Tidak ada `dpm set-device-owner` dan tidak ada receiver
admin di build ini; perangkat siswa tidak perlu di-factory reset.

## Perilaku selama ujian

- Status bar dan navigation bar disembunyikan, layar tetap menyala (`FLAG_KEEP_SCREEN_ON`).
- Tombol Kembali diblokir di `MainActivity.onKeyDown`; Home dan Recents diblokir oleh pinning.
- Mode Jangan Ganggu dinyalakan selama sesi dan dikembalikan saat aplikasi berhenti. Panggilan
  dan mikrofon dipantau `AudioAndDisplayGuard`; panggilan menangguhkan sesi, bukan mengakhiri.
- `lockTaskModeState` diperiksa berkala. Selama pinning aktif nilainya `PINNED`; laporan ke
  server (`lockTaskActive`) bernilai benar untuk state apa pun selain `NONE`.
- Aplikasi tidak pernah memutuskan sendiri ujian dibuka atau selesai. Status ujian datang dari
  server (`lifecycle` pada daftar ujian).

## Pelanggaran

Melepas pin (swipe tahan tombol Kembali, lalu lepas) atau keluar dari aplikasi dicatat sebagai
pelanggaran. Batas pelanggaran mengikuti policy server (`violationLimit`, bawaan 3); pelanggaran
melewati batas mengumpulkan lembar jawaban dan mengunci sesi.

## Akhir ujian

Semua akhir ujian — dikumpulkan siswa, waktu habis, batas pelanggaran, sesi hilang, atau keluar
oleh pengawas — melewati satu jalur pelepasan: keluar dari lock task, DND dikembalikan, state
sesi, policy runtime, dan catatan pelanggaran dibersihkan, lalu aplikasi mendarat di Daftar
Ujian tanpa meminta token. Sesi yang di server sudah selesai (`lifecycle` ujian `FINISHED` di
`GET /me/exams`) juga langsung kembali ke Daftar Ujian saat aplikasi dibuka lagi.

## Keluar darurat oleh pengawas

Tombol keluar darurat meminta akun guru dan kata sandi (role `TEACHER`) yang diperiksa ke
server. Perangkat harus online; tanpa koneksi kunci tidak bisa dilepas lewat aplikasi. Percobaan
yang ditolak dicatat sebagai `supervisor_exit_denied`, yang berhasil sebagai `supervisor_exit`.
PIN statis `SUPERVISOR_PIN` sudah dihapus dari build.

## Kamera ujian

Build `proctor` menyalakan kamera depan selama sesi ujian berjalan. Uji coba memakai HP siswa
sendiri, yang bukan Device Owner. Akibatnya:

- status bar tetap bisa terlihat pada beberapa perangkat, jadi notifikasi "Kamera ujian aktif"
  bisa dibaca siswa;
- Android 13 ke atas mengizinkan siswa menghentikan aplikasi dari notifikasi itu — kamera mati,
  dan kursi di layar pengawas memerah dalam 60 detik;
- HP yang di-reboot tidak menyalakan kamera lagi sampai aplikasi dibuka.

Yang **tetap** berlaku di HP biasa: menekan Home, membuka aplikasi lain, atau mematikan layar
tidak menghentikan kamera. Android 11 hanya melarang *memulai* foreground service kamera dari
background, bukan menjalankan yang sudah aktif.

Kamera hanya hidup selama sesi ujian; tidak ada perekaman, dan tidak ada kamera di luar ujian.
Lihat `TRIAL-CAMERA.md` untuk prosedur uji coba.

## Batasan tanpa Device Owner

Karena tidak ada Device Owner, siswa masih dapat melepas pin dengan gestur sistem. Kejadian itu
terdeteksi dan dihitung sebagai pelanggaran, tetapi tidak bisa dicegah sepenuhnya oleh aplikasi.
Deployment yang menuntut kunci mutlak memerlukan kebijakan MDM/Device Owner di luar aplikasi
ini.
