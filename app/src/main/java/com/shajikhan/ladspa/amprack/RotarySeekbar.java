package com.shajikhan.ladspa.amprack;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import static com.google.android.material.theme.overlay.MaterialThemeOverlay.wrap;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/**
 * Rotary Seekbar Widget with zoom-on-touch for Android.
 * Created by André on 18.02.2015.
 *
 */
public class RotarySeekbar extends View {

    private static final String TAG = "RotarySeekbar";

    private static final int DEFAULT_STYLE_RES = R.style.RotarySeekbar_DefaultMaterialStyle;

    private static final int SCROLL_ANGULAR_SCALE_DP = 48;
    private final int OVERLAY_PADDING_DP = 12;
    private static final int ROTATION_SNAP_BUFFER = 30;
    // Used when mSectorHalfOpening is small, to prevent jumping from max to min value too quick.

    private final int DEFAULT_SEEKBAR_DIAMETER = 88;
    // 96dp allowing for the standard 4dp padding on each side.

    private final int OPENING_TEXT_MARGIN = dpToPx(2);
    private boolean mbScrolling = false;

    private int mNumSteps = 1;
    private float mMinValue = 0;
    private float mMaxValue = 100;
    private int mValueNumDigits = 1;

    private String mValueStr;
    private String mUnitStr = "";
    private float mValue = 50;
    private float mTextSize = spToPx(20);
    private float mTextWidth = 0.0f;
    private float mTextHeight = 0.0f;

    private boolean mNeedleOnTop = true;
    private float mKnobRadius = 0.3f;

    private int mSectorRotation = 0; // degrees. Extra rotation of the Seekbar. User set.
    private float mSectorHalfOpening = 30; // degrees
    private float mSectorMinRadiusScale = 0.4f;
    private float mSectorMajRadiusScale = 0.75f;
    private float mTickMinRadiusScale = 0.8f;
    private float mTickMajRadiusScale = 1.0f;

    private boolean mShowValue = true;
    private boolean mShowNeedle = true;
    private boolean mShowKnob = true;
    private boolean mShowTicks = true;
    private boolean mShowSector = true;
    private boolean mSubtractTicks = true;
    private boolean mShowUnit = true;

    private boolean mTrackValue = false;
    private float mStartScrollValue;

    private int mNumTicks = 2; // +1 sections

    private int mKnobColor = 0xff666666;
    private int mTextColor = 0xff000000;
    private int mSectorColor = 0xffdddddd;
    private int mValueSectorColor = 0xffaaaaaa;
    private int mTicksColor = 0xff006699;
    private int mNeedleColor = 0xff880000;

    private int mOverlaySurfaceColor = 0xffffffff;

    private float mNeedleWidth = dpToPx(4);
    private float mTicksWidth = dpToPx(4);
    private float mTicksSubtractWidth = dpToPx(2);

    private float mNeedleMinorRadius = 0.0f;
    private float mNeedleMajorRadius = 1.0f;

    private float mOverlayBorderMargin = dpToPx(4);

    private Rect mOverlayGlobalBounds = new Rect();
    private final int mOverlaySizeDP = 192; // size of overlay in dp-s.

    private enum ValuePosition {
        Bottom(0),
        Left(1),
        Top(2),
        Right(3),
        Center(4);

        int id;

        ValuePosition(int id) {
            this.id = id;
        }

        static ValuePosition fromId(int id) {
            for (ValuePosition vp : values()) {
                if (vp.id == id)
                    return vp;
            }
            return Bottom;
        }
    }

    private ValuePosition mValuePosition = ValuePosition.Bottom;
    private float mRotation = 0.0f;
    private float mAccumulatedAngleChange;

    private RotarySeekbarDrawable mOverlaySeekbarProxy;
    private RotarySeekbarImpl mLayedOutSeekbar;
    private RotarySeekbarImpl mOverlaySeekbar;
    private LayerDrawable mOverlay;

    private Paint mSectorPaint;
    private Paint mValueSectorPaint;
    private Paint mKnobPaint;

    private GestureDetector mDetector;
    private OnValueChangedListener mListener = null;

    public interface OnValueChangedListener {
        void onValueChanged(RotarySeekbar sourceSeekbar, float value);
    }

    public RotarySeekbar(Context context) {
        this(context, null);
    }

    void setMinValue (float __value__) {
        mMinValue = __value__ ;
    }

    void setMaxValue (float __value__ ) {
        mMaxValue = __value__ ;
    }

    public RotarySeekbar(@NonNull Context context, @NonNull AttributeSet attributeSet)
    {
        this(context, attributeSet, DEFAULT_STYLE_RES);
    }

