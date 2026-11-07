package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.kiosk.ViolationPolicy

data class HelpTopic(
    val title: String,
    val points: List<String>,
)

val examRules: List<HelpTopic> =
    listOf(
        HelpTopic(
            "YANG PALING PENTING",
            listOf(
                "Jangan keluar dari aplikasi selama ujian. Setiap keluar atau kunci layar terlepas dihitung satu pelanggaran.",
                "Batasnya ${ViolationPolicy.LIMIT} kali. Pelanggaran ke-${ViolationPolicy.LIMIT + 1} membuat ujian langsung dikumpulkan dan jawaban yang belum selesai tidak bisa dilanjutkan.",
                "Jumlah pelanggaran tercatat di server. Menutup paksa aplikasi atau menghapus data aplikasi tidak mengembalikannya ke nol.",
            ),
        ),
        HelpTopic(
            "SEBELUM UJIAN",
            listOf(
                "Datang tepat waktu dan duduk di ruangan yang sudah ditentukan.",
                "Pastikan baterai perangkat cukup atau bawa pengisi daya.",
                "Minta token ujian kepada pengawas ruangan. Token hanya diberikan saat ujian akan dimulai.",
                "Satu akun hanya bisa dipakai di satu perangkat pada satu waktu.",
            ),
        ),
        HelpTopic(
            "SELAMA UJIAN",
            listOf(
                "Layar dikunci selama ujian berlangsung. Jangan membuka aplikasi lain atau menekan tombol keluar.",
                "Mematikan layar tidak dihitung sebagai pelanggaran. Keluar dengan izin pengawas juga tidak dihitung.",
                "Kerjakan sendiri. Bekerja sama atau menyalin jawaban adalah kecurangan.",
            ),
        ),
        HelpTopic(
            "JIKA ADA KENDALA",
            listOf(
                "Sinyal terputus: tetap jawab soal. Jawaban tersimpan di perangkat dan terkirim otomatis saat tersambung lagi.",
                "Perangkat bermasalah atau baterai habis: segera lapor ke pengawas. Pengawas dapat melepas sesi agar Anda melanjutkan di perangkat lain.",
                "Waktu terus berjalan sesuai jam sebenarnya, termasuk saat Anda melapor.",
            ),
        ),
        HelpTopic(
            "SETELAH UJIAN",
            listOf(
                "Tekan Selesai & Kumpulkan setelah semua soal dicek. Jawaban yang sudah dikumpulkan tidak bisa diubah.",
                "Jika waktu habis, jawaban otomatis dikumpulkan.",
                "Nilai diumumkan oleh sekolah sesuai ketentuan yang berlaku.",
            ),
        ),
    )

val appGuide: List<HelpTopic> =
    listOf(
        HelpTopic(
            "MASUK",
            listOf(
                "Isi NIS dan kata sandi dari sekolah, lalu tekan Masuk.",
                "Jika lupa kata sandi, minta bantuan wali kelas atau operator sekolah.",
            ),
        ),
        HelpTopic(
            "MELIHAT UJIAN",
            listOf(
                "Daftar Ujian menampilkan ujian yang sedang berlangsung, sudah dibuka, akan datang, dan selesai.",
                "Jadwal Ujian mengelompokkan ujian per jenis (UH, UTS, UAS, dan lainnya) dan per hari. Ketuk satu ujian untuk melihat detailnya.",
                "Tombol muat ulang di kanan bawah memperbarui daftar.",
            ),
        ),
        HelpTopic(
            "MEMULAI UJIAN",
            listOf(
                "Ketuk ujian yang berstatus Sedang dibuka.",
                "Masukkan token dari pengawas pada kolom Token ujian, lalu tekan Mulai Ujian.",
                "Layar akan dikunci. Tombol keluar darurat hanya untuk pengawas.",
            ),
        ),
        HelpTopic(
            "MENGERJAKAN SOAL",
            listOf(
                "Pilih atau isi jawaban. Jawaban tersimpan otomatis.",
                "Tekan Ragu-ragu untuk menandai soal yang ingin Anda periksa lagi.",
                "Gunakan Sebelumnya dan Selanjutnya untuk berpindah soal, atau buka Daftar Soal untuk loncat ke nomor tertentu.",
                "Waktu tersisa tampil di bagian atas. Ujian otomatis dikumpulkan saat waktu habis.",
            ),
        ),
        HelpTopic(
            "MENGUMPULKAN",
            listOf(
                "Tekan Selesai & Kumpulkan. Ringkasan menunjukkan jumlah soal terjawab dan soal yang masih ragu-ragu.",
                "Pilih Periksa Kembali untuk kembali mengerjakan, atau Ya, Kumpulkan untuk mengakhiri ujian.",
                "Jika ada jawaban yang belum terkirim, aplikasi mencoba lagi sebelum mengumpulkan.",
            ),
        ),
        HelpTopic(
            "PROFIL DAN KELUAR",
            listOf(
                "Profil Saya berisi data siswa, data ujian, data kelas, data diri, dan kontak. Hubungi sekolah bila ada data yang salah.",
                "Pilih Keluar di menu untuk mengakhiri sesi masuk Anda di perangkat ini.",
            ),
        ),
    )
