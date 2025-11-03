package com.android.photoviewer;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostDetailActivity extends AppCompatActivity {
    String site_url = "https://doyoung.pythonanywhere.com";
    //String site_url = "http://192.168.45.206:8000";
    String token = "1db64f54572b2cd2c98d3af0aab5542fe2540415";
    int postId;

    ImageView imageView;
    TextView titleView, textView, likeCountView;
    EditText nickInput, commentInput;
    private long lastCommentTime = 0;

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



        postId = getIntent().getIntExtra("post_id", -1);
        Log.d("DEBUG", "Clicked post id = " + postId);
        if (postId == -1) {
            Toast.makeText(this, "게시물 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDetail();

        findViewById(R.id.btn_like).setOnClickListener(v -> likePost());
        findViewById(R.id.btn_comment).setOnClickListener(v -> sendComment());
    }

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

                // ✅ 이미지도 백그라운드에서 다운로드
                String imageUrl = obj.optString("image", "");
                Bitmap bitmap = null;
                if (!imageUrl.isEmpty()) {
                    try {
                        InputStream in = new URL(imageUrl).openStream();
                        bitmap = BitmapFactory.decodeStream(in);
                        in.close();
                    } catch (Exception e) {
                        Log.e("IMAGE_ERROR", "이미지 로드 실패: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // ✅ UI 업데이트만 UI 스레드에서
                final Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> {
                    titleView.setText(obj.optString("title", ""));
                    textView.setText(obj.optString("text", ""));
                    likeCountView.setText("❤️ " + obj.optInt("like_count", 0));

                    if (finalBitmap != null) {
                        imageView.setImageBitmap(finalBitmap);
                    } else {
                        Log.d("IMAGE_ERROR", "비트맵이 null입니다.");
                    }
                });
            } catch (Exception e) {
                Log.e("DETAIL_ERROR", "상세 정보 로드 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

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
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void sendComment() {
        String nick = nickInput.getText().toString().trim();
        String content = commentInput.getText().toString().trim();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCommentTime < 300000) {
            Toast.makeText(this, "30초 이내에는 댓글을 다시 작성할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        lastCommentTime = currentTime;

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



}

