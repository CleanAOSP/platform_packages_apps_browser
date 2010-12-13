/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import com.android.browser.UrlInputView.UrlInputListener;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.webkit.WebView;
import android.widget.ImageView;

/**
 * tabbed title bar for xlarge screen browser
 */
public class TitleBarXLarge extends TitleBarBase
    implements UrlInputListener, OnClickListener, OnFocusChangeListener,
    TextWatcher {

    private static final int PROGRESS_MAX = 100;

    private UiController mUiController;
    private XLargeUi mUi;

    private Drawable mStopDrawable;
    private Drawable mReloadDrawable;

    private View mContainer;
    private View mBackButton;
    private View mForwardButton;
    private ImageView mStar;
    private View mSearchButton;
    private View mUrlContainer;
    private View mGoButton;
    private ImageView mStopButton;
    private View mAllButton;
    private View mClearButton;
    private View mVoiceSearch;
    private PageProgressView mProgressView;
    private UrlInputView mUrlInput;

    private boolean mInLoad;

    public TitleBarXLarge(Activity activity, UiController controller,
            XLargeUi ui) {
        super(activity);
        mUiController = controller;
        mUi = ui;
        Resources resources = activity.getResources();
        mStopDrawable = resources.getDrawable(R.drawable.ic_stop_normal);
        mReloadDrawable = resources.getDrawable(R.drawable.ic_refresh_normal);
        rebuildLayout(activity, true);
    }

    private void rebuildLayout(Context context, boolean rebuildData) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.url_bar, this);

        mContainer = findViewById(R.id.taburlbar);
        mUrlInput = (UrlInputView) findViewById(R.id.url_focused);
        mAllButton = findViewById(R.id.all_btn);
        // TODO: Change enabled states based on whether you can go
        // back/forward.  Probably should be done inside onPageStarted.
        mBackButton = findViewById(R.id.back);
        mForwardButton = findViewById(R.id.forward);
        mStar = (ImageView) findViewById(R.id.star);
        mStopButton = (ImageView) findViewById(R.id.stop);
        mSearchButton = findViewById(R.id.search);
        mLockIcon = (ImageView) findViewById(R.id.lock);
        mGoButton = findViewById(R.id.go);
        mClearButton = findViewById(R.id.clear);
        mVoiceSearch = findViewById(R.id.voicesearch);
        mProgressView = (PageProgressView) findViewById(R.id.progress);
        mUrlContainer = findViewById(R.id.urlbar_focused);

        mBackButton.setOnClickListener(this);
        mForwardButton.setOnClickListener(this);
        mStar.setOnClickListener(this);
        mAllButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mSearchButton.setOnClickListener(this);
        mGoButton.setOnClickListener(this);
        mClearButton.setOnClickListener(this);
        mUrlContainer.setOnClickListener(this);
        mUrlInput.setUrlInputListener(this);
        mUrlInput.setContainer(mUrlContainer);
        mUrlInput.setController(mUiController);
        mUrlInput.setOnFocusChangeListener(this);
        mUrlInput.setSelectAllOnFocus(true);
        mUrlInput.addTextChangedListener(this);
        setUrlMode(false);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        setUrlMode(hasFocus);
    }

    public void setCurrentUrlIsBookmark(boolean isBookmark) {
        mStar.setActivated(isBookmark);
    }

    /**
     * called from the Ui when the user wants to edit
     * Note: only the fake titlebar will get this callback
     * independent of which input field started the edit mode
     * @param clearInput clear the input field
     */
    void onEditUrl(boolean clearInput) {
        mUrlInput.requestFocusFromTouch();
        if (clearInput) {
            mUrlInput.setText("");
        }
    }

    boolean isEditingUrl() {
        return mUrlInput.hasFocus();
    }

    @Override
    public void onClick(View v) {
        if (mUrlInput == v) {
            if (!mUrlInput.hasFocus()) {
                mUi.editUrl(false);
            }
        } else if (mBackButton == v) {
            mUiController.getCurrentTopWebView().goBack();
        } else if (mForwardButton == v) {
            mUiController.getCurrentTopWebView().goForward();
        } else if (mStar == v) {
            mUiController.bookmarkCurrentPage(
                    AddBookmarkPage.DEFAULT_FOLDER_ID);
        } else if (mAllButton == v) {
            mUiController.bookmarksOrHistoryPicker(false);
        } else if (mSearchButton == v) {
            mUi.editUrl(true);
        } else if (mStopButton == v) {
            stopOrRefresh();
        } else if (mGoButton == v) {
            if (!TextUtils.isEmpty(mUrlInput.getText())) {
                onAction(mUrlInput.getText().toString(), null,
                        UrlInputView.TYPED);
            }
        } else if (mClearButton == v) {
            clearOrClose();
        }
    }

    int getHeightWithoutProgress() {
        return mContainer.getHeight();
    }

    @Override
    void setFavicon(Bitmap icon) { }

    private void clearOrClose() {
        if (TextUtils.isEmpty(mUrlInput.getText())) {
            // close
            setUrlMode(false);
        } else {
            // clear
            mUrlInput.setText("");
        }
    }

    // UrlInputListener implementation

    /**
     * callback from suggestion dropdown
     * user selected a suggestion
     */
    @Override
    public void onAction(String text, String extra, String source) {
        mUiController.getCurrentTopWebView().requestFocus();
        mUi.hideFakeTitleBar();
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEARCH);
        i.putExtra(SearchManager.QUERY, text);
        if (extra != null) {
            i.putExtra(SearchManager.EXTRA_DATA_KEY, extra);
        }
        if (source != null) {
            Bundle appData = new Bundle();
            appData.putString(com.android.common.Search.SOURCE, source);
            i.putExtra(SearchManager.APP_DATA, appData);
        }
        mUiController.handleNewIntent(i);
        setUrlMode(false);
        setDisplayTitle(text);
    }

    @Override
    public void onDismiss() {
        WebView top = mUiController.getCurrentTopWebView();
        if (top != null) {
            mUiController.getCurrentTopWebView().requestFocus();
        }
        mUi.hideFakeTitleBar();
        setUrlMode(false);
        // if top != null current must be set
        if (top != null) {
            setDisplayTitle(mUiController.getCurrentWebView().getUrl());
        }
    }

    /**
     * callback from the suggestion dropdown
     * copy text to input field and stay in edit mode
     */
    @Override
    public void onEdit(String text) {
        setDisplayTitle(text, true);
        if (text != null) {
            mUrlInput.setSelection(text.length());
        }
    }

    void setUrlMode(boolean focused) {
        if (focused) {
            mUrlInput.setDropDownWidth(mUrlContainer.getWidth());
            mUrlInput.setDropDownHorizontalOffset(-mUrlInput.getLeft());
            mSearchButton.setVisibility(View.GONE);
            mStar.setVisibility(View.GONE);
            mClearButton.setVisibility(View.VISIBLE);
            updateSearchMode();
        } else {
            mUrlInput.clearFocus();
            mSearchButton.setVisibility(View.VISIBLE);
            mGoButton.setVisibility(View.GONE);
            mVoiceSearch.setVisibility(View.GONE);
            mStar.setVisibility(View.VISIBLE);
            mClearButton.setVisibility(View.GONE);
        }
    }

    private void stopOrRefresh() {
        if (mInLoad) {
            mUiController.stopLoading();
        } else {
            mUiController.getCurrentTopWebView().reload();
        }
    }

    /**
     * Update the progress, from 0 to 100.
     */
    @Override
    void setProgress(int newProgress) {
        if (newProgress >= PROGRESS_MAX) {
            mProgressView.setProgress(PageProgressView.MAX_PROGRESS);
            mProgressView.setVisibility(View.GONE);
            mInLoad = false;
            mStopButton.setImageDrawable(mReloadDrawable);
        } else {
            if (!mInLoad) {
                mProgressView.setVisibility(View.VISIBLE);
                mInLoad = true;
                mStopButton.setImageDrawable(mStopDrawable);
            }
            mProgressView.setProgress(newProgress * PageProgressView.MAX_PROGRESS
                    / PROGRESS_MAX);
        }
    }

    private void updateSearchMode() {
        setSearchMode(TextUtils.isEmpty(mUrlInput.getText()));
    }

    private void setSearchMode(boolean voiceSearchEnabled) {
        mVoiceSearch.setVisibility(voiceSearchEnabled ? View.VISIBLE :
                View.GONE);
        mGoButton.setVisibility(voiceSearchEnabled ? View.GONE :
                View.VISIBLE);
    }

    @Override
    /* package */ void setDisplayTitle(String title) {
        mUrlInput.setText(title, false);
    }

    void setDisplayTitle(String title, boolean filter) {
        mUrlInput.setText(title, filter);
    }

    // UrlInput text watcher

    @Override
    public void afterTextChanged(Editable s) {
        if (mUrlInput.hasFocus()) {
            // check if input field is empty and adjust voice search state
            updateSearchMode();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

}
