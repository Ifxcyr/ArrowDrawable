package com.wuyr.arrowdrawable;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author wuyr
 * @github https://github.com/wuyr/ArrowDrawable
 * @since 2019-07-01 下午8:17
 */
@SuppressWarnings({"WeakerAccess", "SuspiciousNameCombination", "unused"})
public class ArrowDrawable extends Drawable {

    /**
     * 静止状态
     */
    public static final int STATE_NORMAL = 0;

    /**
     * 正在拖动
     */
    public static final int STATE_DRAGGING = 1;

    /**
     * 发射动画播放中
     */
    public static final int STATE_FIRING = 3;

    /**
     * 命中动画播放中
     */
    public static final int STATE_HITTING = 4;

    /**
     * 未命中动画播放中
     */
    public static final int STATE_MISSING = 5;

    /**
     * {@link #decomposePath(PathMeasure)}分解的点密度(单位: px)
     */
    private float mPrecision = 2;

    private int mState;//当前状态
    private float mProgress;//当前进度
    private int mWidth;//总宽
    private int mHeight;//总高
    private float mCenterX;//水平中心点
    private float mBowLength;//弓长
    private float mBowWidth;//弓宽
    private float mStringWidth;//弦宽
    private float mHandleWidth;//握柄宽
    private float mArrowBodyLength;//箭杆长
    private float mArrowBodyWidth;//箭杆宽
    private float mFinHeight;//箭羽高
    private float mFinWidth;//箭羽宽
    private float mFinSlopeHeight;//箭羽倾斜高
    private float mArrowWidth;//箭嘴宽
    private float mArrowHeight;//箭嘴高

    private Path mBowPath = new Path();//弓
    private float[] mBowPathPoints;//弓分解后的点
    private Path mHandlePath = new Path();//握柄
    private Path mArrowPath = new Path();//箭
    private PointF mStringStartPoint = new PointF();//弦在弓左边的坐标点
    private PointF mStringMiddlePoint = new PointF();//弦在弓中间的坐标点
    private PointF mStringEndPoint = new PointF();//弦在弓右边的坐标点
    private RectF mArrowTail = new RectF();//箭的阴影
    private List<Line> mLines;//发射中坠落的线条

    private float mMaxBowOffset;//弓最大偏移量
    private float mBaseStringCenterY;//弦的中心点初始y坐标
    private float mMaxStringOffset;//弦最大偏移量
    private float mStringOffset;//弦当前偏移量
    private float mArrowOffset;//箭当前偏移量

    private final float mBaseAngle = 25;//弓的初始角度
    private final float mUsableAngle = 20;//弓的可弯曲角度

    private int mBaseLinesFallDuration = 200;//线条的坠落时长

    private long mFiringBowFallDuration = 100;//发射中的弓向下移动的时长
    private long mFireTime;//发射开始时间
    private float mFiringBowOffsetDistance;//发射中的弓向下移动的总距离

    private long mFiredArrowShrinkDuration = 200;//发射后的箭收缩动画时长
    private long mFiredArrowShrinkStartTime;//发射后的箭收缩动画开始时间
    private float mFiredArrowShrinkDistance;//发射后的箭要收缩的距离

    private long mFiredArrowMoveDuration = 200;//发射后的箭每次上下移动的时长
    private long mFiredArrowMoveStartTime;//发射后的箭上下移动动画开始时间
    private float mFiredArrowMoveDistance;//发射后的箭每次要移动的距离
    private float mFiredArrowLastMoveDistance;//发射后的箭上一次的移动距离

    private long mMissDuration = 400;//未命中动画时长
    private long mMissStartTime;//未命中动画开始时间
    private float mMissDistance;//未命中动画要移动的距离

    private long mHitDuration = 50;//命中动画时长
    private long mHitStartTime;//命中动画开始时间
    private float mHitDistance;//命中动画要移动的距离

    private float mSkewDuration = 25;//命中后每次左右摆动的时间
    private float mSkewStartTime;//命中后左右摆动动画的开始时间
    private float mSkewTan = .035F;//命中后左右摆动的幅度(正切值)(.035F约等于2度)
    private int mMaxSkewCount = 9;//命中后一共要摆动的次数
    private int mCurrentSkewCount;//当前摆动的次数

    private int mLineColor = Color.WHITE;//坠落的线条颜色
    private int mBowColor = Color.WHITE;//弓颜色
    private int mStringColor = Color.WHITE;//弦颜色
    private int mArrowColor = Color.WHITE;//箭颜色

