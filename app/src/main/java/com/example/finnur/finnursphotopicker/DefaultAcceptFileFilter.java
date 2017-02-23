// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.support.annotation.NonNull;

import java.io.File;

class DefaultAcceptFileFilter extends AcceptFileFilter {

    @Override
    public boolean accept(@NonNull File file) {
        return true;
    }

    @Override
    public boolean acceptsImages() {
        return true;
    }

    @Override
    public boolean acceptsVideos() {
        return true;
    }

    @Override
    public boolean acceptsOther() {
        return true;
    }
}