    public RotarySeekbar(@NonNull Context context, @NonNull AttributeSet attributeSet, int defStyleAttr) {
        super(wrap(context, attributeSet, defStyleAttr, DEFAULT_STYLE_RES), attributeSet, defStyleAttr);
        context = getContext();

        final TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.RotarySeekbar, defStyleAttr, DEFAULT_STYLE_RES);
        try{
            mShowValue = a.getBoolean(R.styleable.RotarySeekbar_showValue, mShowValue);
            mShowUnit = a.getBoolean(R.styleable.RotarySeekbar_showUnit, mShowUnit);
            mValueNumDigits = a.getInteger(R.styleable.RotarySeekbar_valueNumDigits, mValueNumDigits);
            mUnitStr = a.getString(R.styleable.RotarySeekbar_unit);
            mMinValue = a.getFloat(R.styleable.RotarySeekbar_valueMin, mMinValue);
            mMaxValue = a.getFloat(R.styleable.RotarySeekbar_valueMax, mMaxValue);
            assert(mMinValue != mMaxValue);

            mValue = a.getFloat(R.styleable.RotarySeekbar_value, mValue);
            mValuePosition = ValuePosition.fromId(a.getInt(R.styleable.RotarySeekbar_valuePosition, ValuePosition.Bottom.id));

            mNumSteps = a.getInteger(R.styleable.RotarySeekbar_valueNumSteps, mNumSteps);
            if (mNumSteps < 1)
                mNumSteps = 1;

            mTextColor = a.getColor(R.styleable.RotarySeekbar_textColor, mTextColor);
            mTextSize = a.getDimension(R.styleable.RotarySeekbar_textSize, mTextSize);

            mTrackValue = a.getBoolean(R.styleable.RotarySeekbar_trackValue, mTrackValue);
            mShowKnob = a.getBoolean(R.styleable.RotarySeekbar_showKnob, mShowKnob);
            mKnobRadius = a.getFloat(R.styleable.RotarySeekbar_knobRadius, mKnobRadius);
            mKnobColor = a.getColor(R.styleable.RotarySeekbar_knobColor, mKnobColor);

            mOverlaySurfaceColor = a.getColor(R.styleable.RotarySeekbar_overlaySurfaceColor, mOverlaySurfaceColor);

            mShowSector = a.getBoolean(R.styleable.RotarySeekbar_showSector, mShowSector);
            mSectorHalfOpening = 0.5f*a.getFloat(R.styleable.RotarySeekbar_sectorOpenAngle, 2.0f*mSectorHalfOpening);
            mSectorRotation = a.getInt(R.styleable.RotarySeekbar_sectorRotation, mSectorRotation);
            mSectorMinRadiusScale = a.getFloat(R.styleable.RotarySeekbar_sectorMinorRadius, mSectorMinRadiusScale);
            mSectorMajRadiusScale = a.getFloat(R.styleable.RotarySeekbar_sectorMajorRadius, mSectorMajRadiusScale);
            mSectorColor = a.getColor(R.styleable.RotarySeekbar_sectorRangeColor, mSectorColor);
            mValueSectorColor = a.getColor(R.styleable.RotarySeekbar_sectorValueColor, mValueSectorColor);

            mShowTicks = a.getBoolean(R.styleable.RotarySeekbar_showTicks, mShowTicks);
            mSubtractTicks = a.getBoolean(R.styleable.RotarySeekbar_ticksSubtract, mSubtractTicks);
            mTickMinRadiusScale = a.getFloat(R.styleable.RotarySeekbar_ticksMinorRadius, mTickMinRadiusScale);
            mTickMajRadiusScale = a.getFloat(R.styleable.RotarySeekbar_ticksMajorRadius, mTickMajRadiusScale);
            mTicksWidth = a.getDimension(R.styleable.RotarySeekbar_ticksThickness, mTicksWidth);
            mTicksSubtractWidth = a.getDimension(R.styleable.RotarySeekbar_ticksSubtractionThickness, mTicksSubtractWidth);
            mTicksColor = a.getColor(R.styleable.RotarySeekbar_ticksColor, mTicksColor);
            mNumTicks = a.getInteger(R.styleable.RotarySeekbar_numTicks, mNumTicks);

            mShowNeedle = a.getBoolean(R.styleable.RotarySeekbar_showNeedle, mShowNeedle);
            mNeedleColor = a.getColor(R.styleable.RotarySeekbar_needleColor, mNeedleColor);
            mNeedleWidth = a.getDimension(R.styleable.RotarySeekbar_needleThickness, mNeedleWidth);
            mNeedleMinorRadius = a.getFloat(R.styleable.RotarySeekbar_needleMinorRadius, mNeedleMinorRadius);
            mNeedleMajorRadius = a.getFloat(R.styleable.RotarySeekbar_needleMajorRadius, mNeedleMajorRadius);

            mNeedleOnTop = a.getBoolean(R.styleable.RotarySeekbar_needleOnTop, mNeedleOnTop);

            mOverlayBorderMargin = a.getDimension(R.styleable.RotarySeekbar_overlayBorderMargin, mOverlayBorderMargin);

        } finally {
            a.recycle();
        }

        if (mNeedleMinorRadius == mNeedleMajorRadius)
            mNeedleMinorRadius = mNeedleMajorRadius*0.999f; // sometimes the line isn't drawn if they are equal

        init();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putFloat("value", mValue);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {

        if(state instanceof Bundle) {
            Bundle bundle = (Bundle)state;
            state = bundle.getParcelable("instanceState");

            mValue = bundle.getFloat("value");
            mRotation = valueToRotation();
            updateText();
        }
        super.onRestoreInstanceState(state);
    }

