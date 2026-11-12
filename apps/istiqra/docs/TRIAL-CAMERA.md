# Uji coba kamera ujian

Panduan menjalankan uji coba kamera pengawas pada siswa di rumah, memakai HP mereka sendiri.

## Sebelum hari-H

### Yang harus disiapkan sekolah

1. **Persetujuan orang tua, tertulis.** Isinya harus menyebut dengan jelas:
   - kamera menyala **hanya** selama sesi ujian berjalan, dari saat siswa menekan mulai sampai
     menekan kumpulkan;
   - tidak ada rekaman; tayangan hanya dilihat langsung oleh guru pengawas;
   - siswa boleh menghentikannya kapan saja lewat notifikasi, dan itu akan terlihat oleh guru;
   - nama guru yang memantau dan berapa lama tayangan bisa diakses.

   Persetujuan yang diminta sebagai syarat ikut ujian bukan persetujuan yang sah. Kalau siswa
   menolak, sediakan jalan lain: ujian di sekolah dengan perangkat sekolah.

2. **Guru pengawas ditunjuk**, dan tahu apa yang harus dilakukan ketika sebuah kursi memerah.

3. **Server media disiapkan.** Lihat `platform-infra/docs/MEDIA.md`. Satu kelas cukup dengan
   satu kotak kecil; jangan taruh di VPS yang sama dengan API.

### Yang harus disiapkan teknis

- APK `proctor` debug: `./gradlew assembleProctorDebug`. APK ini **hanya** untuk kelompok uji;
  jangan disebar ke semua siswa.
- Alamat server di-set saat build:
  `./gradlew assembleProctorDebug -PAPI_BASE_URL=https://dev-assessment.mts241alikhlash.sch.id/`
- `STREAM_RTMP_URL` dan `STREAM_HLS_URL` di `exam-service` menunjuk ke kotak media.
- Migrasi database sudah dijalankan (`stream_nonce`, `last_seen_at`, `camera_blocked`).

## Saat ujian

1. Siswa membuka aplikasi, masuk, memilih ujian, memasukkan token seperti biasa.
2. Saat layar soal muncul, aplikasi meminta izin kamera. **Siswa harus menekan Izinkan.** Kalau
   ditolak, ujian tetap berjalan tetapi kursinya merah dan guru melihat "Izin kamera ditolak".
3. Guru membuka `assessment-web` → Jadwal Ujian → menu **Kamera** pada jadwal yang sedang
   berjalan. Satu kartu per siswa, urut NIS.
4. Setiap kartu menampilkan tayangan langsung, atau "Kamera mati" bila tidak ada kabar lebih dari
   60 detik, beserta alasannya bila diketahui.

## Membaca layar pengawas

| Yang terlihat | Artinya | Tindakan |
| --- | --- | --- |
| Tayangan hidup | Kamera berjalan | — |
| "Kamera mati" + Jaringan bermasalah | HP tidak bisa mengunggah | Hubungi siswa; minta pindah ke WiFi lain |
| "Kamera mati" + Izin kamera ditolak | Siswa menolak izin | Ujian tetap sah; catat, tindak lanjuti setelah ujian |
| "Kamera mati" + Dihentikan siswa | Siswa menekan Stop di notifikasi | Panggil siswa; ini pelanggaran tata tertib, bukan kegagalan teknis |
| "Kamera mati" + Tidak ada kabar | HP mati atau aplikasi dibunuh | Panggil siswa |
| "Keluar aplikasi: 2×" | Siswa keluar aplikasi dua kali | Sesuai tata tertib yang sudah berlaku |

## Yang harus disampaikan jujur ke sekolah

Pada HP siswa sendiri, ini **bukan penegakan, melainkan pengawasan dengan jejak pelanggaran
yang terlihat**. Siswa yang mau curang bisa menekan Stop di notifikasi; yang terjadi adalah
kursinya memerah dalam 60 detik, bukan kamera yang tidak bisa dimatikan. Penegakan penuh hanya
ada di perangkat sekolah yang di-provision sebagai Device Owner, dan itu di luar lingkup uji coba
ini.

Kamera juga tidak menyala lagi setelah HP di-reboot sampai aplikasi dibuka.

## Setelah uji coba

Kumpulkan: berapa kursi yang sempat merah, alasannya, berapa kali siswa menghentikan sendiri, dan
apakah ada keluhan orang tua. Angka itu yang menentukan apakah fitur ini diteruskan, diperbaiki,
atau dihentikan.
