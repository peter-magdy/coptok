package com.ctg.coptok.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.danielstone.materialaboutlibrary.MaterialAboutFragment;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.Arrays;

import com.ctg.coptok.BuildConfig;
import com.ctg.coptok.MainApplication;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.activities.MainActivity;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.utils.LocaleUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AboutChildFragment extends MaterialAboutFragment {

    private static final String TAG = "AboutChildFragment";

    private String mLocale;
    private MainActivity.MainActivityViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Override
    protected MaterialAboutList getMaterialAboutList(Context context) {
        MaterialAboutList.Builder builder = new MaterialAboutList.Builder()
                .addCard(createInfoCard())
                .addCard(createSettingsCard());
        //           .addCard(createSupportCard())
        //         .addCard(createLegalCard())
        if (mModel.isLoggedIn()) {
            builder.addCard(new MaterialAboutCard.Builder()
                    .addItem(new MaterialAboutActionItem.Builder()
                            .text(R.string.logout_label)
                            .setOnClickAction(() -> ((MainActivity) requireActivity()).logout())
                            .build())
                    .addItem(new MaterialAboutActionItem.Builder()
                            .text(R.string.disable_account_label)
                            .setOnClickAction(this::confirmDisable)
                            .build())
                    .build());
        }

        return builder.build();
    }

    private void confirmDisable() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_disable_account)
                .setNegativeButton(R.string.disable_button, (dialog, i) -> {
                    dialog.dismiss();
                    disableAccount();
                })
                .setPositiveButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .show();
    }

    private MaterialAboutCard createInfoCard() {
        return new MaterialAboutCard.Builder()
                .addItem(new MaterialAboutTitleItem.Builder()
                        .text(R.string.app_name)
                        .icon(R.mipmap.ic_launcher)
                        .build())
                .addItem(new MaterialAboutActionItem.Builder()
                        .text(R.string.version_label)
                        .subText(BuildConfig.VERSION_NAME)
                        .icon(R.drawable.ic_about_version)
                        .build())
                .build();
    }

    private MaterialAboutCard createLegalCard() {
        return new MaterialAboutCard.Builder()
              //  .title(R.string.legal_label)
               // .addItem(new MaterialAboutActionItem.Builder()
            //            .icon(R.drawable.ic_baseline_link_24)
              //          .text(R.string.privacy_policy)
                //        .subText(getString(R.string.link_privacy_policy))
                  //      .setOnClickAction(() -> {
                    //        Intent intent = new Intent(Intent.ACTION_VIEW);
                      //      intent.setData(Uri.parse(getString(R.string.link_privacy_policy)));
                        //    startActivity(intent);
                     //   })
                      //  .build())
                .addItem(new MaterialAboutActionItem.Builder()
                        .icon(R.drawable.ic_baseline_link_24)
                        .text(R.string.term_of_use)
                        .subText(getString(R.string.link_terms_of_use))
                        .setOnClickAction(() -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(getString(R.string.link_terms_of_use)));
                            startActivity(intent);
                        })
                        .build())
                .build();
    }

    private MaterialAboutCard createSettingsCard() {
        return new MaterialAboutCard.Builder()
                .title(R.string.settings_label)
                .addItem(new MaterialAboutActionItem.Builder()
                        .icon(R.drawable.ic_baseline_translate_24)
                        .text(R.string.locale_label)
                        .setOnClickAction(this::showLocaleChooser)
                        .build())
                //   .addItem(new MaterialAboutActionItem.Builder()
                //         .icon(R.drawable.ic_baseline_language_24)
                //         .text(R.string.language_label)
                //         .setOnClickAction(() ->
                //               startActivity(new Intent(requireContext(), LanguageActivity.class)))
                //         .build())
                .build();
    }

    private MaterialAboutCard createSupportCard() {
        MaterialAboutCard.Builder builder = new MaterialAboutCard.Builder()
                .title(R.string.support_label);
        String faq = getString(R.string.support_faq);
        if (!TextUtils.isEmpty(faq)) {
            builder.addItem(new MaterialAboutActionItem.Builder()
                    .icon(R.drawable.ic_about_faq)
                    .text(R.string.faq_label)
                    .subText(faq)
                    .setOnClickAction(() -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(faq));
                        startActivity(intent);
                    })
                    .build());
        }

        String website = getString(R.string.support_website);
        if (!TextUtils.isEmpty(website)) {
            builder.addItem(new MaterialAboutActionItem.Builder()
                    .icon(R.drawable.ic_about_website)
                    .text(R.string.website_label)
                    .subText(website)
                    .setOnClickAction(() -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(website));
                        startActivity(intent);
                    })
                    .build());
        }

        String email = getString(R.string.support_email);
        if (!TextUtils.isEmpty(email)) {
            builder.addItem(new MaterialAboutActionItem.Builder()
                    .icon(R.drawable.ic_about_email)
                    .text(R.string.email_label)
                    .subText(email)
                    .setOnClickAction(() -> {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:" + email));
                        startActivity(intent);
                    })
                    .build());
        }

        String phone = getString(R.string.support_phone);
        if (!TextUtils.isEmpty(phone)) {
            builder.addItem(new MaterialAboutActionItem.Builder()
                    .icon(R.drawable.ic_about_phone)
                    .text(R.string.phone_label)
                    .subText(phone)
                    .setOnClickAction(() -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    })
                    .build());
        }

        return builder.build();
    }

    private void disableAccount() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.profileDelete()
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        progress.dismiss();
                        if (response != null && response.isSuccessful()) {
                            Toast.makeText(requireContext(), R.string.message_account_disabled, Toast.LENGTH_LONG).show();
                            ((MainActivity) requireActivity()).logout();
                        } else {
                            Toast.makeText(requireContext(), R.string.error_server, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to disabled account.", t);
                        progress.dismiss();
                        Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static AboutChildFragment newInstance() {
        return new AboutChildFragment();
    }

    private void showLocaleChooser() {
        String locale = Prefs.getString(SharedConstants.PREF_APP_LOCALE, "en");
        String[] codes = getResources().getStringArray(R.array.locale_codes);
        int current = Arrays.asList(codes).indexOf(locale);
        if (current < 0) {
            current = 0;
        }

        mLocale = null;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.locale_label)
                .setSingleChoiceItems(
                        R.array.locale_names,
                        current,
                        (dialog, which) -> mLocale = codes[which])
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.apply_button, ((dialog, which) -> {
                    dialog.dismiss();
                    if (!TextUtils.equals(mLocale, locale)) {
                        Prefs.putString(SharedConstants.PREF_APP_LOCALE, mLocale);
                        LocaleUtil.override(requireContext());
                        Uri base = Uri.parse(getString(R.string.server_url));
                        Uri uri = base.buildUpon().path("links/about").build();
                        ((MainActivity) requireActivity()).restartActivity(null, uri);
                    }
                }))
                .show();
    }
}