    private void init() {
        setLayerToSW(this);

        mOverlaySeekbarProxy = new RotarySeekbarDrawable(); // uses mOverlaySeekbar for drawing

        ShapeAppearanceModel.Builder builder = ShapeAppearanceModel.builder();
        builder.setAllCorners(CornerFamily.ROUNDED, dpToPx(20));
        MaterialShapeDrawable materialOverlay = new MaterialShapeDrawable(builder.build());
        // TODO: fix elevation shadow. It doesn't appear at all.
        /*materialOverlay.initializeElevationOverlay(getContext());
        materialOverlay.setShadowCompatibilityMode(MaterialShapeDrawable.SHADOW_COMPAT_MODE_DEFAULT);
        materialOverlay.setElevation(dpToPx(4));*/

//        I commented below to hide overlay
//        materialOverlay.setStroke(dpToPx(2), 0x66000000);
        materialOverlay.setFillColor(ColorStateList.valueOf(mOverlaySurfaceColor));
        mOverlay = new LayerDrawable(new Drawable[]{
                materialOverlay,
                mOverlaySeekbarProxy
        });

        // Make space for elevation shadow. Untested; this might clip the layer anyway.
        /*final int pad = dpToPx(OVERLAY_PADDING_DP);
        mOverlay.setLayerInset(0, pad, pad, pad, pad);*/

        checkValueBounds();
        mValue = snapValueToSteps(mValue);
        mRotation = valueToRotation();
        updateText();

        Paint tmpTextPaint = getTextPaint(1.0f);
        // These sizes are needed upon measuring
        mTextHeight = tmpTextPaint.getTextSize();
        mTextWidth = getTextWidth(tmpTextPaint);

        // These paints does not change between overlay and displayed widget:
        mSectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSectorPaint.setStyle(Paint.Style.FILL);
        mSectorPaint.setColor(mSectorColor);
        mValueSectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mValueSectorPaint.setStyle(Paint.Style.FILL);
        mValueSectorPaint.setColor(mValueSectorColor);

        mKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mKnobPaint.setStyle(Paint.Style.FILL);
        mKnobPaint.setColor(mKnobColor);

        mDetector = new GestureDetector(RotarySeekbar.this.getContext(), new mGestureListener());
        mDetector.setIsLongpressEnabled(false);
    }

    private String formatValueString(float value) {
        String res = String.format("%." + mValueNumDigits + "f", value);
        if(mShowUnit && mUnitStr != null && !mUnitStr.equals(""))
            res += mUnitStr;
        return res;
    }

    private void updateText() {
        mValueStr = formatValueString(mValue);
    }

    private void checkValueBounds() {
        if(mMaxValue < mMinValue) {
            float tmp = mMinValue;
            mMinValue = mMaxValue;
            mMaxValue = tmp;
        }

        if(mValue > mMaxValue)
            mValue = mMaxValue;
        else if(mValue < mMinValue)
            mValue = mMinValue;
    }

    public float snapValueToSteps(float value) {
        final float VALUE_STEP_SIZE = (mMaxValue-mMinValue)/mNumSteps;
        return ( mMinValue+VALUE_STEP_SIZE*Math.round( (value-mMinValue)/VALUE_STEP_SIZE ));
    }

    public void setLayerToSW(View v) {
        if(!v.isInEditMode() && Build.VERSION.SDK_INT >= 11)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setLayerToHW(View v) {
        if(!v.isInEditMode() && Build.VERSION.SDK_INT >= 11)
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        mListener = listener;
        listener.onValueChanged(this, mValue);
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        mValue = value;
        checkValueBounds();
        mValue = snapValueToSteps(mValue);
        mRotation = valueToRotation();
        updateText();

        if(!mbScrolling) {
            if (mLayedOutSeekbar != null)
                mLayedOutSeekbar.recreatePaths();
            invalidate();
        }else {
            if(mOverlaySeekbar != null)
                mOverlaySeekbar.recreatePaths();
            mOverlaySeekbarProxy.invalidateSelf();
        }
    }

    public void setValueByStep(int step) {
        // setValue will enforce clamping of the value
        setValue(mMinValue+step*(mMaxValue-mMinValue)/mNumSteps);
    }

    public int getCurrentStep() {
        return Math.round((mValue-mMinValue)/(mMaxValue-mMinValue)*mNumSteps);
    }

    public int getNumSteps() {
        return mNumSteps;
    }

    public float getMinValue() {
        return mMinValue;
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!mbScrolling)
            mLayedOutSeekbar.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //if(!changed) return;
        /* this short circuit wasn't correct; while the left an top values might not have
         * changed, the global position (getLocationOnScreen()) might actually have changed.
         */
        RectF bounds = mLayedOutSeekbar.getBounds();
        float aspectRatio = bounds.width()/bounds.height();
        calculateOverlayBounds(aspectRatio);

        int padding = dpToPx(OVERLAY_PADDING_DP);
        RectF overlayBounds = new RectF(0,0,
                mOverlayGlobalBounds.width()-2*padding,
                mOverlayGlobalBounds.height()-2*padding);
        float overlayRelativeScale = overlayBounds.width()/bounds.width();
        mOverlaySeekbar = new RotarySeekbarImpl(overlayBounds, overlayRelativeScale);
        mOverlay.setBounds(mOverlayGlobalBounds);
    }

