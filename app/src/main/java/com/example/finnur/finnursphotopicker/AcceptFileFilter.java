// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileFilter;

public abstract class AcceptFileFilter implements FileFilter {
    private static AcceptFileFilter sDefaultSingleton = null;

    @NonNull
    public static AcceptFileFilter getDefault() {
        if (sDefaultSingleton == null) {
            sDefaultSingleton = new DefaultAcceptFileFilter();
        }
        return sDefaultSingleton;
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