    private Paint mPaint;
    private PathMeasure mBowPathMeasure;
    private ScaleHelper mScaleHelper;//缩放比例辅助类
    private BlurMaskFilter mTailMaskFilter;//发射后的箭尾阴影特效
    private Random mRandom = new Random();

    /**
     * 通过目标View创建ArrowDrawable对象
     * ArrowDrawable宽高=View的宽高
     */
    public static ArrowDrawable create(final View targetView) {
        //关闭硬件加速（Paint的setMaskFilter方法不支持硬件加速）
        targetView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //弓长 取 总宽度的 40%
        int bowLength = (int) (targetView.getWidth() * .4F);
        final ArrowDrawable drawable = new ArrowDrawable(targetView.getWidth(), targetView.getHeight(), bowLength);
        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
            //无效宽高，等待布局完成后更新一次尺寸
            targetView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (targetView.getWidth() > 0 || targetView.getHeight() > 0) {
                        //弓长 取 总宽度的 40%
                        int bowLength = (int) (targetView.getWidth() * .4F);
                        //更新有效宽高
                        drawable.updateSize(targetView.getWidth(), targetView.getHeight(), bowLength);
                        //移除监听器，不再需要
                        targetView.removeOnLayoutChangeListener(this);
                    }
                }
            });
        }
        return drawable;
    }

    /**
     * 通过目标View创建ArrowDrawable对象
     *
     * @param targetView 目标View
     * @param width      ArrowDrawable的宽
     * @param height     ArrowDrawable的高
     */
    public static ArrowDrawable create(View targetView, int width, int height) {
        return create(targetView, width, height, (int) (width * .4F)/*弓长 取 总宽度的 40%*/);
    }

    /**
     * 通过目标View创建ArrowDrawable对象
     *
     * @param targetView 目标View
     * @param width      ArrowDrawable的宽
     * @param height     ArrowDrawable的高
     * @param bowLength  弓的长度
     */
    public static ArrowDrawable create(View targetView, int width, int height, int bowLength) {
        targetView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        if (width <= 0 || height <= 0) {
            //无效宽高
            throw new IllegalArgumentException("Invalid size!");
        }
        return new ArrowDrawable(width, height, bowLength);
    }

    private ArrowDrawable(int width, int height, int bowLength) {
        initPaint();
        mScaleHelper = new ScaleHelper(.2F, 0, 1, .05F, 2F, .5F, 1, .95F, .2F, 1);
        updateSize(width, height, bowLength);
        initLines();
    }

    /**
     * 重置ArrowDrawable为静止状态
     */
    public void reset() {
        resetWithoutInvalidate();
        invalidateSelf();
    }

    /**
     * 重置状态，不重绘
     */
    private void resetWithoutInvalidate() {
        mState = STATE_NORMAL;
        mProgress = 0;
        mArrowOffset = 0;
        mStringOffset = 0;
        initArrowPath(mArrowBodyLength);
    }

    /**
     * 开始播放命中动画，当前状态为{@link #STATE_FIRING}才有效
     */
    public void hit() {
        //处在上下移动状态时才可以hit
        if (mState == STATE_FIRING && mFiredArrowMoveStartTime > 0) {
            mState = STATE_HITTING;
            mHitStartTime = SystemClock.uptimeMillis();

            float currentArrowOffset = mArrowOffset + mFiredArrowLastMoveDistance;
            if (mFiredArrowMoveDistance > 0) {
                //如果距离是正数，证明已经向上偏移过一次了，因为第一次是负数，所以要减去这个距离
                currentArrowOffset -= mFiredArrowMoveDistance;
            }
            float arrowBodyHeight = mFinHeight + mFinSlopeHeight + mArrowBodyLength;
            //因为是向上移动，所以是负数
            mHitDistance = -(currentArrowOffset - arrowBodyHeight);
            mFiredArrowLastMoveDistance = 0;
            invalidateSelf();
        }
    }

    /**
     * 开始播放未命中动画，当前状态为{@link #STATE_FIRING}才有效
     */
    public void miss() {
        //处在上下移动状态时才可以miss
        if (mState == STATE_FIRING && mFiredArrowMoveStartTime > 0) {
            mState = STATE_MISSING;
            mMissStartTime = SystemClock.uptimeMillis();

            float currentArrowOffset = mArrowOffset + mFiredArrowLastMoveDistance;
            if (mFiredArrowMoveDistance > 0) {
                //如果距离是正数，证明已经向上偏移过一次了，因为第一次是负数，所以要减去这个距离
                currentArrowOffset -= mFiredArrowMoveDistance;
            }
            //因为是向上移动，所以是负数
            mMissDistance = -(currentArrowOffset + mArrowTail.height());
            mFiredArrowLastMoveDistance = 0;

            invalidateSelf();
        }
    }

    /**
     * 播放发射动画，当前状态为{@link #STATE_DRAGGING}并且{@link #setProgress(float)}>0.95 才有效
     */
    public void fire() {
        if (mProgress >= .95F && mState == STATE_DRAGGING) {
            mState = STATE_FIRING;
            for (Line tmp : mLines) {
                initLines(tmp);
            }
            mFiredArrowShrinkStartTime = 0;
            mFiredArrowMoveStartTime = 0;
            //重置上一次的偏移距离
            mFiredArrowLastMoveDistance = 0;
            //第一次要向上移动，所以是负数
            mFiredArrowMoveDistance = -Math.abs(mFiredArrowMoveDistance);
            mFireTime = SystemClock.uptimeMillis();
            invalidateSelf();
        }
    }

    /**
     * 初始化线条
     */
    private void initLines() {
        mLines = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            mLines.add(new Line());
        }
    }

    /**
     * 初始化线条数据
     */
    private void initLines(Line tmp) {
        tmp.startTime = SystemClock.uptimeMillis();
        tmp.duration = mBaseLinesFallDuration / 4 + mRandom.nextInt(mBaseLinesFallDuration);
        tmp.startY = -mHeight + mRandom.nextFloat() * mHeight;
        tmp.height = -tmp.startY;
        tmp.startX = mRandom.nextFloat() * mWidth;
        tmp.endX = tmp.startX;
        tmp.distance = mHeight - tmp.startY;
    }

    /**
     * 更新ArrowDrawable的尺寸
     *
     * @param width     总宽度
     * @param height    总高度
     * @param bowLength 弓长
     */
    public void updateSize(int width, int height, int bowLength) {
        mWidth = width;
        mHeight = height;
        //弓长
        mBowLength = bowLength;
        //水平中心点
        mCenterX = mWidth / 2F;
        //弦的水平中心点，保持不变
        mStringMiddlePoint.x = mCenterX;
        //弓宽 取 弓长的 1/50
        mBowWidth = mBowLength / 50;
        //弦宽 取 弓长的 1/3
        mStringWidth = mBowWidth / 3;
        //握柄宽 取 弓宽的 2.5倍
        mHandleWidth = mBowWidth * 2.5F;
        //箭杆长度 取 弓长的一半
        mArrowBodyLength = mBowLength / 2;
        //箭杆宽度 取 箭杆长度的 1/70
        mArrowBodyWidth = mArrowBodyLength / 70;
        //箭羽高度 取 箭杆长度的 1/6
        mFinHeight = mArrowBodyLength / 6;
        //箭羽宽度 取 箭羽高度 1/3
        mFinWidth = mFinHeight / 3;
        //箭羽倾斜高度 = 箭羽宽度
        mFinSlopeHeight = mFinWidth;
        //箭嘴宽度 = 箭羽宽度
        mArrowWidth = mFinWidth;
        //箭嘴高度 取 箭杆长度的 1/8
        mArrowHeight = mArrowBodyLength / 8;
        //发射后的箭长度要缩短30%
        mFiredArrowShrinkDistance = mArrowBodyLength * .3F;
        //发射后的箭每次上下移动的距离 取 箭羽的高度
        mFiredArrowMoveDistance = mFinHeight;

        mBaseStringCenterY = getPointByAngle(mBaseAngle).y + mBowWidth;//+mBowWidth，就是画笔的宽度，这样才不会画出格
        float bowHeight = mBaseStringCenterY + (mHandleWidth / 2/*握柄宽度的一半*/);
        mMaxBowOffset = bowHeight + (mHeight - mArrowBodyLength) / 2;
        mMaxStringOffset = mArrowBodyLength - bowHeight;
        //发射中的弓身向下移动的总距离
        mFiringBowOffsetDistance = mHeight - mMaxBowOffset + bowHeight;


        if (mFinWidth > 0) {
            mTailMaskFilter = new BlurMaskFilter(mFinWidth, BlurMaskFilter.Blur.NORMAL);
        }
        mPaint.setPathEffect(new CornerPathEffect(mBowWidth));
        initArrowPath(mArrowBodyLength);
        initArrowTail();
        invalidateSelf();
    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    /**
     * 初始化箭尾
     */
    private void initArrowTail() {
        float tailHeight = mFinHeight * 2;
        mArrowTail.set(mCenterX - mFinWidth, 0, mCenterX + mFinWidth, tailHeight);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        switch (mState) {
            case STATE_MISSING:
                handleMissingState(canvas);
                break;
            case STATE_HITTING:
                handleHittingState(canvas);
                break;
            case STATE_FIRING:
                handleFiringState(canvas);
                break;
            default:
                //画弓身
                updateBowPath(getAngleByProgress());
                drawBowPath(canvas);
                //画弓柄
                updateHandlePath();
                drawHandlePath(canvas);
                //画弦
                updateStringPoints();
                drawString(canvas);
                //画箭
                updateArrowOffset();
                drawArrow(canvas);
                break;
        }
    }

    /**
     * 处理发射中的状态
     */
    private void handleFiringState(@NonNull Canvas canvas) {
        float firedTime = SystemClock.uptimeMillis() - mFireTime;
        if (firedTime <= mFiringBowFallDuration) {
            drawBowFalling(canvas, firedTime);
        }
        drawLinesAndArrow(canvas);
        invalidateSelf();
    }

    /**
     * 处理命中状态
     */
    private void handleHittingState(@NonNull Canvas canvas) {
        if (mHitStartTime > 0) {
            drawArrowHitting(canvas);
            invalidateSelf();
        } else {
            if (mSkewStartTime > 0) {
                drawArrowSkewing(canvas);
            } else {
                drawArrow(canvas);
            }
        }
    }

    /**
     * 处理未命中状态
     */
    private void handleMissingState(@NonNull Canvas canvas) {
        if (mMissStartTime > 0) {
            float runTime = SystemClock.uptimeMillis() - mMissStartTime;
            float percent = runTime / mMissDuration;
            if (percent > 1) {
                percent = 1;
            }
            float distance = percent * mMissDistance;
            float offset = distance - mFiredArrowLastMoveDistance;
            mFiredArrowLastMoveDistance = distance;
            mArrowTail.offset(0, offset);
            mArrowPath.offset(0, offset);

            if (percent < 1) {
                //先画线条
                drawLines(canvas);
                updateLinesY();
            }
            //画箭
            drawArrow(canvas);

            drawArrowTail(canvas);
            if (percent == 1) {
                mMissStartTime = 0;
                return;
            }
        }
        invalidateSelf();
    }

    /**
     * 画正在坠落的弓
     */
    private void drawBowFalling(@NonNull Canvas canvas, float firedTime) {
        float percent = firedTime / mFiringBowFallDuration;
        if (percent > 1) {
            percent = 1;
        }
        float angle = getAngleByProgress() - (percent * 3/*在弓向下移动了总距离的1/3时完全展开*/ * mUsableAngle);
        if (angle < mBaseAngle) {
            angle = mBaseAngle;
        }
        updateBowPath(angle);
        mBowPath.offset(0, percent * mFiringBowOffsetDistance);

        drawBowPath(canvas);
        updateHandlePath();
        drawHandlePath(canvas);

        //画弦
        updateStringPoints(false);
        if (mStringMiddlePoint.y < mStringStartPoint.y) {
            mStringMiddlePoint.y = mStringStartPoint.y;
            if (mFiredArrowShrinkStartTime == 0) {
                mFiredArrowShrinkStartTime = SystemClock.uptimeMillis();
            }
        }
        drawString(canvas);
        //画箭
        drawArrow(canvas);
    }

    /**
     * 画正在左右摇摆的箭
     */
    private void drawArrowSkewing(@NonNull Canvas canvas) {
        float runTime = SystemClock.uptimeMillis() - mSkewStartTime;
        float percent = runTime / mSkewDuration;
        if (percent > 1) {
            percent = 1;
        }
        float tan = mSkewTan * percent;
        if (mCurrentSkewCount % 2 == 0) {
            tan -= mSkewTan;
        }
        //箭头已经到达目的地
        canvas.skew(tan, 0);
        //画箭
        drawArrow(canvas);
        if (percent == 1) {
            if (mCurrentSkewCount == mMaxSkewCount) {
                //完满结束
                mSkewStartTime = 0;
                return;
            } else {
                mSkewStartTime = SystemClock.uptimeMillis();
                mCurrentSkewCount++;
            }
            //如果次数为偶数就要切换方法(一次来一次回，所以是偶数)
            if (mCurrentSkewCount % 2 == 0) {
                mSkewTan = -mSkewTan;
            }
        }
        invalidateSelf();
    }

    /**
     * 画正在射向目标的箭
     */
    private void drawArrowHitting(@NonNull Canvas canvas) {
        float runTime = SystemClock.uptimeMillis() - mHitStartTime;
        float percent = runTime / mHitDuration;
        if (percent > 1) {
            percent = 1;
            mHitStartTime = 0;
            mSkewStartTime = SystemClock.uptimeMillis();
            mCurrentSkewCount = 1;
        }
        float distance = percent * mHitDistance;
        float offset = distance - mFiredArrowLastMoveDistance;
        mFiredArrowLastMoveDistance = distance;
        mArrowTail.offset(0, offset);
        mArrowPath.offset(0, offset);

        //先画线条
        drawLines(canvas);
        updateLinesY();
        //画箭
        drawArrow(canvas);
        //箭尾渐渐变得透明起来，直至完全透明
        drawArrowTail(canvas, (int) (255 * (1 - percent)));
        mPaint.setAlpha(255);
    }

    /**
     * 画线条和箭
     */
    private void drawLinesAndArrow(@NonNull Canvas canvas) {
        if (mFiredArrowMoveStartTime > 0) {
            //先画线条
            drawLines(canvas);
            updateLinesY();
            drawDancingArrow(canvas);
        } else if (mFiredArrowShrinkStartTime > 0) {
            drawShrinkingArrow(canvas);
        }
    }

    /**
     * 画正在上下移动的箭
     */
    private void drawDancingArrow(@NonNull Canvas canvas) {
        float runTime = SystemClock.uptimeMillis() - mFiredArrowMoveStartTime;
        float percent = runTime / mFiredArrowMoveDuration;
        if (percent > 1) {
            percent = 1;
        }
        float distance = percent * mFiredArrowMoveDistance;
        float offset = distance - mFiredArrowLastMoveDistance;
        mFiredArrowLastMoveDistance = distance;
        mArrowTail.offset(0, offset);
        mArrowPath.offset(0, offset);
        drawArrow(canvas);

        drawArrowTail(canvas);

        if (percent == 1) {
            //刷新开始时间
            mFiredArrowMoveStartTime = SystemClock.uptimeMillis();
            //切换方向
            mFiredArrowMoveDistance = -mFiredArrowMoveDistance;
            //重置上一次的偏移距离
            mFiredArrowLastMoveDistance = 0;
        }
    }

    /**
     * 画正在缩短的箭
     */
    private void drawShrinkingArrow(@NonNull Canvas canvas) {
        //这里要更新ArrowPath
        float runTime = SystemClock.uptimeMillis() - mFiredArrowShrinkStartTime;
        float percent = runTime / mFiredArrowShrinkDuration;
        if (percent > 1) {
            percent = 1;
        }
        float needSubtractLength = percent * mFiredArrowShrinkDistance;
        float arrowLength = mArrowBodyLength - needSubtractLength;
        initArrowPath(arrowLength);

        float newArrowOffset = mArrowOffset - needSubtractLength;
        mArrowPath.offset(0, newArrowOffset);
        mArrowTail.offsetTo(mArrowTail.left, newArrowOffset - mFinHeight / 2);

        drawArrowTail(canvas, (int) (255 * percent));

        mPaint.setAlpha(255);
        drawArrow(canvas);

        if (percent == 1) {
            mFiredArrowShrinkStartTime = 0;
            mFiredArrowMoveStartTime = SystemClock.uptimeMillis();
        }
    }

    /**
     * 画正在坠落的线条
     */
    private void drawLines(@NonNull Canvas canvas) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mArrowBodyWidth);
        mPaint.setColor(mLineColor);

        for (Line tmp : mLines) {
            canvas.drawLine(tmp.startX, tmp.startY, tmp.endX, tmp.startY + tmp.height, mPaint);
        }
    }

    /**
     * 画弓
     */
    private void drawBowPath(Canvas canvas) {
        mBowPathMeasure = new PathMeasure(mBowPath, false);
        mBowPathPoints = decomposePath(mBowPathMeasure);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mBowColor);

        final int length = mBowPathPoints.length;
        float fraction;
        float radius;
        for (int i = 0; i < length; i += 2) {
            fraction = ((float) i) / length;
            radius = mBowWidth * mScaleHelper.getScale(fraction) / 2;
            canvas.drawCircle(mBowPathPoints[i], mBowPathPoints[i + 1], radius, mPaint);
        }
    }

    /**
     * 画手柄
     */
    private void drawHandlePath(@NonNull Canvas canvas) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mHandleWidth);
        mPaint.setColor(mBowColor);

        canvas.drawPath(mHandlePath, mPaint);
    }

    /**
     * 画弦
     */
    private void drawString(@NonNull Canvas canvas) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStringWidth);
        mPaint.setColor(mStringColor);

        canvas.drawLine(mStringStartPoint.x, mStringStartPoint.y, mStringMiddlePoint.x, mStringMiddlePoint.y, mPaint);
        canvas.drawLine(mStringEndPoint.x, mStringEndPoint.y, mStringMiddlePoint.x, mStringMiddlePoint.y, mPaint);
    }

    /**
     * 画箭
     */
    private void drawArrow(@NonNull Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mArrowColor);

        canvas.drawPath(mArrowPath, mPaint);
    }

    private void drawArrowTail(@NonNull Canvas canvas) {
        drawArrowTail(canvas, 255);
    }

    /**
     * 画箭尾
     */
    private void drawArrowTail(@NonNull Canvas canvas, int alpha) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mArrowColor);
        mPaint.setAlpha(alpha);
        mPaint.setMaskFilter(mTailMaskFilter);

        canvas.drawRect(mArrowTail, mPaint);

        mPaint.setMaskFilter(null);
    }

    /**
     * 更新箭偏移量
     */
    private void updateArrowOffset() {
        float newOffset = 0;
        if (mProgress > .5F) {
            newOffset = mStringOffset;
        } else if (mProgress >= .25F) {
            newOffset = (mProgress - .25F/*从0开始*/) * mStringOffset * 4/*剩下的25%要走完这段距离*/;
        }
        mArrowPath.offset(0, -mArrowOffset);
        mArrowPath.offset(0, mArrowOffset = newOffset);
    }

    /**
     * 重画手柄
     */
    private void updateHandlePath() {
        float bowPathLength = mBowPathMeasure.getLength();
        float handlePathLength = bowPathLength / 5;
        float center = bowPathLength / 2;
        float start = center - handlePathLength / 2;
        mHandlePath.reset();
        mBowPathMeasure.getSegment(start, start + handlePathLength, mHandlePath, true);
    }

    /**
     * 更新每一条线的y坐标
     */
    private void updateLinesY() {
        for (Line tmp : mLines) {
            float runtime = SystemClock.uptimeMillis() - tmp.startTime;
            float percent = runtime / tmp.duration;
            tmp.startY = percent * tmp.distance - tmp.height;
            if (tmp.startY >= mHeight) {
                initLines(tmp);
            }
        }
    }

    /**
     * 更新弦的坐标点
     */
    private void updateStringPoints() {
        updateStringPoints(true);
    }

    /**
     * 更新弦的坐标点
     *
     * @param updateMiddlePointY 是否更新中间的y轴坐标
     */
    private void updateStringPoints(boolean updateMiddlePointY) {
        int length = mBowPathPoints.length;
        int stringStartIndex = (int) (length * .05F);
        //必须是偶数
        if (stringStartIndex % 2 != 0) {
            stringStartIndex--;
        }
        int stringEndIndex = (int) (length * .95F);
        if (stringEndIndex % 2 != 0) {
            stringEndIndex--;
        }
        mStringStartPoint.x = mBowPathPoints[stringStartIndex];
        mStringStartPoint.y = mBowPathPoints[stringStartIndex + 1];
        mStringEndPoint.x = mBowPathPoints[stringEndIndex];
        mStringEndPoint.y = mBowPathPoints[stringEndIndex + 1];
        if (updateMiddlePointY) {
            mStringMiddlePoint.y = mStringOffset = mStringStartPoint.y + (mProgress <= .5F ?
                    0 : (mProgress - .5F) * mMaxStringOffset * 2/*因为只剩下50%的距离，所以要2倍*/);
        }
    }

    /**
     * 重画弓
     *
     * @param currentAngle 弓弯曲的角度
     */
    private void updateBowPath(float currentAngle) {
        PointF stringPoint = getPointByAngle(currentAngle);

        float startX = mCenterX * 2 - stringPoint.x;
        float startY = stringPoint.y;
        float controlX = mCenterX;
        float controlY = -stringPoint.y;
        float endX = stringPoint.x;
        float endY = stringPoint.y;

        mBowPath.reset();
        //镜像的x轴
        mBowPath.moveTo(startX, startY);
        mBowPath.quadTo(controlX, controlY, endX, endY);

        //初始偏移量
        float offsetY = -mBaseStringCenterY;
        //根据滑动进度偏移
        offsetY += mMaxBowOffset * (mProgress <= .25F ? mProgress * 4/*因为总距离只有25%，所以要4倍速度赶上*/ : 1);
        //偏移弓
        mBowPath.offset(0, offsetY);
    }

    /**
     * 初始化箭
     *
     * @param arrowBodyLength 箭身长度
     */
    private void initArrowPath(float arrowBodyLength) {
        mArrowPath.reset();
        mArrowPath.moveTo(mCenterX + mArrowBodyWidth, -mFinSlopeHeight);

        mArrowPath.rLineTo(mFinWidth, mFinSlopeHeight);
        mArrowPath.rLineTo(0, -mFinHeight);
        mArrowPath.rLineTo(-mFinWidth, -mFinSlopeHeight);
        mArrowPath.rLineTo(0, -arrowBodyLength);
        mArrowPath.rLineTo(mArrowWidth, 0);
        mArrowPath.rLineTo(-mArrowWidth - mArrowBodyWidth, -mArrowHeight);
        mArrowPath.rLineTo(-mArrowWidth - mArrowBodyWidth, mArrowHeight);
        mArrowPath.rLineTo(mArrowWidth, 0);
        mArrowPath.rLineTo(0, arrowBodyLength);
        mArrowPath.rLineTo(-mFinWidth, mFinSlopeHeight);
        mArrowPath.rLineTo(0, mFinHeight);
        mArrowPath.rLineTo(mFinWidth, -mFinSlopeHeight);
        mArrowPath.close();
    }

    /**
     * 根据当前拖动的进度计算出弓的弯曲角度
     */
    private float getAngleByProgress() {
        //当前角度 = 基本角度 + (可用角度 * 滑动进度)
        return mBaseAngle + (mProgress <= .5F ? 0 :
                mUsableAngle * (mProgress - .5F/*对齐(从0%开始)*/) * 2/*两倍追赶*/);
    }

    private PointF mTempPoint = new PointF();

    /**
     * 根据弓当前弯曲的角度计算新的端点坐标
     *
     * @param angle 弓当前弯曲的角度
     * @return 新的端点坐标
     */
    private PointF getPointByAngle(float angle) {
        //先把角度转成弧度
        double radian = angle * Math.PI / 180;
        //半径 取 弓长的一半
        float radius = mBowLength / 2;
        //x轴坐标值
        float x = (float) (mCenterX + radius * Math.cos(radian));
        //y轴坐标值
        float y = (float) (radius * Math.sin(radian));
        mTempPoint.set(x, y);
        return mTempPoint;
    }

    /**
     * 分解Path
     *
     * @return Path上的全部坐标点
     */
    private float[] decomposePath(PathMeasure pathMeasure) {
        if (pathMeasure.getLength() == 0) {
            return new float[0];
        }
        final float pathLength = pathMeasure.getLength();
        int numPoints = (int) (pathLength / mPrecision) + 1;
        float[] points = new float[numPoints * 2];
        final float[] position = new float[2];
        int index = 0;
        float distance;
        for (int i = 0; i < numPoints; ++i) {
            distance = (i * pathLength) / (numPoints - 1);
            pathMeasure.getPosTan(distance, position, null);
            points[index] = position[0];
            points[index + 1] = position[1];
            index += 2;
        }
        return points;
    }

    /**
     * 设置{@link #decomposePath(PathMeasure)}分解的点密度(单位: px)
     *
     * @param precision 新密度
     */
    public void setPrecision(float precision) {
        mPrecision = precision;
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(@FloatRange(from = 0F, to = 1F) float progress) {
        //只有普通状态和拖动状态下才能设置进度
        if (mState == STATE_NORMAL || mState == STATE_DRAGGING) {
            mState = STATE_DRAGGING;
            if (progress > 1) {
                progress = 1;
            } else if (progress < 0) {
                progress = 0;
            }
            this.mProgress = progress;
            invalidateSelf();
        }
    }

    /**
     * 获取线条的坠落时长
     */
    public int getBaseLinesFallDuration() {
        return mBaseLinesFallDuration;
    }

    /**
     * 设置线条的坠落时长
     */
    public void setBaseLinesFallDuration(int duration) {
        this.mBaseLinesFallDuration = duration;
    }

    /**
     * 获取发射中的弓向下移动的时长
     */
    public long getFiringBowFallDuration() {
        return mFiringBowFallDuration;
    }

    /**
     * 设置发射中的弓向下移动的时长
     */
    public void setFiringBowFallDuration(long duration) {
        this.mFiringBowFallDuration = duration;
    }

    /**
     * 获取发射后的箭收缩动画时长
     */
    public long getFiredArrowShrinkDuration() {
        return mFiredArrowShrinkDuration;
    }

    /**
     * 设置发射后的箭收缩动画时长
     */
    public void setFiredArrowShrinkDuration(long duration) {
        this.mFiredArrowShrinkDuration = duration;
    }

    /**
     * 获取发射后的箭每次上下移动的时长
     */
    public long getFiredArrowMoveDuration() {
        return mFiredArrowMoveDuration;
    }

    /**
     * 设置发射后的箭每次上下移动的时长
     */
    public void setFiredArrowMoveDuration(long duration) {
        this.mFiredArrowMoveDuration = duration;
    }

    /**
     * 获取未命中动画时长
     */
    public long getMissDuration() {
        return mMissDuration;
    }

    /**
     * 设置未命中动画时长
     */
    public void setMissDuration(long duration) {
        this.mMissDuration = duration;
    }

    /**
     * 获取命中动画时长
     */
    public long getHitDuration() {
        return mHitDuration;
    }

    /**
     * 设置命中动画时长
     */
    public void setHitDuration(long duration) {
        this.mHitDuration = duration;
    }

    /**
     * 获取命中后每次左右摆动的时间
     */
    public float getSkewDuration() {
        return mSkewDuration;
    }

    /**
     * 设置命中后每次左右摆动的时间
     */
    public void setSkewDuration(float duration) {
        this.mSkewDuration = duration;
    }

    /**
     * 获取坠落的线条颜色
     */
    public int getLineColor() {
        return mLineColor;
    }

    /**
     * 设置坠落的线条颜色
     */
    public void setLineColor(int color) {
        this.mLineColor = color;
        invalidateSelf();
    }

    /**
     * 获取弓颜色
     */
    public int getBowColor() {
        return mBowColor;
    }

    /**
     * 设置弓颜色
     */
    public void setBowColor(int color) {
        this.mBowColor = color;
        invalidateSelf();
    }

    /**
     * 获取弦颜色
     */
    public int getStringColor() {
        return mStringColor;
    }

    /**
     * 设置弦颜色
     */
    public void setStringColor(int color) {
        this.mStringColor = color;
        invalidateSelf();
    }

    /**
     * 获取箭颜色
     */
    public int getArrowColor() {
        return mArrowColor;
    }

    /**
     * 设置箭颜色
     */
    public void setArrowColor(int color) {
        this.mArrowColor = color;
        invalidateSelf();
    }

    /**
     * 获取命中后左右摆动的幅度
     */
    public float getSkewTan() {
        return mSkewTan;
    }

    /**
     * 设置命中后左右摆动的幅度(正切值)
     */
    public void setSkewTan(float tan) {
        this.mSkewTan = tan;
    }

    /**
     * 获取命中后一共要摆动的次数
     */
    public int getMaxSkewCount() {
        return mMaxSkewCount;
    }

    /**
     * 设置命中后一共要摆动的次数
     */
    public void setMaxSkewCount(int count) {
        this.mMaxSkewCount = count;
    }

    /**
     * 坠落的线条
     */
    private static class Line {

        long duration;//坠落的时长
        long startTime;//开始坠落的时间
        float distance;//坠落的总距离

        float startX;//线条端点x坐标
        float startY;//线条端点y坐标
        float height;//线条高度
        float endX;//线条端点x坐标
    }
}
