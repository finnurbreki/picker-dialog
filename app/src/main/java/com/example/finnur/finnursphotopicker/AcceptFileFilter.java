// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileFilter;

public abstract class AcceptFileFilter implements FileFilter {
    private static AcceptFileFilter s_DefaultSingleton = null;

    @NonNull
    public static AcceptFileFilter getDefault() {
        if (s_DefaultSingleton == null) {
            s_DefaultSingleton = new DefaultAcceptFileFilter();
        }
        return s_DefaultSingleton;
    }

    @NonNull
    public static AcceptFileFilter forAttr(@NonNull String acceptAttr) {
        return new AttrAcceptFileFilter(acceptAttr);
    }

    public abstract boolean accept(@NonNull File file);

    public abstract boolean acceptsImages();

    public abstract boolean acceptsVideos();

    public abstract boolean acceptsOther();
}
