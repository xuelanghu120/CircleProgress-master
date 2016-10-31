package com.ethanco.circleprogresslibrary;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.OvershootInterpolator;


/**
 *  圆形的进度条
 */
public class CircleProgress extends View {
    protected float maxProgress; //最大进度
    protected float currProgress; //现在的进度

    protected int mWidth;
    protected int mHeight;
    protected PointF mCenter;
    //进度条宽度
    protected float mStrokeWidth;
    protected RectF mPaintRectF;

    //前景色起始颜色
    private int foreStartColor;
    //前景色结束颜色
    private int foreEndColcor;
    private Paint forePaint;

    private int bgPaintColor;
    private Paint bgPaint;
    //进度条初始位置
    private int progressInitialPosition;
    //是否使用动画
    protected boolean useAnimation;
    //动画的执行时间
    protected int ANIMATION_DURATION = 1000;
    //是否使用渐变
    protected boolean useGradient;
    //边角是否是圆的
    private boolean isCircleCorner;
    //是否是实心的
    protected boolean isSolid;
    //刻度值文本
    private String mScaleValueTxt;
    //刻度值文本画笔
    private Paint mScallValueTextPaint;
    //刻度值文本宽度
    private float mScallValueTxtWidth;
    //刻度文本个数
    private int mScallValueCount;
    //刻度尺文本开始的角度
    private int mScallStarteAngle;
    //刻度尺文本如果是360度需要+1
    private int mScallValueCountFull= 1;


    public CircleProgress(Context context) {
        this(context, null);
    }

    public CircleProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVar(context, attrs);
        init();
    }

    protected void initVar(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress);
        bgPaintColor = ta.getColor(R.styleable.CircleProgress_bgColor, Color.GRAY);
        foreStartColor = ta.getColor(R.styleable.CircleProgress_foreStartColor, Color.BLUE);
        foreEndColcor = ta.getColor(R.styleable.CircleProgress_foreEndColor, Color.BLUE);
        maxProgress = ta.getInteger(R.styleable.CircleProgress_maxProgress, 270);
        mStrokeWidth = ta.getDimension(R.styleable.CircleProgress_progressWidth, 12);
        currProgress = ta.getInteger(R.styleable.CircleProgress_currProgress, 160);
        progressInitialPosition = ta.getInteger(R.styleable.CircleProgress_progressInitialPosition, 135);
        useAnimation = ta.getBoolean(R.styleable.CircleProgress_useAnimation, true);
        useGradient = ta.getBoolean(R.styleable.CircleProgress_useGradient, true);
        isCircleCorner = ta.getBoolean(R.styleable.CircleProgress_isCircleCorner, true);
        isSolid = ta.getBoolean(R.styleable.CircleProgress_isSolid, false);


        mScallValueCount = ta.getInteger(R.styleable.CircleProgress_mScallValueCount,8);
        mScallStarteAngle = ta.getInteger(R.styleable.CircleProgress_mScallStarteAngle, -45);
        ta.recycle();

        //检查值是否合理
        maxProgress = maxProgress >= 0 && maxProgress < 360 ? maxProgress : 360;
        currProgress = currProgress <= maxProgress && currProgress >= 0 ? currProgress : maxProgress;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initViewSize();
    }

    protected void initViewSize() {
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        //防止宽高不一致
        if (mWidth > mHeight) {
            mWidth = mHeight;
        } else {
            mHeight = mWidth;
        }
        mCenter = new PointF(mWidth / 2, mHeight / 2);
        mPaintRectF = new RectF(0 + mStrokeWidth, 0 + mStrokeWidth, mWidth - mStrokeWidth, mHeight - mStrokeWidth);

        if (useGradient) {
            LinearGradient gradient = new LinearGradient(0, 0, mWidth, mHeight, foreEndColcor, foreStartColor, Shader.TileMode.CLAMP);
            forePaint.setShader(gradient);
        } else {
            forePaint.setColor(foreStartColor);
        }
    }

    protected void init() {
        initForePaint();
        initBgPaint();
        initScall();
    }

//初始化刻度尺的信息
    private void initScall() {
        initTextPaint();
        if(maxProgress==360){
            mScallValueCountFull = 0;
        }
    }

    private void initTextPaint() {
        mScallValueTextPaint = new Paint();
        mScallValueTextPaint.setTextSize(40);
        mScallValueTextPaint.setColor(Color.BLACK);
    }

    protected void initForePaint() {
        forePaint = new Paint();
        setCommonPaint(forePaint, isSolid, isCircleCorner);
    }



    protected void initBgPaint() {
        bgPaint = new Paint();
        bgPaint.setColor(bgPaintColor);
        setCommonPaint(bgPaint, isSolid, isCircleCorner);
    }

    /**
     *
     * @param paint 画笔
     * @param isSolid 是否空心
     * @param isRound 边角是否是圆的
     */
    protected void setCommonPaint(Paint paint, boolean isSolid, boolean isRound) {
        paint.setAntiAlias(true);
        paint.setStrokeWidth(mStrokeWidth);
        if (!isSolid) {
            paint.setStyle(Paint.Style.STROKE);
        }
        if (isRound) {
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
    }

    protected void valueAnimator(final float originProgress, final float endProgress) {
        ValueAnimator mValueAnim = ValueAnimator.ofInt(1);
        mValueAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator mAnim) {
                float fraction = mAnim.getAnimatedFraction();
                Integer newProgress = evaluate(fraction, (int) originProgress, (int) endProgress);
                currProgress = newProgress;
                //防止超越最大值
                if (currProgress >= 0 && currProgress <= maxProgress) {
                    invalidate();
                }
            }
        });

        mValueAnim.setInterpolator(new OvershootInterpolator());
        int duration = (int) (ANIMATION_DURATION * (Math.abs((endProgress - originProgress)) / maxProgress));
        mValueAnim.setDuration(duration);
        mValueAnim.start();
    }

    protected Integer evaluate(float fraction, Integer startValue, Integer endValue) {
        int startInt = startValue;
        return (int) (startInt + fraction * (endValue - startInt));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.rotate(progressInitialPosition, mCenter.x, mCenter.y);//旋转开始的角度
        canvas.drawArc(mPaintRectF, 0, maxProgress, isSolid, bgPaint);//画一个背景色
        canvas.drawArc(mPaintRectF, 0, currProgress, isSolid, forePaint);//画一个前景色
        //已经旋转progressInitialPosition 135，所以文字的旋转要在加90


        for (int i = 1; i <= mScallValueCount; i++) {
            canvas.save();// 保存当前画布
            canvas.rotate(-progressInitialPosition-mScallStarteAngle + (maxProgress /(mScallValueCount-mScallValueCountFull)  *(i-1)), mCenter.x, mCenter.y);
            mScaleValueTxt = String.valueOf(i * 100);
            mScallValueTxtWidth = mScallValueTextPaint.measureText(mScaleValueTxt, 0, mScaleValueTxt.length());
            canvas.drawText(mScaleValueTxt + "", mCenter.x- mScallValueTxtWidth /2, mCenter.y + mHeight/2  - getDpValue(10)  -mStrokeWidth, mScallValueTextPaint);
            canvas.restore();//
        }
    }
    private int getDpValue(int w) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, w, getContext().getResources().getDisplayMetrics());
    }


    /**
     * 设置进度
     * @param progress 0到100
     */
    public void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("progress must >=0 && <=100，now progress is " + progress);
        }

        float originProgress = currProgress;
        currProgress = progress / 100F * maxProgress;
        if (useAnimation) {
            valueAnimator(originProgress, currProgress);
        } else {
            invalidate();
        }
    }
}
