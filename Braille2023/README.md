# <Braille2023> Application w.Android Studio

Introduction - [객체 인식 모델과 자연어 처리 기술을 결합한 점자 번역기](https://drive.google.com/file/d/1aCURjZfxgsx-URhKW6UdJLEAokspgm6l/view?usp=drive_link)

Reference - [안드로이드에서 YOLO5 모델 실행 예제 프로젝트](https://discuss.pytorch.kr/t/yolo5/379)

<br/>

## About

You have to modify 'Papago Client Id', 'Papago Client Secret', 'GPT-3 API KEY' part in [app/src/main/java/org/pytorch/demo/objectdetection/MainActivity.java](https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/Braille2023/app/src/main/java/org/pytorch/demo/objectdetection/MainActivity.java)

``` shell
# Papago API
String clientId = "your own papago Client ID";     //애플리케이션 클라이언트 아이디값";
String clientSecret = "your own papago Client Secret";      //애플리케이션 클라이언트 시크릿값";

# GPT-3 API
Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer <your own API KEY>")
                .post(body)
                .build();
```

<br/>

## Application Pipeline

<p align="center">
  <img src="https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/Braille2023/pipeline.png">
</p>

<br/>

## Result

<p align="center">
  <img src="https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/Braille2023/result.png">
</p>