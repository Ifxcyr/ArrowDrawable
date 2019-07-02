##  ArrowDrawable，纯Paint实现的一个射箭效果，可用作Loading动画。
### 博客详情： 敬请期待。。。

### 使用方式:
#### 添加依赖：
```
implementation 'com.wuyr:arrowdrawable:1.0.0'
```

### APIs:
|Method|Description|
|------|-----------|
|create(View targetView)|创建对象<br>**targetView**: 显示此Drawable的容器，下同|
|create(View targetView, int width, int height)|指定Drawable的宽高来创建对象|
|create(View targetView, int width, int height, int bowLength)|指定Drawable的宽高、弓的长度来创建对象|
|reset()|重置ArrowDrawable为静止状态|
|hit()|开始播放命中动画|
|miss()|开始播放未命中动画|
|fire()|播放发射动画|
|updateSize(int width, int height, int bowLength)|更新ArrowDrawable的尺寸|
|setBaseLinesFallDuration(int duration)|设置线条的坠落时长|
|setFiringBowFallDuration(int duration)|设置发射中的弓向下移动的时长|
|setFiredArrowShrinkDuration(int duration)|设置发射后的箭收缩动画时长|
|setFiredArrowMoveDuration(int duration)|设置发射后的箭每次上下移动的时长|
|setSkewTan(float tan)|设置命中后左右摆动的幅度(正切值)|
|setMaxSkewCount(int count)|设置命中后一共要摆动的次数|
|setMissDuration(int duration)|设置未命中动画时长|
|setHitDuration(int duration)|设置命中动画时长|
|setSkewDuration(int duration)|设置命中后每次左右摆动的时间|
|setLineColor(int color)|设置坠落的线条颜色|
|setBowColor(int color)|设置弓颜色|
|setStringColor(int color)|设置弦颜色|
|setArrowColor(int color)|设置箭颜色|

### Demo下载: [app-debug.apk](https://github.com/wuyr/ArrowDrawable/raw/master/app-debug.apk)
### Demo源码地址： <https://github.com/wuyr/ArrowDrawable>

### 效果图：
![preview](https://github.com/wuyr/ArrowDrawable/raw/master/previews/preview1.gif) ![preview](https://github.com/wuyr/ArrowDrawable/raw/master/previews/preview2.gif)