    @Override
    public void onWindowSystemUiVisibilityChanged(int visible) {
        super.onWindowSystemUiVisibilityChanged(visible);

        if (0 != (visible &
                (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN))
        ) {
            requestLayout();
            /* Sometimes onLayout() isn't called when, say, the navbar is hidden. I suspect that
             * e.g. ConstraintLayout might move the global screen position of views without actually
             * calling onLayout() for the child views, as the position within the container hasn't
             * changed.
             */
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float xpad = (float)(getPaddingLeft()+getPaddingRight());
        float ypad = (float)(getPaddingTop()+getPaddingBottom());
        float ww = (float)w-xpad;
        float hh = (float)h-ypad;

        RectF bounds = new RectF(0, 0, ww, hh);
        bounds.offsetTo(getPaddingLeft(), getPaddingTop());
        mLayedOutSeekbar = new RotarySeekbarImpl(bounds);

        // The overlay's bounds is calculated upon layout to get the proper global position of this view.
    }

    public float clampRotation(float rotation) {
        // TODO: should allow rotation == 360.
        rotation %= 360;
        if(rotation < 0) rotation+=360;
        return rotation;
    }

    public float valueToRotation() {
        float rotation = (270- mSectorHalfOpening -(mValue-mMinValue)/(mMaxValue-mMinValue)*(360-2* mSectorHalfOpening));
        return clampRotation(rotation);
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        int res = 0;
        switch(mValuePosition) {
            case Bottom:
            case Top:
                res = (int)(mShowValue?mTextHeight:0)+getSuggestedMinimumWidth();
                break;
            case Center:
            case Right:
            case Left:
                res = (int)mTextHeight*2;
                break;
        }
        return res;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        int res = 0;
        switch(mValuePosition) {
            case Bottom:
            case Top:
                if(mShowValue)
                    res = (int)mTextWidth;
                else
                    res = getSuggestedMinimumHeight();
                break;
            case Center:
                res = getSuggestedMinimumHeight();
                break;
            case Right:
            case Left:
                res = (int)(mShowValue ? mTextWidth:0)+getSuggestedMinimumHeight();
                break;
        }
        return res;
    }

    /**
     * Get the suggested height of this view given a determined width.
     * @param width View's width (without padding)
     * @param maxHeight Maximum allowed height
     * @return suggested height (without padding)
     */
    private int getSuggestedHeight(int width, int maxHeight) {
        if(width<=0 || maxHeight<=0)
            return 0;

        int h = width;
        switch (mValuePosition) {
            case Center:
                // Do nothing, try to have same height as width
                break;
            case Bottom:
            case Top:
                if(mShowValue)
                    h += (int) mTextHeight;
                break;
            case Right:
            case Left:
                if(mShowValue) {
                    int rw = width - (int) mTextWidth;
                    // real width of Seekbar
                    h = Math.max((rw > 0 ? rw : 0), (int) mTextHeight);
                    // biggest of Seekbar width or text height
                }else
                    h = width;
                break;
        }
        if(h>maxHeight)
            h = maxHeight;
        return h;
    }

    public float getTextOffset(float textSize, float expectedRadius) {
        float offset = OPENING_TEXT_MARGIN+0.5f * (textSize) / (float) Math.tan(mSectorHalfOpening / 180.0d * Math.PI);

        final float expectedKnobRadius = mKnobRadius*expectedRadius;
        if(mShowKnob && offset < expectedKnobRadius) {
            offset = expectedKnobRadius + OPENING_TEXT_MARGIN;
        }

        if (offset > expectedRadius)
            offset = expectedRadius + OPENING_TEXT_MARGIN;
        return offset;
    }

    /**
     * Get the suggested width of this view given a set height.
     * @param height View's height (without padding)
     * @param maxWidth
     * @return suggested width (without padding)
     */
    private int getSuggestedWidth(int height, int maxWidth) {
        if(height <= 0 || maxWidth <=0)
            return 0;

        int w = height;
        switch (mValuePosition) {
            case Center:
                // Do nothing, try to have same width as height
                break;
            case Bottom:
            case Top:
                if(mShowValue) {
                    int rh = height - (int) mTextHeight;
                    w = Math.max((rh > 0 ? rh : 0), (int) mTextHeight);
                }
                break;
            case Right:
            case Left:
                if(mShowValue) {
                    float offset = getTextOffset(mTextSize, 0.5f*height);
                    w += (int) (mTextWidth + offset - 0.5f*w);
                }else
                    w *= 0.5f;
                break;
        }

        if(w>maxWidth)
            w = maxWidth;
        return w;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int specHeight = MeasureSpec.getSize(heightMeasureSpec);

        int xPad = getPaddingLeft()+getPaddingRight();
        int yPad = getPaddingTop()+getPaddingBottom();

        int w=0;
        int h=0;
        //Log.d(TAG, "measurespecs: "+widthMode+", "+heightMode);

        float radii = 0.5f*dpToPx(DEFAULT_SEEKBAR_DIAMETER);
        switch(widthMode) {
            case MeasureSpec.EXACTLY:
                w = specWidth;
                switch(heightMode) {
                    case MeasureSpec.EXACTLY:
                        h = specHeight;
                        break;
                    case MeasureSpec.AT_MOST:
                        h = getSuggestedHeight(w-xPad, specHeight-yPad)+yPad;
                        break;
                    case MeasureSpec.UNSPECIFIED:
                        h = getSuggestedHeight(w-xPad, Integer.MAX_VALUE-yPad)+yPad;
                        break;
                }
                break;
            case MeasureSpec.AT_MOST:
                switch(heightMode) {
                    case MeasureSpec.EXACTLY:
                        h = specHeight;
                        w = getSuggestedWidth(h - yPad, specWidth - xPad) + xPad;
                        break;
                    case MeasureSpec.AT_MOST:
                        h = dpToPx(DEFAULT_SEEKBAR_DIAMETER) + yPad;
                        w = getSuggestedWidth(h - yPad, specWidth - xPad) + xPad;
                        break;
                    case MeasureSpec.UNSPECIFIED:
                        w = specWidth;
                        h = getSuggestedHeight(w-xPad, Integer.MAX_VALUE-yPad)+yPad;
                        break;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                switch(heightMode) {
                    case MeasureSpec.EXACTLY:
                        h = specHeight;
                        w = getSuggestedWidth(h-yPad, Integer.MAX_VALUE-xPad)+xPad;
                        break;
                    case MeasureSpec.AT_MOST:
                        h = dpToPx(DEFAULT_SEEKBAR_DIAMETER) + yPad;
                        w = getSuggestedWidth(h-yPad, Integer.MAX_VALUE-xPad)+xPad;
                        break;
                    case MeasureSpec.UNSPECIFIED:
                        h = dpToPx(DEFAULT_SEEKBAR_DIAMETER)+yPad;
                        w = getSuggestedWidth(h-yPad, Integer.MAX_VALUE-xPad)+xPad;
                        break;
                }
                break;
        }

        setMeasuredDimension(w, h);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mDetector.onTouchEvent(event);
        if(!result) {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                getRootView().getOverlay().remove(mOverlay);

                // Make sure we are drawing correctly, once the overlay is removed.
                mLayedOutSeekbar.recreatePaths();
                invalidate();
                mbScrolling = false;

                if(!mTrackValue && mStartScrollValue != mValue && mListener!=null)
                    mListener.onValueChanged(this, mValue);
            }
        }
        return result;
    }

    public float getSeekbarRotation() {
        return mRotation;
    }

    public void addRotationChange(float deltaAlpha) {
        mAccumulatedAngleChange += deltaAlpha;
        setSeekbarRotation(mRotation+mAccumulatedAngleChange);
    }

    public void setSeekbarRotation(float rotation) {
        rotation = clampRotation(rotation);
        float oldRotation = mRotation;
        // make sure we are working with a rotation in [0,360] deg
        boolean forbidden = (rotation > (270- mSectorHalfOpening) && rotation < (270+ mSectorHalfOpening));
        if(mRotation <= (270- mSectorHalfOpening) && forbidden)
            mRotation = 270- mSectorHalfOpening;
        else if(mRotation >= (270+ mSectorHalfOpening) && forbidden)
            mRotation = 270+ mSectorHalfOpening;
        else
            mRotation = rotation;

        float newValue = rotationToValidValue(mRotation);
        final float rotDiff = oldRotation-mRotation;
        boolean snap = false;
        if(oldRotation >= 270 && oldRotation < 360 && mRotation < 270 && mRotation > 180 && mValue != mMinValue) {
            if(rotDiff<ROTATION_SNAP_BUFFER) {
                newValue = mMaxValue;
                snap = true;
            }else if(mValue != mMinValue)
                newValue = mMinValue;
        }else if(oldRotation <= 270 && oldRotation > 180 && mRotation > 270 && mRotation < 360 && mValue != mMaxValue) {
            if(-rotDiff<ROTATION_SNAP_BUFFER) {
                newValue = mMinValue;
                snap = true;
            }else if(mValue != mMaxValue)
                newValue = mMaxValue;
        }

        boolean notify = mValue != newValue;

        mValue = newValue;
        mRotation = valueToRotation(); // move needle to the validated value.
        if(!snap && mRotation != oldRotation)
            mAccumulatedAngleChange = 0.0f;

        updateText();

        if(!mbScrolling) {
            if (mLayedOutSeekbar != null)
                mLayedOutSeekbar.recreatePaths();
            invalidate();
        }else {
            if(mOverlaySeekbar != null)
                mOverlaySeekbar.recreatePaths();
            mOverlaySeekbarProxy.invalidateSelf();
        }

        if(mTrackValue && notify && mListener != null)
            mListener.onValueChanged(this, mValue);
    }

    public float rotationToSweep(float rotation) {
        float sweep = 270.0f- mSectorHalfOpening -rotation;
        if(rotation == 270.0f && mValue == mMaxValue)
            sweep = 360.0f-2* mSectorHalfOpening;
        else if(rotation > 270.0f)
            sweep += 360.0f;
        return sweep;
    }

    public float rotationToValidValue(float rotation) {
        rotation = clampRotation(rotation);
        final float MAX_SWEEP = 360-2* mSectorHalfOpening;
        final float sweepAngle = rotationToSweep(rotation);
        float sweepRatio = sweepAngle / MAX_SWEEP;
        if(sweepRatio > 1.0f)
            sweepRatio = 1.0f;
        else if (sweepRatio < 0.0f)
            sweepRatio = 0.0f;

        return snapValueToSteps((mMaxValue - mMinValue) * sweepRatio + mMinValue);
    }

    public boolean showValue() { return mShowValue; }

    public void setShowValue(boolean show) {
        mShowValue = show;
        invalidate();
    }

    private void calculateOverlayBounds(float aspectRatio) {
        //Log.d("calculateOverlayBounds", getResources().getResourceName(getId()));
        Rect visibleRect = new Rect();
        getGlobalVisibleRect(visibleRect);//, globalOffset);
        //Log.d("calculateOverlayBounds", "getGlobalVisibleRect("+visibleRect.left+", "+visibleRect.top+")");

        View root = getRootView();
        int rootWidth = root.getWidth();
        int rootHeight = root.getHeight();

        int centerX = visibleRect.centerX() + (getPaddingLeft()-getPaddingRight());
        int centerY = visibleRect.centerY() + (getPaddingTop()-getPaddingBottom());
        //Log.d("calculateOverlayBounds", "center: ("+centerX+", "+centerY+")");

        int overlayHeight = dpToPx(mOverlaySizeDP);
        int overlayWidth = (int)(overlayHeight*aspectRatio);
        // TODO: make sure overlay size is not larger than screen:
        //Log.d("calculateOverlayBounds", "overlayWidth, overlayHeight: "+overlayWidth+",\t"+overlayHeight);

        mOverlayGlobalBounds.left = mOverlayGlobalBounds.top = 0; // to avoid bugs below.
        mOverlayGlobalBounds.right = overlayWidth;
        mOverlayGlobalBounds.bottom = overlayHeight;

        int posX, posY;
        if(centerX < (overlayWidth/2+mOverlayBorderMargin))
            posX = (int)mOverlayBorderMargin; // at left edge
        else if(centerX > (rootWidth-overlayWidth/2-mOverlayBorderMargin))
            posX = rootWidth-overlayWidth-(int)mOverlayBorderMargin; // push in from right
        else
            posX = centerX-overlayWidth/2;

        if(centerY < (overlayHeight/2+mOverlayBorderMargin))
            posY = (int)mOverlayBorderMargin; // at top edge
        else if(centerY > (rootHeight-overlayHeight/2-mOverlayBorderMargin))
            posY = rootHeight-overlayHeight-(int)mOverlayBorderMargin; // push in from bottom
        else
            posY = centerY-overlayHeight/2;

        //Log.d("calculateOverlayBounds", "overlay pos("+posX+", "+posY+")\n");
        mOverlayGlobalBounds.offsetTo(posX, posY);
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density+0.5f);
    }

