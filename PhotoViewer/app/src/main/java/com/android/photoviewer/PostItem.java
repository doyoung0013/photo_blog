package com.android.photoviewer;

public class PostItem {
    public int id;                 // 게시물 ID
    public String title;           // 제목
    public String text;            // 본문 내용
    public String imageUrl;        // 이미지 URL
    public String publishedDate;   // 작성 날짜
    public int likeCount;          // 좋아요 수

    public PostItem(int id, String title, String text, String imageUrl, String publishedDate, int likeCount) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
        this.publishedDate = publishedDate;
        this.likeCount = likeCount;
    }
}
