# 📸 Photo Blog Service

Django 기반 **Server**와 Android 기반 **Client**로 구성된 포토 블로그 프로젝트입니다. 서버는 PythonAnywhere 클라우드 환경에서 운영되며, 클라이언트는 로컬 앱에서 REST API를 통해 서버와 통신합니다.

---

## 🌐 배포 환경

- **Server**: Django 5.2.6 (PythonAnywhere)
- **Client**: Android Studio (Java)
- **Database**: SQLite3
- **API Test Tool**: CodeBeautify, Postman

---

## 🧩 구성도
📱 Android (PhotoViewer)
├─ 이미지 업로드 (POST)
├─ 게시물 목록 불러오기 (GET)
├─ 좋아요 +1 (POST)
├─ 댓글 작성 (POST)
└─ 제목 검색 / 최신순·좋아요순 정렬

🌐 Django (PhotoBlogServer)
├─ /api_root/Post/ ← 게시물 전체 조회 / 업로드
├─ /api_root/Post/<id>/ ← 게시물 상세 조회
├─ /posts/<id>/like/ ← 좋아요 +1
├─ /posts/<id>/comment/ ← 익명 댓글 작성
└─ admin/ ← 관리자 페이지

---

## 📁 프로젝트 폴더 구조

📦 프로젝트 루트
│
├── 📁 PhotoBlogServer        ← Django 서버 폴더
│     ├── blog/
│     ├── mysite/
│     ├── manage.py
│     └── ... (Django 관련 파일들)
│
├── 📁 PhotoViewer             ← Android 클라이언트 폴더
│     ├── app/src/
│     ├── build.gradle
│     └── ... (Android 프로젝트 파일들)
│
└── 📄 모바일/웹서비스 프로젝트 공통평가 01_수행 결과 보고서.docx


---

## ✅ 주요 기능 목록

| 구분 | 기능명 | 설명 |
|------|--------|------|
| 1 | **게시글 업로드 (Upload)** | 갤러리에서 이미지 선택 후 제목·내용과 함께 서버에 업로드 |
| 2 | **게시글 목록 불러오기 (List)** | 서버의 게시글 목록을 RecyclerView로 표시 |
| 3 | **게시글 상세 보기 (Detail)** | 게시글 클릭 시 이미지, 내용, 댓글, 좋아요 수 표시 |
| 4 | **좋아요 기능 (Like)** | 로그인 없이 좋아요 버튼 클릭 시 +1 증가 |
| 5 | **익명 댓글 기능 (Comment)** | 닉네임과 내용만으로 댓글 작성 가능 |
| 6 | **제목 검색 기능 (Search)** | 제목에 포함된 키워드로 게시물 검색 |
| 7 | **게시물 정렬 기능 (Sort)** | 최신순 / 좋아요순으로 게시글 정렬 |

---

## 👨‍💻 개발자 정보

| 항목 | 내용 |
|------|------|
| 이름 | 김도영 |
| 학번 | 2022100631 |
| 과제 | 모바일/웹서비스 프로젝트 공통평가 01 |
| 개발 환경 | Django + Android Studio |

---

