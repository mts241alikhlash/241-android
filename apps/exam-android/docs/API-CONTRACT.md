# Kontrak API yang diminta klien Android

Kontrak ini sudah ada di identity-service dan exam-service dan mengikuti OpenAPI keduanya.

## Envelope

Semua respons dibungkus `ResponseInterceptor`:

```json
{ "statusCode": 200, "message": "OK", "data": {} }
```

Error: `{ "statusCode": 401, "message": "Kredensial salah", "data": null }`.
Semua field JSON memakai camelCase.

## Autentikasi (identity-service)

Rute ini ada di identity-service (`/auth/native/*`). Semua field camelCase.

| Endpoint | Body | `data` |
|---|---|---|
| `POST /auth/native/login` | `{identifier, password, deviceId}` | `{accessToken, refreshToken, expiresIn, user: {id, name, roles[]}}` |
| `POST /auth/native/refresh` | `{refreshToken, deviceId}` | sama dengan login; refresh token dirotasi |
| `POST /auth/native/logout` | `{refreshToken}` | tanpa isi (204) |

- Token dikembalikan di body, tanpa cookie. `expiresIn` dalam detik.
- Hanya akun dengan role `STUDENT` atau `TEACHER` yang diterima (`403` selain itu); salah kata sandi `401`; terlalu sering `429`.
- `TEACHER` dipakai untuk verifikasi pengawas lewat endpoint login yang sama.
- Sesi terikat ke `deviceId` yang dipakai saat login: refresh dari `deviceId` lain `401` tanpa mengubah apa pun.
- Refresh yang berbalapan dengan refresh lain menjawab access token baru dengan `refreshToken: null`; klien memakai refresh token yang sudah dipegangnya.
- Logout idempoten dan tidak gagal untuk token yang sudah tidak berlaku.

## Ujian (exam-service)

Kontrak ini mengikuti `services/contracts/exam/openapi.json`; exam-service memegang kontraknya dan klien
mengikutinya.

Semua permintaan mengirim `Authorization: Bearer <access token>` dan `X-Device-Id: <id perangkat>`. Rute
`student-exams/*` menjawab `400` bila header kedua hilang dan `409` bila sesi terikat ke perangkat lain.

| Endpoint | Body | `data` |
|---|---|---|
| `GET /me/exams` | - | `[{id, title, subjectId, startsAt, endsAt, durationMinutes, status, sessionId}]` |
| `POST /me/exams/{examId}/sessions` | `{accessCode, deviceId}` | `{sessionId, deadline, serverTime}` |
| `GET /student-exams/{sessionId}/questions` | - | `[{id, number, type, bodyHtml, points, choices: [{key, html}] \| null}]` |
| `GET /student-exams/{sessionId}/answers` | - | `[{questionId, answer}]` |
| `POST /student-exams/{sessionId}/answers` | `{questionId, clientId, answer}` | `{questionId, clientId, receivedAfterDeadline, updatedAt}` |
| `POST /student-exams/{sessionId}/submit` | - | `{status, submittedAt}` |
| `GET /student-exams/{sessionId}/files/{fileId}` | - | bytes gambar (bukan envelope) |

- `examId` adalah id jadwal. `status`: `UPCOMING`, `OPEN`, `IN_PROGRESS`, `FINISHED`. `sessionId` null bila belum mulai.
- `startsAt`, `endsAt`, `submittedAt`: ISO-8601 UTC. `deadline` dan `serverTime`: epoch millis; klien menghitung deadline lokal dari selisihnya.
- `type`: `MULTIPLE_CHOICE`, `MULTIPLE_RESPONSE`, `TRUE_FALSE`, `SHORT_ANSWER`, `ESSAY`. Urutan soal dan pilihan sudah diacak per sesi.
- `answer` bertipe: `{"key":"B"}` (pilihan ganda), `{"keys":["A","C"]}` (pilihan ganda kompleks), `{"value":true}` (benar/salah), `{"text":"..."}` (isian singkat dan uraian, maksimal 20000 karakter).
- Memulai sesi: `403` token salah, kelas lain, belum dibuka atau sudah berakhir; `429` terkunci lima menit setelah lima token salah; `409` sesi aktif di perangkat lain atau sudah dikumpulkan. Memulai lagi dengan token dari perangkat yang sama melanjutkan sesi yang sama; sesi yang dilepas pengawas dilanjutkan dengan token dari perangkat baru.
- Jawaban: satu per soal, `clientId` unik per revisi (dipakai ulang untuk soal lain ditolak `409`); `clientId` yang sama diulang tidak mengubah apa pun. Kirim berurutan per soal. Diterima sampai deadline + 10 menit (ditandai `receivedAfterDeadline` setelah deadline); sesudahnya `409 Waktu pengiriman jawaban sudah habis`.
- `submit` idempoten. `status`: `SUBMITTED` atau `EXPIRED` bila ada soal isian singkat atau uraian yang menunggu guru, `GRADED` bila semua soal sudah dinilai otomatis.
- Gambar soal ada di HTML sebagai `<img data-file-id="uuid">`; unduh lewat rute `files` sesi yang sama, hanya untuk gambar yang dipakai kertas soal sesi itu (selain itu `404`).
- Siswa diidentifikasi dari JWT, tidak pernah dari parameter. Kunci jawaban tidak pernah dikirim ke klien.
