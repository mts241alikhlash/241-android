package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.kiosk.ViolationPolicy

data class HelpTopic(
    val title: String,
    val points: List<String>,
)

val examRules: List<HelpTopic> =
    listOf(
        HelpTopic(
            "Yang paling penting",
            listOf(
                "Jangan keluar dari aplikasi selama ujian atau tugas berlangsung. Setiap keluar atau kunci layar terlepas dihitung satu pelanggaran.",
                "Batasnya ${ViolationPolicy.LIMIT} kali. Pelanggaran ke-${ViolationPolicy.LIMIT + 1} membuat lembar jawaban langsung dikumpulkan dan tidak bisa dilanjutkan.",
                "Jumlah pelanggaran tercatat di server. Menutup paksa aplikasi atau menghapus data aplikasi tidak mengembalikannya ke nol.",
            ),
        ),
        HelpTopic(
            "Sebelum mengerjakan",
            listOf(
                "Pastikan baterai cukup atau bawa pengisi daya, dan izin kamera serta mode Jangan Ganggu sudah aktif.",
                "Minta token kepada pengawas bila ujian memakai token. Token hanya diberikan saat ujian akan dimulai.",
                "Satu akun hanya bisa dipakai di satu perangkat pada satu waktu.",
            ),
        ),
        HelpTopic(
            "Selama mengerjakan",
            listOf(
                "Layar dikunci selama sesi berlangsung. Jangan membuka aplikasi lain.",
                "Mematikan layar tidak dihitung sebagai pelanggaran. Keluar dengan izin pengawas juga tidak dihitung.",
                "Kerjakan sendiri. Bekerja sama atau menyalin jawaban adalah kecurangan.",
            ),
        ),
        HelpTopic(
            "Jika ada kendala",
            listOf(
                "Sinyal terputus: tetap jawab soal. Jawaban tersimpan di perangkat dan terkirim otomatis saat tersambung lagi.",
                "Perangkat bermasalah atau baterai habis: segera lapor ke pengawas. Pengawas dapat melepas sesi agar Anda melanjutkan di perangkat lain.",
                "Waktu terus berjalan sesuai jam sebenarnya, termasuk saat Anda melapor.",
            ),
        ),
        HelpTopic(
            "Setelah selesai",
            listOf(
                "Buka Daftar Soal, lalu tekan Selesai & Kumpulkan. Dialog konfirmasi menampilkan jumlah terjawab, ragu-ragu, dan belum dijawab.",
                "Jawaban tidak dapat diubah setelah dikumpulkan. Jika waktu habis, jawaban otomatis dikumpulkan.",
                "Nilai diumumkan oleh sekolah sesuai ketentuan yang berlaku.",
            ),
        ),
    )

val appGuide: List<HelpTopic> =
    listOf(
        HelpTopic(
            "Masuk",
            listOf(
                "Isi NIS dan kata sandi dari sekolah, lalu tekan Masuk.",
                "Jika lupa kata sandi, minta bantuan wali kelas atau operator sekolah.",
            ),
        ),
        HelpTopic(
            "Daftar dan jadwal",
            listOf(
                "Daftar Ujian menampilkan pekerjaan yang sedang berlangsung, sudah dibuka, akan datang, dan selesai. Ketuk satu baris untuk melihat paket, tanggal, durasi, dan tombol mulai.",
                "Jadwal mengelompokkan pekerjaan per jenis: ulangan harian, quiz, tugas, dan ujian lainnya, lalu per hari. Ketuk satu baris untuk melihat detailnya.",
                "Tombol muat ulang di kanan bawah Daftar Ujian memperbarui daftar.",
            ),
        ),
        HelpTopic(
            "Memulai",
            listOf(
                "Ketuk pekerjaan yang berstatus Sedang dibuka.",
                "Masukkan token dari pengawas bila diminta, lalu tekan Mulai Ujian.",
                "Layar akan dikunci. Tombol keluar darurat hanya untuk pengawas.",
            ),
        ),
        HelpTopic(
            "Mengerjakan soal",
            listOf(
                "Pilih atau isi jawaban. Jawaban tersimpan otomatis, dan dot oranye di kanan header soal menandakan jawaban masih dikirim.",
                "Tekan Ragu-ragu di bawah untuk menandai soal yang ingin diperiksa lagi.",
                "Gunakan Sebelumnya dan Selanjutnya, atau tombol Daftar Soal untuk meloncat ke nomor tertentu dan melihat status tiap soal.",
                "Sisa waktu tampil di header dengan angka besar. Pekerjaan otomatis dikumpulkan saat waktu habis.",
            ),
        ),
        HelpTopic(
            "Mengumpulkan",
            listOf(
                "Buka Daftar Soal, lalu tekan Selesai & Kumpulkan.",
                "Dialog menampilkan jumlah soal terjawab, ragu-ragu, belum dijawab, dan jawaban yang belum terkirim.",
                "Pilih Periksa Lagi untuk kembali mengerjakan, atau Kumpulkan untuk mengakhiri sesi.",
            ),
        ),
        HelpTopic(
            "Profil dan keluar",
            listOf(
                "Profil Saya berisi data siswa, data peserta, data kelas, data diri, dan kontak. Hubungi sekolah bila ada data yang salah.",
                "Pilih Keluar di menu untuk mengakhiri sesi masuk Anda di perangkat ini.",
            ),
        ),
    )
