# GistApp — GitHub Gist Client untuk Android 📱

Aplikasi Android native untuk mengelola **GitHub Gists** — buat, lihat, edit, dan hapus gist langsung dari HP. Didesain dengan **dark theme ala GitHub** dan kompatibel dengan **Android 8.0 (Oreo, API 26) ke atas**.

---

## ✨ Fitur

| Fitur | Deskripsi |
|-------|-----------|
| 🔐 **Login PAT** | Login dengan GitHub Personal Access Token (enkripsi AES-256) |
| 👀 **Mode Publik** | Jelajahi public gists tanpa login |
| 📋 **My Gists** | Lihat & kelola semua gist milik Anda |
| ➕ **Buat Gist** | Buat gist baru dengan deskripsi, file, dan visibility |
| ✏️ **Edit Gist** | Edit deskripsi atau konten file gist yang sudah ada |
| 🗑️ **Hapus Gist** | Hapus gist dengan konfirmasi |
| 📤 **Share** | Bagikan link gist ke aplikasi lain |
| 📋 **Copy Content** | Salin konten file gist ke clipboard |
| 🌐 **Open Raw** | Buka raw file di browser |
| 🔄 **Pull to Refresh** | Refresh daftar gist dengan swipe |
| 📜 **Infinite Scroll** | Pagination otomatis saat scroll ke bawah |
| 🎨 **GitHub Dark Theme** | Tampilan gelap khas GitHub |

---

## 📸 Screenshot

*(Tambahkan screenshot setelah build)*

---

## 🔧 Tech Stack

- **Bahasa**: Kotlin 1.9
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)
- **Arsitektur**: MVVM + Repository Pattern
- **Networking**: Retrofit 2 + OkHttp 4 + Gson
- **UI**: Material Design 3, ViewBinding, RecyclerView
- **Keamanan**: EncryptedSharedPreferences (AES-256)
- **Async**: Kotlin Coroutines
- **Markdown**: Markwon (opsional)

---

## 🚀 Cara Build & Run

### Prasyarat
- **Android Studio Hedgehog (2023.1.1)** atau lebih baru
- **JDK 17**
- **Gradle 8.5**

### Langkah
```bash
# 1. Clone repository
git clone https://github.com/USERNAME/GistApp.git
cd GistApp

# 2. Buka di Android Studio
# File → Open → pilih folder GistApp

# 3. Sync Gradle (otomatis atau klik "Sync Now")

# 4. Build & Run
# Klik tombol ▶ (Run) atau:
./gradlew assembleDebug
```

### Install APK langsung
```bash
./gradlew installDebug
```

---

## 🔑 Cara Mendapatkan Personal Access Token

1. Buka [github.com/settings/tokens](https://github.com/settings/tokens)
2. Klik **Generate new token → Generate new token (classic)**
3. Beri nama (contoh: "GistApp Android")
4. Centang scope: **`gist`**
5. Klik **Generate token**
6. **Copy token** (hanya muncul sekali!)
7. Paste di aplikasi GistApp

> ⚠️ Token bersifat rahasia — jangan commit ke repository!

---

## 📂 Struktur Proyek

```
GistApp/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gistapp/
│       │   ├── GistApplication.kt
│       │   ├── data/
│       │   │   ├── model/GistModels.kt        # Data classes
│       │   │   ├── remote/
│       │   │   │   ├── GitHubApiService.kt    # Retrofit endpoints
│       │   │   │   └── RetrofitClient.kt      # OkHttp + Retrofit
│       │   │   └── repository/
│       │   │       └── GistRepository.kt      # Repository layer
│       │   ├── ui/
│       │   │   ├── MainActivity.kt            # Main + BottomNav
│       │   │   ├── auth/AuthActivity.kt       # Login PAT
│       │   │   ├── gistlist/
│       │   │   │   ├── GistListFragment.kt    # Daftar gist
│       │   │   │   └── GistAdapter.kt         # RecyclerView adapter
│       │   │   ├── gistdetail/
│       │   │   │   └── GistDetailActivity.kt  # Detail + file viewer
│       │   │   └── create/
│       │   │       └── CreateGistActivity.kt  # Create/Edit gist
│       │   └── util/
│       │       └── TokenManager.kt            # Encrypted token storage
│       └── res/
│           ├── layout/                        # XML layouts
│           ├── values/                        # Colors, strings, themes
│           ├── menu/                          # Menu definitions
│           ├── drawable/                      # Icons & backgrounds
│           └── mipmap-anydpi-v26/            # Adaptive launcher icon
├── build.gradle                               # Root build
├── settings.gradle
├── gradle.properties
└── README.md
```

---

## 📝 API Reference

Aplikasi menggunakan **GitHub REST API v3**:

| Endpoint | Method | Deskripsi |
|----------|--------|-----------|
| `/gists` | GET | List gists user |
| `/gists/public` | GET | List public gists |
| `/gists/{id}` | GET | Get single gist |
| `/gists` | POST | Create gist |
| `/gists/{id}` | PATCH | Update gist |
| `/gists/{id}` | DELETE | Delete gist |
| `/gists/{id}/star` | PUT/DELETE | Star/unstar |
| `/user` | GET | Verify token |

[Dokumentasi Resmi GitHub Gist API](https://docs.github.com/en/rest/gists)

---

## 🔒 Keamanan

- **Token disimpan terenkripsi** menggunakan Android `EncryptedSharedPreferences` (AES-256-GCM)
- **Tidak ada telemetri** — aplikasi 100% offline kecuali panggilan API ke GitHub
- **Hanya izin INTERNET** — tidak ada permission sensitif lainnya
- **ProGuard** diaktifkan di mode release

---

## 📄 Lisensi

MIT License — Bebas digunakan, dimodifikasi, dan didistribusikan.

---

## 🤝 Kontribusi

Pull request welcome! Untuk perubahan besar, buka issue dulu untuk diskusi.

---

**Dibuat dengan ❤️ untuk komunitas open-source Indonesia.**
