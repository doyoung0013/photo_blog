package com.android.photoviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;


public class PostDetailActivity extends AppCompatActivity {
    String site_url = "https://doyoung.pythonanywhere.com";
    //String site_url = "http://192.168.45.206:8000";
    String token = "1db64f54572b2cd2c98d3af0aab5542fe2540415";
    int postId;

    ImageView imageView;
    TextView titleView, textView, likeCountView;
    EditText nickInput, commentInput;
    RecyclerView commentListView;
    CommentAdapter commentAdapter;
    List<CommentItem> commentList = new ArrayList<>();

    private long lastCommentTime = 0; // 댓글 중복 작성 방지용 타이머

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        imageView = findViewById(R.id.detail_image);
        titleView = findViewById(R.id.detail_title);
        textView = findViewById(R.id.detail_text);
        likeCountView = findViewById(R.id.detail_likes);
        nickInput = findViewById(R.id.input_nickname);
        commentInput = findViewById(R.id.input_comment);
        commentListView = findViewById(R.id.comment_list);

        // 댓글 목록 RecyclerView 기본 설정
        commentAdapter = new CommentAdapter(commentList);
        commentListView.setLayoutManager(new LinearLayoutManager(this));
        commentListView.setAdapter(commentAdapter);

        postId = getIntent().getIntExtra("post_id", -1);
        Log.d("DEBUG", "Clicked post id = " + postId);
        if (postId == -1) {
            Toast.makeText(this, "게시물 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDetail();
        loadComments(); // ✅ 댓글 목록도 함께 불러오기

        findViewById(R.id.btn_like).setOnClickListener(v -> likePost());
        findViewById(R.id.btn_comment).setOnClickListener(v -> sendComment());
    }

    // ✅ 게시물 상세 정보 불러오기
    private void loadDetail() {
        new Thread(() -> {
            try {
                URL url = new URL(site_url + "/api_root/Post/" + postId + "/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                JSONObject obj = new JSONObject(sb.toString());

                // 이미지 로드
                String imageUrl = obj.optString("image", "");
                Bitmap bitmap = null;
                if (!imageUrl.isEmpty()) {
                    try {
                        InputStream in = new URL(imageUrl).openStream();
                        bitmap = BitmapFactory.decodeStream(in);
                        in.close();
                    } catch (Exception e) {
                        Log.e("IMAGE_ERROR", "이미지 로드 실패: " + e.getMessage());
                    }
                }

                final Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> {
                    titleView.setText(obj.optString("title", ""));
                    textView.setText(obj.optString("text", ""));
                    likeCountView.setText("❤️ " + obj.optInt("like_count", 0));

                    if (finalBitmap != null) {
                        imageView.setImageBitmap(finalBitmap);
                    }
                });
            } catch (Exception e) {
                Log.e("DETAIL_ERROR", "상세 정보 로드 실패: " + e.getMessage());
            }
        }).start();
    }

    // ✅ 댓글 목록 불러오기
    private void loadComments() {
        new Thread(() -> {
            try {
                URL url = new URL(site_url + "/posts/" + postId + "/comments/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token " + token);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray arr = new JSONArray(sb.toString());
                List<CommentItem> newComments = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String createdAt = obj.optString("created_at", "");

                    // ✅ 날짜 포맷 변환 (2025-11-04 16:20 형태로)
                    String formattedDate = formatDate(createdAt);
                    newComments.add(new CommentItem(
                            obj.optString("nickname", "익명"),
                            obj.optString("content", ""),
                            formattedDate)
                    );
                }

                runOnUiThread(() -> {
                    commentList.clear();
                    commentList.addAll(newComments);
                    commentAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("COMMENT_LOAD", "댓글 불러오기 실패: " + e.getMessage());
            }
        }).start();
    }

    // ISO 날짜 문자열을 "yyyy-MM-dd HH:mm" 형식으로 변환
    private String formatDate(String isoDate) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.KOREA);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA);

            java.util.Date date = inputFormat.parse(isoDate.replace("Z", ""));
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return isoDate; // 변환 실패 시 원본 그대로 반환
        }
    }


    // ✅ 좋아요 +1
    private void likePost() {
        new Thread(() -> {
            try {
                URL url = new URL(site_url + "/posts/" + postId + "/like/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.getInputStream().close();

                runOnUiThread(() -> {
                    int count = Integer.parseInt(likeCountView.getText().toString().replace("❤️ ", "")) + 1;
                    likeCountView.setText("❤️ " + count);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ✅ 댓글 등록
    private void sendComment() {
        String nick = nickInput.getText().toString().trim();
        String content = commentInput.getText().toString().trim();

        long now = System.currentTimeMillis();
        if (now - lastCommentTime < 300000) {
            Toast.makeText(this, "30초 이내에는 댓글을 다시 작성할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        lastCommentTime = now;

        if (content.isEmpty()) {
            Toast.makeText(this, "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(site_url + "/posts/" + postId + "/comment/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("nickname", nick);
                json.put("content", content);

                conn.getOutputStream().write(json.toString().getBytes("UTF-8"));
                int responseCode = conn.getResponseCode();

                InputStream stream = (responseCode >= 400)
                        ? conn.getErrorStream()
                        : conn.getInputStream();

                if (stream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    Log.d("COMMENT_RESPONSE", sb.toString());
                }

                int finalCode = responseCode;
                runOnUiThread(() -> {
                    if (finalCode == 201) {
                        commentInput.setText("");
                        Toast.makeText(this, "댓글 등록 완료!", Toast.LENGTH_SHORT).show();
                        loadComments(); // ✅ 등록 후 즉시 갱신
                    } else if (finalCode == 429) {
                        Toast.makeText(this, "댓글을 너무 자주 작성하고 있습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                    } else if (finalCode == 404) {
                        Toast.makeText(this, "해당 게시물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "댓글 등록 중 오류가 발생했습니다. (" + finalCode + ")", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "서버 연결 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ✅ 댓글 데이터 클래스
    public static class CommentItem {
        String nickname;
        String content;
        String created_at;

        public CommentItem(String nickname, String content, String created_at) {
            this.nickname = nickname;
            this.content = content;
            this.created_at = created_at;
        }
    }

    public static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
        private List<CommentItem> commentList;

        public CommentAdapter(List<CommentItem> commentList) {
            this.commentList = commentList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.comment_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CommentItem comment = commentList.get(position);
            holder.nickname.setText(comment.nickname);
            holder.content.setText(comment.content);
            holder.time.setText(comment.created_at);
        }

        @Override
        public int getItemCount() {
            return commentList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nickname, content, time;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nickname = itemView.findViewById(R.id.comment_nickname);
                content = itemView.findViewById(R.id.comment_content);
                time = itemView.findViewById(R.id.comment_time);
            }
        }
    }


}
