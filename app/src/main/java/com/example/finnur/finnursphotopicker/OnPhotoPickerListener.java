// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.ui;
package com.example.finnur.finnursphotopicker;

/**
 * The callback used to indicate what action the user took in the picker.
 */
public interface OnPhotoPickerListener {
    /**
     *  The action the user took in the picker.
     */
    enum Action {
        PHOTOS_SELECTED,
        LAUNCH_CAMERA,
        LAUNCH_GALLERY,
    }

    /**
     * The types of requests supported.
     */
    static final int SHOW_GALLERY = 1;

    /**
     * Called when the user has selected an action. For possible actions see
     * above.
     *
     * @param photos The photos that were selected.
     */
    void onPickerUserAction(Action action, String[] photos);
}
