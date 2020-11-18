// Copyright 2020 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import org.chromium.chrome.browser.base.SplitCompatService;
// import org.chromium.chrome.browser.base.SplitCompatUtils;

/** See {@link DecoderServiceImpl}. */
public class DecoderService extends SplitCompatService {
    public DecoderService() {
        super("com.example.finnur.finnursphotopicker.DecoderServiceImpl");
        /* Not needed for Android Studio project.
        super(SplitCompatUtils.getIdentifierName(
                "org.chromium.chrome.browser.photo_picker.DecoderServiceImpl"));
        */
    }
}
