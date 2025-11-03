package com.android.photoviewer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    String site_url = "https://doyoung.pythonanywhere.com"; //배포 서버
    //String site_url = "http://10.0.2.2:8000"; //개발 서버
    //String site_url = "http://192.168.45.206:8000"; //socket_server test

    String token = "1db64f54572b2cd2c98d3af0aab5542fe2540415";

    CloadImage taskDownload;
    PutPost taskUpload;

    // ✅ 선택된 이미지 URI 저장
    private Uri selectedImageUri;

    // ✅ 이미지 선택을 위한 ActivityResultLauncher
    private ActivityResultLauncher<String> imagePickerLauncher;

    Spinner spinnerSort;
    List<PostItem> postList = new ArrayList<>();
    EditText editSearch;
    RecyclerView recyclerView; // ✅ 전역 선언 추가

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        editSearch = findViewById(R.id.edit_search);
        spinnerSort = findViewById(R.id.spinner_sort);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // ✅ RecyclerView 초기화

        // ✅ Spinner 항목 연결
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        // ✅ Spinner 선택 시 정렬 실행
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                sortPosts(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ✅ 이미지 선택 런처 초기화
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Toast.makeText(this, "이미지 선택됨: " + uri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                        // 선택 후 바로 업로드 시작
                        startUpload();
                    } else {
                        Toast.makeText(this, "이미지 선택이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // ✅ HTTPS 인증서 검증 무시 (PythonAnywhere HTTPS 접근용 / 실습용)
        try {
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
            context.init(null, new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
            }}, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ✅ '동기화' 버튼 클릭 시: 서버에서 이미지 목록 불러오기 */
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING)
            taskDownload.cancel(true);
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(this, "Download 시작", Toast.LENGTH_SHORT).show();
    }

    /** ✅ '새로운 이미지 게시' 버튼 클릭 시: 이미지 선택 다이얼로그 열기 */
    public void onClickUpload(View v) {
        imagePickerLauncher.launch("image/*");
    }

    /** ✅ 실제 업로드 실행 메서드 */
    private void startUpload() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "이미지를 먼저 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        taskUpload = new PutPost(selectedImageUri, "안드로이드에서 올린 테스트 게시물");
        taskUpload.execute(site_url + "/api_root/Post/");
    }

    private class CloadImage extends AsyncTask<String, Integer, List<PostItem>> {
        @Override
        protected List<PostItem> doInBackground(String... urls) {
            List<PostItem> newList = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                URL urlAPI = new URL(urls[0]);
                conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);

                    JSONArray aryJson = new JSONArray(result.toString());

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject postJson = aryJson.getJSONObject(i);
                        int id = postJson.getInt("id");
                        String title = postJson.optString("title", "");
                        String text = postJson.optString("text", "");
                        String imageUrl = postJson.optString("image", "");
                        String publishedDate = postJson.optString("published_date", "");
                        int likeCount = postJson.optInt("like_count", 0);

                        newList.add(new PostItem(id, title, text, imageUrl, publishedDate, likeCount));
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
            return newList;
        }

        @Override
        protected void onPostExecute(List<PostItem> posts) {
            if (posts.isEmpty()) {
                textView.setText("이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                postList.clear();               // ✅ 기존 리스트 초기화
                postList.addAll(posts);         // ✅ 새 데이터 추가
                recyclerView.setAdapter(new ImageOnlyAdapter(postList));
            }
        }
    }

    private class ImageOnlyAdapter extends RecyclerView.Adapter<ImageOnlyAdapter.ViewHolder> {
        private final List<PostItem> postList;

        ImageOnlyAdapter(List<PostItem> postList) {
            this.postList = postList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_only, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PostItem post = postList.get(position);

            new Thread(() -> {
                try {
                    URL url = new URL(post.imageUrl);
                    InputStream in = url.openStream();
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    holder.img.post(() -> holder.img.setImageBitmap(bmp));
                } catch (Exception e) { e.printStackTrace(); }
            }).start();

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.id);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            ViewHolder(View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.img_post_only);
            }
        }
    }

    /** ✅ AsyncTask ② : 이미지 업로드 (POST) - Uri 방식으로 변경 */
    private class PutPost extends AsyncTask<String, Integer, String> {
        private final Uri imageUri;
        private final String title;

        PutPost(Uri imageUri, String title) {
            this.imageUri = imageUri;
            this.title = title;
        }

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection conn = null;
            String responseMsg = "";

            try {
                String apiUrl = urls[0];
                String boundary = "----AndroidFormBoundary" + System.currentTimeMillis();

                URL url = new URL(apiUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                dos.writeBytes(title + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                String fileName = "upload_" + System.currentTimeMillis() + ".jpg";
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) return "이미지 파일을 읽을 수 없습니다.";

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                inputStream.close();

                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED)
                    responseMsg = "이미지 업로드 성공!";
                else
                    responseMsg = "업로드 실패 (code: " + responseCode + ")";

            } catch (Exception e) {
                e.printStackTrace();
                responseMsg = "업로드 중 오류 발생: " + e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }

            return responseMsg;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }

    public void onClickGoToUpload(View v) {
        Intent intent = new Intent(MainActivity.this, UploadActivity.class);
        startActivity(intent);
    }

    // ✅ 정렬 기능
    private void sortPosts(String sortType) {
        if (postList == null || postList.isEmpty()) return;
        if (sortType.equals("최신순")) {
            Collections.sort(postList, (a, b) -> b.publishedDate.compareTo(a.publishedDate));
        } else if (sortType.equals("좋아요순")) {
            Collections.sort(postList, (a, b) -> Integer.compare(b.likeCount, a.likeCount));
        }
        recyclerView.setAdapter(new ImageOnlyAdapter(postList));
    }

    // ✅ 검색 기능
    public void onClickSearch(View v) {
        String keyword = editSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            recyclerView.setAdapter(new ImageOnlyAdapter(postList));
            return;
        }

        List<PostItem> filtered = new ArrayList<>();
        for (PostItem p : postList) {
            if (p.title.toLowerCase().contains(keyword.toLowerCase())) {
                filtered.add(p);
            }
        }

        recyclerView.setAdapter(new ImageOnlyAdapter(filtered));
        textView.setText("검색 결과: " + filtered.size() + "개");
    }
}