    public int spToPx(int sp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(sp * displayMetrics.scaledDensity);
    }

    class RotarySeekbarDrawable extends Drawable {

        public RotarySeekbarDrawable()
        {
            super();
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            canvas.translate(bounds.left, bounds.top);
            int padding = dpToPx(OVERLAY_PADDING_DP);
            canvas.translate(padding, padding); // a little padding.
            mOverlaySeekbar.draw(canvas);
        }

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter cf) {}

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    class mGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            mbScrolling = true;
            mStartScrollValue = mValue;
            mAccumulatedAngleChange = 0.0f;
            invalidate(); // force redraw, where we don't draw the layed out View (this)
            getRootView().getOverlay().add(mOverlay);
            return true; // must return true for onScroll to be called (!)
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Rect visibleRect = new Rect();
            getGlobalVisibleRect(visibleRect);

            final float cX = mOverlayGlobalBounds.left + dpToPx(OVERLAY_PADDING_DP) + mOverlaySeekbar.center().x;
            final float cY = mOverlayGlobalBounds.top + dpToPx(OVERLAY_PADDING_DP) + mOverlaySeekbar.center().y;
            float vx = (e2.getX()+visibleRect.left) - cX;
            float vy = -((e2.getY()+visibleRect.top) - cY);
            // invert y-coordinates so that up on the screen is positive y (wrt. the overlay center).
            float vxPrev = vx+distanceX;
            float vyPrev = vy-distanceY;

