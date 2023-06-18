# Braille Recognition Model w.YOLOv7

Reference - [Official YOLOv7](https://github.com/WongKinYiu/yolov7)



## Dataset

install - [Braille Train&Validation Dataset](https://drive.google.com/drive/folders/14cNsZTjbizgjo3PVlpCGAQg1vZSgpDP8) // Labeling Tool - [LabelImg](https://github.com/heartexlabs/labelImg) / [사용 방법](https://velog.io/@kimsoohyun/YOLO-%EC%9D%B4%EB%AF%B8%EC%A7%80-%EB%9D%BC%EB%B2%A8%EB%A7%81%EC%9D%84-%EC%9C%84%ED%95%9C-labelImg-%EC%82%AC%EC%9A%A9%EB%B2%95)

Classes : 28개 (26개 알파벳 소문자표 + 1개 대문자표 + 1개 소문자표)

  

#### 총 Dataset - 8,645개 Images

영어 점자 스티커를 다양한 각도, 거리, 위치에서 촬영 + 단어, 문장 단위 점자 추가 & 다양한 밝기 조절로 Data Augmentation

Train : Val = 8 : 2 = 6,904 : 1,741

  

## Modify Official YOLOv7 files

Reference - [Official YOLO v7 Custom Object Detection Tutorial](https://www.youtube.com/watch?v=-QWxJ0j9EY8)

  

### install

1. Official YOLOv7 Github (Download zip or git clone)

2. image size 1280, 전이학습 O -> yolov7-w6_training.pt 설치

| Model | Test Image Size | Transfer Learning | .pt File | 
| :-: | :-: | :-: | :-: | 
| **YOLOv7** | 640 | X | yolov7.pt | 
| **YOLOv7** | 640 | O | yolov7_training.pt | 
| **YOLOv7-w6** | 1280 | X | yolov7-w6.pt | 
| **YOLOv7-w6** | 1280 | O | yolov7-w6_training.pt | 

  

### files to modify

- [cfg/training/yolov7-w6-custom.yaml](https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/YOLOv7_custom/cfg/training/yolov7-w6-custom.yaml) -> dataset class 개수(28) 수정

- [data/custom_data.yaml](https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/YOLOv7_custom/data/custom_data.yaml) -> dataset 구성대로 수정

- [data/hyp.scratch.custom.yaml](https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/YOLOv7_custom/data/hyp.scratch.custom.yaml) -> hyperparameter 조정, flip값 0으로 수정

- [train_aux.py](https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/YOLOv7_custom/train_aux.py) -> layer freeze를 위함

  

## Training w. Transfer Learning

학습 환경: Local, GPU: NVIDIA GeForce RTX 3080

YOLOv7_custom 폴더 경로에서 모델 학습 진행

``` shell
# freeze 47 : freeze YOLOv7-w6 Backbone Layers
python train_aux.py --workers 8 --device 0 --batch-size 2 --epochs 100 --img 1280 1280 --data custom_data.yaml --hyp data/hyp.scratch.custom.yaml --cfg cfg/yolov7-w6-custom.yaml --name yolov7-w6-custom --weights yolov7-w6_training.pt --freeze 47
```
  

### Result

| Model | workers | device | batch size | image size | learning rate(lr0) | freeze | epochs | mAP@0.5 | mAP@0.5:0.95 | 
| :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | 
| **YOLOv7-w6** | 8 | 0 | 2 | 1280 | 0.00125 | 47 | 100 | **0.93%** | **0.67%** | 

<p align="center">
  <img src="https://github.com/ailleen1004/Braille_Recognition_2023/blob/main/YOLOv7_custom/run/train/yolov7-w6-custom/results.png">
</p>

자세한 결과는 [run/train/yolov7-w6-custom](https://github.com/ailleen1004/Braille_Recognition_2023/tree/main/YOLOv7_custom/run/train/yolov7-w6-custom)

best.pt file : [best.pt](https://drive.google.com/file/d/1iUdwJF_1KrJCmY5DBaTpCDd-8GFX1ayp/view?usp=drive_link)

  

## Testing

``` shell
python detect.py --weights run/train/yolov7-w6-custom/weights/best.pt --conf 0.1 --img-size 1280 --source detect
```

점자 알파벳 당 평균 정확도 0.91% 달성

자세한 결과는 [run/detect](https://github.com/ailleen1004/Braille_Recognition_2023/tree/main/YOLOv7_custom/run/detect) 참고

  

## Export

안드로이드 스튜디오 애플리케이션 <Braille2023> 상에서 모델을 사용할 수 있도록 **best.pt -> best.torchscript.ptl** 파일로 변경

```shell
# Install onnx
pip install onnx

# Export
python export.py --weights /content/drive/MyDrive/2023_1_Braille_Recognition/result/yolov7-w6-custom/weights/best.pt --grid --end2end --simplify --topk-all 100 --iou-thres 0.65 --conf-thres 0.35 --img-size 1280 1280 --max-wh 1280
```

export한 파일은 [Braille2023/app/src/main/assets](https://github.com/ailleen1004/Braille_Recognition_2023/tree/main/Braille2023/app/src/main/assets) 에 2023best.torchscript.ptl 파일로 저장됨