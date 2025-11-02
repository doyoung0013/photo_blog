package com.android.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * RecyclerView 어댑터: 게시물 목록 표시
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private final List<PostItem> posts;
    private final String baseUrl;

    public PostAdapter(List<PostItem> posts, String baseUrl) {
        this.posts = posts;
        this.baseUrl = baseUrl;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int pos) {
        PostItem p = posts.get(pos);
        h.title.setText(p.title);
        h.text.setText(p.text);
        h.date.setText("🕓 " + p.publishedDate);
        h.likes.setText("❤️ " + p.likeCount);

        // 이미지 로드 (별도 스레드)
        new Thread(() -> {
            try {
                if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
                    InputStream in = new URL(p.imageUrl).openStream();
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    h.image.post(() -> h.image.setImageBitmap(bmp));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // 클릭 시 상세 페이지 이동
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), PostDetailActivity.class);
            i.putExtra("post_id", p.id);
            v.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() { return posts.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, text, date, likes;

        ViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.img_post);
            title = v.findViewById(R.id.tv_title);
            text = v.findViewById(R.id.tv_text);
            date = v.findViewById(R.id.tv_date);
            likes = v.findViewById(R.id.tv_likes);
        }
    }
}
