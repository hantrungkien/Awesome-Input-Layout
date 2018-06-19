package htkien.awesome_input;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import htkien.awesome_input.gesturedetectors.RotateGestureDetector;

/**
 * Note:
 * Created by kienht on 6/12/18.
 */
public class AwesomeInputLayout extends RelativeLayout {

    public static final String TAG = AwesomeInputLayout.class.getSimpleName();

    private ScaleGestureDetector scaleDetector;
    private RotateGestureDetector rotateDetector;
    private OnDeleteViewListener deleteViewListener;

    private ImageButton mButtonDelete;
    private EditText mEditText;

    private boolean isHorizontal = true;
    private boolean isMoveMode = true;
    private boolean isMoving = false;

    private float startX;
    private float startY;
    private float deltaX;
    private float deltaY;

    private int startWidth;
    private int editStartWidth;
    private float scaleFactor = 1.f;

    private float minTextSize;

    private Drawable mDrawableWhiteTroke;

    private class RotateListener extends RotateGestureDetector.SimpleOnRotateGestureListener {
        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            if (!isMoveMode) {
                setRotation(getRotation() - detector.getRotationDegreesDelta());
            }
            return false;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            scaleFactor *= detector.getScaleFactor();

            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f));
            int editTextMinWidth = getTextBoundWidth(minTextSize);

            if (!isMoveMode && !TextUtils.isEmpty(mEditText.getText().toString())) {
                float x = getTranslationX();
                float y = getTranslationY();

                int newWidth = (int) (startWidth * scaleFactor);
                int newEditWidth = (int) (editStartWidth * scaleFactor);

                if (newEditWidth > editTextMinWidth &&
                        newWidth > getMinimumWidth() && newWidth < ((View) getParent()).getWidth()) {

                    ViewGroup.LayoutParams rootParams = getLayoutParams();
                    rootParams.width = newWidth;
                    rootParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    setLayoutParams(rootParams);

                    ViewGroup.LayoutParams editTextParams = mEditText.getLayoutParams();
                    editTextParams.width = newEditWidth;
                    editTextParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    mEditText.setLayoutParams(editTextParams);

                    correctTextsize(mEditText, newEditWidth);
                }
                setTranslationX(x);
                setTranslationY(y);

                //invalidate();

                resetWrapContentForView();
            }

            return true;
        }
    }

    public void correctTextsize(TextView textView, int desiredWidth) {
        Paint paint = new Paint();
        Rect bounds = new Rect();

        paint.setTypeface(textView.getTypeface());
        float textSize = textView.getTextSize();
        paint.setTextSize(textSize);

        String text = textView.getText().toString();
        paint.getTextBounds(text, 0, text.length(), bounds);

        if (bounds.width() > desiredWidth) {
            while (bounds.width() >= desiredWidth && textSize >= minTextSize) {
                textSize--;
                paint.setTextSize(textSize);
                paint.getTextBounds(text, 0, text.length(), bounds);
            }
        } else {
            while (bounds.width() < desiredWidth) {
                textSize++;
                paint.setTextSize(textSize);
                paint.getTextBounds(text, 0, text.length(), bounds);
            }
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    public AwesomeInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mDrawableWhiteTroke = context.getResources().getDrawable(R.drawable.rounded_edittext_white_troke);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        rotateDetector = new RotateGestureDetector(context, new RotateListener());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        for (int i = 0; i < getChildCount(); i++) {
            mButtonDelete = (ImageButton) getChildAt(0);
            mEditText = (EditText) getChildAt(1);
        }

        if (mButtonDelete != null) {
            mButtonDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteViewListener.onDeleteView(AwesomeInputLayout.this);
                }
            });
        }

        minTextSize = mEditText.getTextSize();

        mEditText.requestFocus();
        mEditText.requestFocusFromTouch();

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                resetWrapContentForView();
            }
        });

        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    setBackground(mDrawableWhiteTroke);
                    mButtonDelete.setVisibility(VISIBLE);
                } else {
                    mEditText.setFocusable(false);
                    mEditText.setFocusableInTouchMode(false);
                    if (isEmptyInput()) {
                        deleteViewListener.onDeleteView(AwesomeInputLayout.this);
                    } else {
                        setBackground(null);
                        mButtonDelete.setVisibility(GONE);
                    }
                }
            }
        });

        mEditText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mEditText.setFocusable(true);
                mEditText.setFocusableInTouchMode(true);
                mEditText.requestFocus();
                mEditText.requestFocusFromTouch();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        rotateDetector.onTouchEvent(event);

        final int pointX = (int) event.getRawX();
        final int pointY = (int) event.getRawY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = getTranslationX();
                startY = getTranslationY();
                deltaX = pointX - getTranslationX();
                deltaY = pointY - getTranslationY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (isMoveMode) {
                    startWidth = getWidth();
                    editStartWidth = getTextBoundWidth(mEditText.getTextSize());

                    isMoveMode = false;
                    isMoving = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float translationX = pointX - deltaX;
                float translationY = pointY - deltaY;
                if (isMoveMode && mEditText.isFocusableInTouchMode()) {
                    if (isInBounds(translationX, translationY)) {
                        setTranslationX(translationX);
                        setTranslationY(translationY);
                    } else if (canMoveVertically(translationY)) {
                        setTranslationY(translationY);
                    } else if (canMoveHorizontally(translationX)) {
                        setTranslationX(translationX);
                    }

                    if (!isMoving) {
                        isMoving = true;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                scaleFactor = 1.0f;
                break;
            case MotionEvent.ACTION_UP:
                boolean isEditTextMoved = Math.abs(startX - getTranslationX()) > 25 || Math.abs(startY - getTranslationY()) > 25;
                setClickable(!isEditTextMoved);
                setFocusable(!isEditTextMoved);
                setFocusableInTouchMode(!isEditTextMoved);

                isMoving = false;
                isMoveMode = true;
                break;
        }

        return true;
    }

    private void resetWrapContentForView() {
        ViewGroup.LayoutParams rootParams = getLayoutParams();
        rootParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        rootParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        setLayoutParams(rootParams);

        ViewGroup.LayoutParams editTextParams = mEditText.getLayoutParams();
        editTextParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        editTextParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mEditText.setLayoutParams(editTextParams);

        invalidate();
    }

    private int getTextBoundWidth(float textSize) {
        String text = mEditText.getText().toString();
        Rect textBounds = new Rect();
        Paint textPaint = new Paint();
        textPaint.setTypeface(mEditText.getTypeface());
        textPaint.setTextSize(textSize);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        return textBounds.width();
    }

    private boolean isInBounds(float translationX, float translationY) {
        if (getParent() == null) {
            return true;
        }

        View parent = (View) getParent();

        float degreeRemain = getRotation() % 360;
        isHorizontal = !((degreeRemain > 45 && degreeRemain < 135)
                || (degreeRemain > 225 && degreeRemain < 315)
                || (degreeRemain > -135 && degreeRemain < -45)
                || (degreeRemain > -315 && degreeRemain < -225));

        if (isHorizontal) {
            return translationX + getWidth() < parent.getWidth()
                    && translationX > 0
                    && translationY + getHeight() < parent.getHeight()
                    && translationY > 0;
        } else {
            float correction = getWidth() / 2 - getHeight() / 2;
            return translationX + correction + getHeight() < parent.getWidth()
                    && translationX + correction > 0
                    && translationY - correction + getWidth() < parent.getHeight()
                    && translationY - correction > 0;
        }
    }

    private boolean canMoveVertically(float translationY) {
        if (getParent() == null) {
            return true;
        }
        View parent = (View) getParent();

        if (isHorizontal) {
            return translationY + getHeight() < parent.getHeight()
                    && translationY > 0;
        } else {
            float correction = getWidth() / 2 - getHeight() / 2;
            return translationY - correction + getWidth() < parent.getHeight()
                    && translationY - correction > 0;
        }
    }

    private boolean canMoveHorizontally(float translationX) {
        if (getParent() == null) {
            return true;
        }
        View parent = (View) getParent();

        if (isHorizontal) {
            return translationX + getWidth() < parent.getWidth()
                    && translationX > 0;
        } else {
            float correction = getWidth() / 2 - getHeight() / 2;
            return translationX + correction + getHeight() < parent.getWidth()
                    && translationX + correction > 0;
        }
    }

    private boolean isEmptyInput() {
        String text = mEditText.getText().toString();
        return TextUtils.isEmpty(text);
    }

    public void setDeleteViewListener(OnDeleteViewListener deleteViewListener) {
        this.deleteViewListener = deleteViewListener;
    }

    public interface OnDeleteViewListener {
        void onDeleteView(AwesomeInputLayout awesomeInputLayout);
    }
}
