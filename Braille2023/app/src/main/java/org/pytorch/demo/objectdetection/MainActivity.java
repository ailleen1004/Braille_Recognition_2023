// You have to modify GPT API key, Papago Client ID, Secret

package org.pytorch.demo.objectdetection;

import static android.net.wifi.p2p.WifiP2pManager.ERROR;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements Runnable {
    //gpt
    //private TextView myGPT;

    //private final String BASE_URL = "https://api.openai.com";
    //private final Gson gson = new Gson();
    /////////////////////////////

    ///tts
    private TextToSpeech tts;
    private Button button01;
    /////////////
    private int mImageIndex = 0;
    private String[] mTestImages = {"AI_applications.jpg", "__.png", "She_loves_Inha1.jpg"};
    private ImageView mImageView;
    private ResultView mResultView;
    private Button mButtonDetect;
    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;
    private Module mModule = null;
    List<Message> messageList;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messageList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        setContentView(R.layout.activity_main);

        try {
            mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);

        mResultView = findViewById(R.id.resultView);
        mResultView.setVisibility(View.INVISIBLE);

        TextView answer;
        answer = findViewById(R.id.TextAnswer);
        String set_imageName = mTestImages[0];
        int dotIndex = set_imageName.lastIndexOf(".");
        String set_imageNameWithoutExtension = (dotIndex > 0) ? set_imageName.substring(0, dotIndex) : set_imageName;
        String set_formattedImageName = set_imageNameWithoutExtension.replace("_", " ");
        answer.setText("정답 : " + set_formattedImageName);

        final Button buttonTest = findViewById(R.id.testButton);
        buttonTest.setText(("이미지 1/3"));
        buttonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView myTextView = (TextView) findViewById(R.id.textView);
                myTextView.setText("");
                TextView myGPT = findViewById(R.id.gpt);
                myGPT.setText("");
                TextView myTranslate = findViewById(R.id.Eng2Kor);
                myTranslate.setText("");
                mResultView.setVisibility(View.INVISIBLE);
                mImageIndex = (mImageIndex + 1) % mTestImages.length;
                Button buttonTest = findViewById(R.id.testButton);
                buttonTest.setText(String.format("이미지 %d/%d", mImageIndex + 1, mTestImages.length));

                String imageName = mTestImages[mImageIndex];
                int dotIndex = imageName.lastIndexOf(".");
                String imageNameWithoutExtension = (dotIndex > 0) ? imageName.substring(0, dotIndex) : imageName;
                String formattedImageName = imageNameWithoutExtension.replace("_", " ");
                answer.setText("정답 : " + formattedImageName);

                try {
                    mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
                    mImageView.setImageBitmap(mBitmap);
                } catch (IOException e) {
                    Log.e("Object Detection", "Error reading assets", e);
                    finish();
                }
            }
        });

        ////// gpt
        Button buttonSubmit;
        buttonSubmit = findViewById(R.id.buttonCorrect);

        buttonSubmit.setOnClickListener((v)->{
            TextView myTextView = (TextView) findViewById(R.id.textView);
            String inputText = myTextView.getText().toString().trim();

            String question = "Correct the following text with no comments and please don't ignore the number and make sure to print it out in the results : " + inputText;

            addToChat(question,Message.SENT_BY_ME);
            callAPI(question);
        });

        //////

        Button buttonSubmit2;
        buttonSubmit2 = findViewById(R.id.buttonTranslate);
        buttonSubmit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Translate translate = new Translate();
                translate.execute(); //버튼 클릭시 ASYNC 사용
            }
        });

        ///tts OnInitListner로 초기화
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });
        button01 = findViewById(R.id.button01);
        button01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView myGPT = findViewById(R.id.gpt);
                // editText에 있는 문장을 읽는다.
                CharSequence ttsText = myGPT.getText();
                tts.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, "TTS");
            }
        });

        ////


        final Button buttonSelect = findViewById(R.id.selectButton);
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultView.setVisibility(View.INVISIBLE);

                final CharSequence[] options = { "갤러리에서 가져오기", "카메라로 촬영하기", "취소" };
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("점자 이미지 불러오기");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("카메라로 촬영하기")) {
                            Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(takePicture, 0);
                        }
                        else if (options[item].equals("갤러리에서 가져오기")) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto , 1);
                        }
                        else if (options[item].equals("취소")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });

        mButtonDetect = findViewById(R.id.detectButton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButtonDetect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mButtonDetect.setEnabled(false);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mButtonDetect.setText(getString(R.string.run));

                mImgScaleX = (float)mBitmap.getWidth() / PrePostProcessor.mInputWidth;
                mImgScaleY = (float)mBitmap.getHeight() / PrePostProcessor.mInputHeight;

                mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float)mImageView.getWidth() / mBitmap.getWidth() : (float)mImageView.getHeight() / mBitmap.getHeight());
                mIvScaleY  = (mBitmap.getHeight() > mBitmap.getWidth() ? (float)mImageView.getHeight() / mBitmap.getHeight() : (float)mImageView.getWidth() / mBitmap.getWidth());

                mStartX = (mImageView.getWidth() - mIvScaleX * mBitmap.getWidth())/2;
                mStartY = (mImageView.getHeight() -  mIvScaleY * mBitmap.getHeight())/2;
                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });

        try {
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "2023best.torchscript.ptl"));
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("braille.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            PrePostProcessor.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcessor.mClasses);
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }
    }
    class Translate extends AsyncTask<String ,Void, String > {   //ASYNCTASK를 사용

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override

        protected String doInBackground(String... strings) {
            TextView myTranslate;
            myTranslate = findViewById(R.id.Eng2Kor);
            TextView myGPT;
            myGPT = findViewById(R.id.gpt);
            String getresult;
            //////네이버 API

            String clientId = "   ";     //애플리케이션 클라이언트 아이디값";
            String clientSecret = "   ";      //애플리케이션 클라이언트 시크릿값";

            try {
                String text = myGPT.getText().toString();  /// 번역할 문장 Edittext  입력
                String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                // post request
                String postParams = "source=en&target=ko&text=" + text; // 영 -> 한 번역
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {  // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                getresult = response.toString();
                getresult = getresult.split("\"")[15];   //스플릿으로 번역된 결과값만 가져오기
                addResponse2(getresult);
            } catch (Exception e) {
                System.out.println(e);
            }
            return null;
        }
    }

    // tts
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTS 객체가 남아있다면 실행을 중지하고 메모리에서 제거한다.
        if(tts != null){
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
    ////

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        mBitmap = (Bitmap) data.getExtras().get("data");
                        Matrix matrix = new Matrix();
                        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                        mImageView.setImageBitmap(mBitmap);
                    }
                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                mBitmap = BitmapFactory.decodeFile(picturePath);
                                Matrix matrix = new Matrix();
                                mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                                mImageView.setImageBitmap(mBitmap);
                                cursor.close();
                            }
                        }
                    }
                    break;
            }
        }
    }

    public int w;
    public int h;

    @Override
    public void run() {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        final ArrayList<Result> results =  PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);

        runOnUiThread(() -> {
            mButtonDetect.setEnabled(true);
            mButtonDetect.setText(getString(R.string.detect));
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mResultView.setResults(results);
            TextView myTextView = (TextView) findViewById(R.id.textView);
            myTextView.setText("");
            int[][] arr = new int[results.size()][3];

            for(int i =0; i< results.size(); i++) {
                arr[i][0] = (int)results.get(i).rect.left;
                arr[i][1] = (int)results.get(i).rect.top;
                arr[i][2] = results.get(i).classIndex;
                //   myTextView.append(Integer.toString(arr[i][1]));
                //   myTextView.append(" ");
            }

            for(int j=0; j<arr.length; j++) {
                myTextView.append(PrePostProcessor.mClasses[arr[j][2]]);
            }
            String detectText = myTextView.getText().toString().trim();
            Log.i("detection text before sorting", detectText);
            myTextView.setText("");
            Arrays.sort(arr, new Comparator<int[]>() {
                @Override
                public int compare(int[] o1, int[] o2) {
                    return o1[1]-o2[1]; // 첫번째 숫자 기준 오름차순 {1,30}{2,10}{3,50}{4,20}{5,40}
                }
            });

            Arrays.sort(arr, new Comparator<int[]>() {
                @Override
                public int compare(int[] o1, int[] o2) {
                    if (o1[1] - o2[1] > 100) {
                        return o1[1] - o2[1];
                    } else if (o1[0] - o2[0] > 50) {
                        return o1[0] - o2[0]; // 첫번째 숫자 기준 오름차순 {1,30}{2,10}{3,50}{4,20}{5,40}
                    } else return o1[0] - o2[0];
                }
            });
            for(int j=0; j<arr.length-1; j++) {
                myTextView.append(PrePostProcessor.mClasses[arr[j][2]]);
                if (arr[j+1][0] - arr[j][0] > 200){
                    myTextView.append(" ");
                }
                if (arr[j+1][1] - arr[j][1] > 300) {
                    myTextView.append("\n");
                }
            }
            myTextView.append(PrePostProcessor.mClasses[arr[arr.length-1][2]]);
            String inputText = myTextView.getText().toString().trim();
            Log.i("detection text after sorting", inputText);
            StringBuilder result = new StringBuilder();
            int length = inputText.length();

            for (int i = 0; i < length; i++) {
                char currentChar = inputText.charAt(i);

                if (currentChar == 'u' && i < length - 5 && inputText.substring(i, i + 5).equals("upper")) {
                    char nextChar = Character.toUpperCase(inputText.charAt(i + 5));
                    result.append(nextChar);
                    i += 5;
                } else if (currentChar == 'n' && i < length - 6 && inputText.substring(i, i + 6).equals("number")) {
                    char nextChar = inputText.charAt(i + 6);
                    int numericValue = Character.toLowerCase(nextChar) - 'a' + 1;
                    result.append(numericValue);
                    i += 6;
                } else {
                    result.append(currentChar);
                }
            }
            myTextView.setText(result);
            // myTextView.setText("hi");
            mResultView.invalidate();
            mResultView.setVisibility(View.VISIBLE);
        });
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView myGPT;
                myGPT = findViewById(R.id.gpt);
                myGPT.setText("");
                // index가 2 초과일 경우 messageList 초기화
                if (messageList.size() > 1) {
                    messageList.clear();
                }
                messageList.add( new Message(message, sentBy));

                if (messageList.size() > 1) {
                    Message message = messageList.get(1);
                    Log.d("MessageList", "Second message: " + message.getMessage());
                    if (message.getSentBy().equals(Message.SENT_BY_ME)) {
                        // 코드 넣기
                    } else {
                        myGPT.append(message.getMessage());
                        Log.i("append message", message.getMessage());
                    }
                }

            }
        });
    }

    void addToChat2(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView myTranslate;
                myTranslate = findViewById(R.id.Eng2Kor);
                myTranslate.setText("");
                // index가 2 초과일 경우 messageList 초기화
                if (messageList.size() > 1) {
                    messageList.clear();
                }
                messageList.add( new Message(message, sentBy));

                if (messageList.size() > 1) {
                    Message message = messageList.get(1);
                    Log.d("MessageList", "Second message: " + message.getMessage());
                    if (message.getSentBy().equals(Message.SENT_BY_ME)) {
                        // 코드 넣기
                    } else {
                        myTranslate.append(message.getMessage());
                        Log.i("append message", message.getMessage());
                    }
                }

            }
        });
    }

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response,Message.SENT_BY_BOT);
    }

    void addResponse2(String response){
        messageList.remove(messageList.size()-1);
        addToChat2(response,Message.SENT_BY_BOT);
    }

    void callAPI(String question) {
        //okhttp
        messageList.add(new Message("Typing... ", Message.SENT_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "text-davinci-003");
            jsonBody.put("prompt", question);
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer   ")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                } else {
                    addResponse("Failed to load response due to " + response.body().toString());
                }
            }
        });
    }
}
