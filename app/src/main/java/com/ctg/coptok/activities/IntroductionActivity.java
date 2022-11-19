package com.ctg.coptok.activities;

import android.content.Context;
import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.ctg.coptok.R;
import com.ctg.coptok.utils.LocaleUtil;

public class IntroductionActivity extends IntroActivity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide_1_title)
                .description(R.string.slide_1_description)
                .image(R.drawable.intro_1_image)
                .background(R.color.colorIntro1Background)
                .backgroundDark(R.color.colorIntro1Buttons)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide_2_title)
                .description(R.string.slide_2_description)
                .image(R.drawable.intro_2_image)
                .background(R.color.colorIntro2Background)
                .backgroundDark(R.color.colorIntro2Buttons)
                .build());
    }
}
