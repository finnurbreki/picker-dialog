// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

/**
 * The callback used to indicate the user selected one or more photos.
 */
public interface OnPhotoChangedListener {

    /**
     * Called upon when photos have been selected.
     *
     * @param photos The photos that were selected.
     */
    void onPhotoChanged(String[] photos);
}