            float deltaAlpha = (float)Math.atan2(vy, vx)-(float)Math.atan2(vyPrev, vxPrev);
            // Correct for -PI to PI jumps (and v.v.) in deltaAlpha:
            if(deltaAlpha > Math.PI)
                deltaAlpha-=2*Math.PI;
            else if(deltaAlpha < -Math.PI)
                deltaAlpha+=2*Math.PI;

            final float vLen = (float)Math.sqrt(vx*vx+vy*vy);
            addRotationChange(deltaAlpha/(float)Math.PI*180.0f * (vLen/dpToPx(SCROLL_ANGULAR_SCALE_DP)));
            //Scale angle with length from center, do give user control.
            // TODO: implement inverse option as selectable attribute.
            return true;
        }
    }

    /**
     * Helper method for translating (x,y) scroll vectors into scalar rotation of the pie.
     *
     * @param dx The x component of the current scroll vector.
     * @param dy The y component of the current scroll vector.
     * @param x  The x position of the current touch, relative to the center.
     * @param y  The y position of the current touch, relative to the center.
     * @return The scalar representing the change in angular position for this scroll.
     */
    private static float vectorToScalarScroll(float dx, float dy, float x, float y) {
        // get the length of the vector
        float l = (float) Math.sqrt(dx * dx + dy * dy);

        // decide if the scalar should be negative or positive by finding
        // the dot product of the vector perpendicular to (x,y).
        float crossX = -y;
        float crossY = x;

        float dot = (crossX * dx + crossY * dy);
        float sign = Math.signum(dot);

        return l * sign;
    }

    public String getUnitStr() { return (mUnitStr == null ? "" : mUnitStr); }

    private Paint getTextPaint(float scaling) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(mTextColor);
        // TODO: Fix font size for overlays
        if(mTextSize <= 0)
            mTextSize = spToPx(10);
        textPaint.setTextSize(mTextSize*scaling);
        return textPaint;
    }

    private int getTextWidth(Paint textPaint) {
        Rect textBounds = new Rect();
        String minString = formatValueString(mMinValue);
        String maxString = formatValueString(mMaxValue);
        textPaint.getTextBounds(minString, 0, minString.length(), textBounds);
        int minStringWidth = textBounds.width();
        textPaint.getTextBounds(maxString, 0, maxString.length(), textBounds);
        int maxStringWidth = textBounds.width();
        return Math.max(minStringWidth, maxStringWidth);
    }

    private class RotarySeekbarImpl {
        private Path mSectorPath;
        private Path mValuePath;
        private float mRadius;
        private RectF mBounds;
        private PointF mSeekbarCenter;

        private Paint mTextPaint;
        private Paint mNeedlePaint;
        private Paint mTicksPaint;

        private float mTextX = 0.0f;
        private float mTextY = 0.0f;
        private float mScaling;

        private float mTextWidth;
        private float mTextHeight;

        public RotarySeekbarImpl( RectF bounds) {
            this(bounds, 1.0f);
        }

        public RotarySeekbarImpl (RectF bounds, float scaling) {
            mBounds = bounds;
            mScaling = scaling;

            mTextPaint = getTextPaint(mScaling);
            mTextHeight = mTextPaint.getTextSize();
            mTextWidth = getTextWidth(mTextPaint);

            mNeedlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mNeedlePaint.setStyle(Paint.Style.STROKE);
            mNeedlePaint.setStrokeCap(Paint.Cap.ROUND);
            mNeedlePaint.setColor(mNeedleColor);
            mNeedlePaint.setStrokeWidth(mNeedleWidth*mScaling);

            mTicksPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mTicksPaint.setStyle(Paint.Style.STROKE);
            mTicksPaint.setColor(mTicksColor);
            mTicksPaint.setStrokeWidth(mTicksWidth*mScaling);

            float dW = bounds.width();
            float dH = bounds.height();
            float cX = bounds.centerX();
            float cY = bounds.centerY();

            float d = Math.min(dW, dH);
            float offset = 0.0f;

            if(mShowValue) {
                switch (mValuePosition) {
                    case Center:
                        mTextPaint.setTextAlign(Paint.Align.CENTER);
                        mTextX = cX;
                        mTextY = cY - .5f*(mTextPaint.getFontMetrics().descent+mTextPaint.getFontMetrics().ascent);
                        break;
                    case Bottom:
                        mTextPaint.setTextAlign(Paint.Align.CENTER);
                        dH -= mTextHeight;
                        d = Math.min(dW, dH);
                        cY -= 0.5f * mTextHeight;
                        mTextX = cX;
                        mTextY = cY + 0.5f * d - (mTextPaint.getFontMetrics().descent+mTextPaint.getFontMetrics().ascent);
                        break;
                    case Top:
                        mTextPaint.setTextAlign(Paint.Align.CENTER);
                        dH -= mTextHeight;
                        d = Math.min(dW, dH);
                        cY += 0.5f * mTextHeight;
                        mTextX = cX;
                        mTextY = cY - 0.5f * d - mTextPaint.getFontMetrics().descent;
                        //descent is negative
                        break;
                    case Right:
                        mTextPaint.setTextAlign(Paint.Align.LEFT);
                        d = dH;
                        offset = getTextOffset(mTextHeight, 0.5f*d); // TODO, fix positioning of Seekbar!
                        cX -= 0.5f * (mTextWidth + offset - 0.5f * d);
                        mTextX = cX + offset;
                        mTextY = cY - .5f*(mTextPaint.getFontMetrics().descent+mTextPaint.getFontMetrics().ascent);
                        break;
                    case Left:
                        mTextPaint.setTextAlign(Paint.Align.RIGHT);
                        d = dH;
                        offset = getTextOffset(mTextHeight, 0.5f*d);
                        cX += 0.5f * (mTextWidth + offset - 0.5f * d);
                        mTextX = cX - offset;
                        mTextY = cY - .5f*(mTextPaint.getFontMetrics().descent+mTextPaint.getFontMetrics().ascent);
                        break;
                }
            }

            mRadius = 0.5f*d;
            mSeekbarCenter = new PointF(cX, cY);
            recreatePaths();
        }

        public PointF center() { return mSeekbarCenter; }
        public float getRadius() { return mRadius; }
        public float getTextSize() { return mTextPaint.getTextSize(); }
        public RectF getBounds() { return mBounds; }

        public void draw(Canvas canvas) {

            float rot = 0.0f;
            switch(mValuePosition) {
                case Top:
                    rot = 180.0f;
                    break;
                case Right:
                    rot = -90.0f;
                    break;
                case Left:
                    rot = 90.0f;
                    break;
            }
            if(mSectorRotation != 0)
                rot += mSectorRotation;
            canvas.rotate(rot, mSeekbarCenter.x, mSeekbarCenter.y);

            if(mShowSector) {
                canvas.drawPath(mSectorPath, mSectorPaint);
                canvas.drawPath(mValuePath, mValueSectorPaint);
            }

            if(mShowTicks && mNumTicks > 0) {
                float tickAngle = (270- mSectorHalfOpening)*(float)Math.PI/180.0f;
                float tickAngleIncrement = (float)Math.PI/180.0f*(360-2* mSectorHalfOpening)/(mNumTicks-1);
                for(int i=0; i<mNumTicks; i++) {
                    canvas.drawLine(
                            mSeekbarCenter.x+ mRadius *mTickMinRadiusScale*(float)Math.cos(tickAngle-i*tickAngleIncrement),
                            mSeekbarCenter.y- mRadius *mTickMinRadiusScale*(float)Math.sin(tickAngle-i*tickAngleIncrement),
                            mSeekbarCenter.x+ mRadius *mTickMajRadiusScale*(float)Math.cos(tickAngle-i*tickAngleIncrement),
                            mSeekbarCenter.y- mRadius *mTickMajRadiusScale*(float)Math.sin(tickAngle-i*tickAngleIncrement),
                            mTicksPaint
                    );
                }
            }

            final boolean drawKnob = mShowKnob && mKnobRadius > 0.01f;
            if(mNeedleOnTop && drawKnob)
                canvas.drawCircle(mSeekbarCenter.x, mSeekbarCenter.y, mRadius * mKnobRadius, mKnobPaint);

            if(mShowNeedle) {
                final float needleAngle = mRotation * (float) Math.PI / 180.f; // convert to radians
                final float cosNA = (float)Math.cos(needleAngle);
                final float sinNA = (float)Math.sin(needleAngle);
                canvas.drawLine(
                        mSeekbarCenter.x + mRadius * mNeedleMinorRadius * cosNA,
                        mSeekbarCenter.y - mRadius * mNeedleMinorRadius * sinNA,
                        mSeekbarCenter.x + mRadius * mNeedleMajorRadius * cosNA,
                        mSeekbarCenter.y - mRadius * mNeedleMajorRadius * sinNA,
                        mNeedlePaint
                );
            }

            if(!mNeedleOnTop && drawKnob)
                canvas.drawCircle(mSeekbarCenter.x, mSeekbarCenter.y, mRadius * mKnobRadius, mKnobPaint);

            canvas.rotate(-rot, mSeekbarCenter.x, mSeekbarCenter.y);
            if(mShowValue)
                canvas.drawText(mValueStr, mTextX, mTextY, mTextPaint);
        }

        private Path createSectorPath(float sweepAngle) {
            float startAngle = 90+mSectorHalfOpening;

            Path path = new Path();
            if(sweepAngle == 360) {
                path.addOval(circleBounds(mSectorMajRadiusScale), Path.Direction.CCW);
                path.addOval(circleBounds(mSectorMinRadiusScale), Path.Direction.CW);
            }else {
                path.arcTo(circleBounds(mSectorMinRadiusScale), startAngle, sweepAngle);
                path.arcTo(circleBounds(mSectorMajRadiusScale), startAngle + sweepAngle, -sweepAngle);
                path.close();
            }

            if(mSubtractTicks && mNumTicks > 0) {
                Path tickPath = new Path();
                tickPath.addRect(
                        0.0f, -0.5f*mTicksSubtractWidth*mScaling/mRadius,
                        1.0f,  0.5f*mTicksSubtractWidth*mScaling/mRadius,
                        Path.Direction.CCW);
                float tickAngle = (270- mSectorHalfOpening);
                float tickAngleIncrement = (360-2*mSectorHalfOpening)/(mNumTicks-1);
                Matrix tickMatrix = new Matrix();
                tickMatrix.postRotate(-tickAngle);
                Path rotatedTick = new Path();
                for(int i=0; i<mNumTicks; i++) {
                    tickPath.transform(tickMatrix, rotatedTick);
                    path.op(rotatedTick, Path.Op.DIFFERENCE);
                    // TODO: rewrite in terms of regions for lower API versions.
                    tickMatrix.postRotate(tickAngleIncrement);
                }
            }

            Matrix matrix = new Matrix();
            matrix.postScale(mRadius, mRadius);
            matrix.postTranslate(mSeekbarCenter.x, mSeekbarCenter.y);
            path.transform(matrix);

            return path;
        }

        private RectF circleBounds(float radius) {
            return new RectF(-radius,-radius,radius,radius);
        }

        public void recreatePaths() {
            mSectorPath = createSectorPath(360-2* mSectorHalfOpening);
            mValuePath = createSectorPath(rotationToSweep(mRotation));
        }
    }
}

