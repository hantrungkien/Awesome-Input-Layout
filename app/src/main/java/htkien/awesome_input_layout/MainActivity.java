package htkien.awesome_input_layout;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import htkien.awesome_input.AwesomeInputLayout;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.layout_root)
    RelativeLayout mLayoutRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    public void onBackPressed() {
        if (!mLayoutRoot.isFocused()) {
            mLayoutRoot.requestFocus();
            mLayoutRoot.requestFocusFromTouch();
        } else {
            super.onBackPressed();
        }
    }

    @OnClick(R.id.button_add_text)
    void onClickBtnAddText() {
        final AwesomeInputLayout awesomeLayout = (AwesomeInputLayout) LayoutInflater.from(this).inflate(R.layout.layout_awesome_input, mLayoutRoot, false);

        mLayoutRoot.addView(awesomeLayout);

        awesomeLayout.post(new Runnable() {
            @Override
            public void run() {
                int w = mLayoutRoot.getWidth();
                int h = mLayoutRoot.getHeight();
                awesomeLayout.setX((w / 2) - awesomeLayout.getWidth() / 2);
                awesomeLayout.setY((h / 2) - awesomeLayout.getHeight() / 2);
                awesomeLayout.requestLayout();
                awesomeLayout.setVisibility(View.VISIBLE);
                showKeyboard(MainActivity.this);
            }
        });

        awesomeLayout.setDeleteViewListener(new AwesomeInputLayout.OnDeleteViewListener() {
            @Override
            public void onDeleteView(AwesomeInputLayout awesomeLayout) {
                hideKeyboard(awesomeLayout);
                mLayoutRoot.requestFocus();
                mLayoutRoot.requestFocusFromTouch();
                try {
                    mLayoutRoot.removeView(awesomeLayout);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "onDeleteView: ", e);
                }
            }
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0); // hide
    }

    private void showKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY); // show
    }
}
