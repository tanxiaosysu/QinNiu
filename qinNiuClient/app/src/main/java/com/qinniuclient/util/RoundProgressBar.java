package com.qinniuclient.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.qinniuclient.R;

/**
 * 仿iphone带进度的进度条，线程安全的View，可直接在线程中更新进度
 *
 * @author xiaanming
 */
public class RoundProgressBar extends View {
    /**
     * 文字
     */
    private String mText;

    /**
     * 画笔对象的引用
     */
    private Paint paint;

    /**
     * 圆环的颜色
     */
    private int roundColor;

    /**
     * 圆环进度的颜色
     */
    private int roundProgressColor;

    /**
     * 中间进度百分比的字符串的颜色
     */
    private int textColor;

    /**
     * 中间进度百分比的字符串的字体
     */
    private float textSize;

    /**
     * 圆环的宽度
     */
    private float roundWidth;

    /**
     * 最大进度
     */
    private int max;

    /**
     * 当前进度
     */
    private int progress;
    /**
     * 是否显示中间的进度
     */
    private boolean textIsDisplayable;

    /**
     * 进度的风格，实心或者空心
     */
    private int style;

    public static final int STROKE = 0;
    public static final int FILL = 1;

    public RoundProgressBar(Context context) {
        this(context, null);
    }

    public RoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paint = new Paint();


        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RoundProgressBar);

        //获取自定义属性和默认值
        roundColor = mTypedArray.getColor(R.styleable.RoundProgressBar_roundColor, Color.BLACK);
        roundProgressColor =
                mTypedArray.getColor(R.styleable.RoundProgressBar_roundProgressColor, Color.GREEN);
        textColor = mTypedArray.getColor(R.styleable.RoundProgressBar_textColor, Color.WHITE);
        textSize = mTypedArray.getDimension(R.styleable.RoundProgressBar_textSize, 18);
        roundWidth = mTypedArray.getDimension(R.styleable.RoundProgressBar_roundWidth, 5);
        max = mTypedArray.getInteger(R.styleable.RoundProgressBar_max, 100);
        textIsDisplayable =
                mTypedArray.getBoolean(R.styleable.RoundProgressBar_textIsDisplayable, true);
        style = mTypedArray.getInt(R.styleable.RoundProgressBar_style, 0);
        mTypedArray.recycle();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /**
         * 指定圆心及半径
         */
        int centre = getWidth() / 2; //获取圆心的x坐标
        int radius = (int) (centre - roundWidth / 2); //圆环的半径

        /**
         * 画进度百分比
         */
        Paint textPaint = new Paint();
        textPaint.setStrokeWidth(0);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD); //设置字体
        String percent = mText;
        float textWidth = textPaint.measureText(percent + "分");   //测量字体宽度，我们需要根据字体的宽度设置在圆环中间

        if (textIsDisplayable && percent != null && style == STROKE) {
            canvas.drawText(percent + "分", centre - textWidth / 2, centre + textSize / 2,
                    textPaint); //画出进度百分比
        }

        /**
         * 进度为0，退出
         */
        if (progress == 0) {
            return;
        }

        /**
         * 画最外层的大圆环
         */
        Paint bigCirclePaint = new Paint();
        bigCirclePaint.setColor(roundColor); //设置圆环的颜色
        bigCirclePaint.setStyle(Paint.Style.STROKE); //设置空心
        bigCirclePaint.setStrokeWidth(roundWidth); //设置圆环的宽度
        bigCirclePaint.setAntiAlias(true);  //消除锯齿
        canvas.drawCircle(centre, centre, radius, bigCirclePaint); //画出圆环

        Log.e("log", centre + "");

        /**
         * 画圆弧 ，画圆环的进度
         */

        //设置进度是实心还是空心
        paint.setStrokeWidth(roundWidth); //设置圆环的宽度
        paint.setColor(roundProgressColor);  //设置进度的颜色
        RectF oval = new RectF(centre - radius, centre - radius,
                centre + radius, centre + radius);  //用于定义的圆弧的形状和大小的界限

        // gradient effect
        LinearGradient lg = new LinearGradient(centre - radius, centre,
                centre + radius, centre, Color.rgb(130, 230, 220),
                Color.rgb(210, 210, 140), Shader.TileMode.MIRROR);
        paint.setShader(lg);

        paint.setStyle(Paint.Style.STROKE); // hollow circle
        canvas.drawArc(oval, 270, -(360 * (float) progress / max), false, paint);  // anti-clockwise

        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL_AND_STROKE); //设置实心
        p.setStrokeWidth(roundWidth); //设置圆环的宽度
        p.setAntiAlias(true);  //消除锯齿
        float angle = 360 * (float) progress / max; // calculate the angle
        float x = centre - (float) (Math.sin(Math.PI * angle / 180)) * radius;
        float y = centre - (float) (Math.cos(Math.PI * angle / 180)) * radius;
        canvas.drawCircle(x, y, 2, p); // draw the end point
    }

    public synchronized int getMax() {
        return max;
    }

    /**
     * 设置进度的最大值
     *
     * @param max
     */
    public synchronized void setMax(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max not less than 0");
        }
        this.max = max;
    }

    /**
     * 获取进度.需要同步
     *
     * @return
     */
    public synchronized int getProgress() {
        return progress;
    }

    /**
     * 设置进度，此为线程安全控件，由于考虑多线的问题，需要同步 刷新界面调用postInvalidate()能在非UI线程刷新
     *
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        if (progress < 0) {
            throw new IllegalArgumentException("progress not less than 0");
        }
        if (progress > max) {
            progress = max;
        }
        if (progress <= max) {
            this.progress = progress;
            postInvalidate();
        }

    }


    public int getCricleColor() {
        return roundColor;
    }

    public void setCricleColor(int cricleColor) {
        this.roundColor = cricleColor;
    }

    public int getCricleProgressColor() {
        return roundProgressColor;
    }

    public void setCricleProgressColor(int cricleProgressColor) {
        this.roundProgressColor = cricleProgressColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public float getRoundWidth() {
        return roundWidth;
    }

    public void setRoundWidth(float roundWidth) {
        this.roundWidth = roundWidth;
    }

    public void setText(String text) {
        mText = text;
    }

    public String getText() {
        return mText;
    }
}
