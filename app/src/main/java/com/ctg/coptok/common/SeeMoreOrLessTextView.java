package com.ctg.coptok.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import com.vanniktech.emoji.EmojiTextView;

import com.ctg.coptok.R;

public class SeeMoreOrLessTextView extends EmojiTextView {

    private static final String ELLIPSIZE = "...";
    private static final int MAX_LENGTH = 200;
    private static final int MAX_LINES = 3;

    private BufferType mBufferType;
    private final ClickableSpan mClickableSpan = new ClickableSpan() {

        @Override
        public void onClick(View widget) {
            mStateReadMore = !mStateReadMore;
            setText();
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(ds.linkColor);
        }
    };
    private int mEndIndex;
    private CharSequence mSeeLessText;
    private CharSequence mSeeMoreText;
    private boolean mStateReadMore = true;
    private CharSequence mText;

    public SeeMoreOrLessTextView(Context context) {
        super(context);
    }

    public SeeMoreOrLessTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSeeMoreText = getResources().getString(R.string.see_more_label);
        mSeeLessText = getResources().getString(R.string.see_less_label);
        onGlobalLayoutLineEndIndex();
        setText();
    }

    private void setText() {
        super.setText(getDisplayableText(), mBufferType);
        setMovementMethod(LinkMovementMethod.getInstance());
        setHighlightColor(Color.TRANSPARENT);
    }

    private CharSequence getDisplayableText() {
        return getTrimmedText(mText);
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void setText(CharSequence text, BufferType type) {
        mText = text;
        mBufferType = type;
        setText();
    }

    private CharSequence getTrimmedText(CharSequence text) {
        if (text != null && mEndIndex > 0) {
            if (mStateReadMore) {
                if (getLayout().getLineCount() > MAX_LINES) {
                    return updateCollapsedText();
                }
            } else {
                return updateExpandedText();
            }
        }

        return text;
    }

    private CharSequence updateCollapsedText() {
        int i = mEndIndex - (ELLIPSIZE.length() + mSeeMoreText.length() + 1);
        if (i < 0) {
            i = MAX_LENGTH + 1;
        }

        SpannableStringBuilder s = new SpannableStringBuilder(mText, 0, i)
                .append(' ')
                .append(ELLIPSIZE)
                .append(mSeeMoreText);
        return addClickableSpan(s, mSeeMoreText);
    }

    private CharSequence updateExpandedText() {
        SpannableStringBuilder s = new SpannableStringBuilder(mText, 0, mText.length())
                .append(' ')
                .append(mSeeLessText);
        return addClickableSpan(s, mSeeLessText);
    }

    private CharSequence addClickableSpan(SpannableStringBuilder s, CharSequence trimText) {
        s.setSpan(mClickableSpan, s.length() - trimText.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private void onGlobalLayoutLineEndIndex() {
        getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        ViewTreeObserver obs = getViewTreeObserver();
                        obs.removeOnGlobalLayoutListener(this);
                        refreshEndIndex();
                        setText();
                    }
                });
    }

    private void refreshEndIndex() {
        try {
            if (MAX_LINES == 0) {
                mEndIndex = getLayout().getLineEnd(0);
            } else if (MAX_LINES > 0 && getLineCount() >= MAX_LINES) {
                mEndIndex = getLayout().getLineEnd(MAX_LINES - 1);
            } else {
                mEndIndex = -1;
            }
        } catch (Exception ignore) {
        }
    }
}
