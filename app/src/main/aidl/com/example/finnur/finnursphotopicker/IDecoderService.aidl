// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.os.Bundle;

import com.example.finnur.finnursphotopicker.IDecoderServiceListener;

interface IDecoderService {
  oneway void decodeImage(in Bundle payload, IDecoderServiceListener listener);
}
