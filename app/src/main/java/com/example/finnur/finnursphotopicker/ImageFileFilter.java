// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Locale;

/**
 * A file filter for handling extensions and MIME types (images/jpeg) and
 * supertypes (images/*).
 */
class ImageFileFilter implements FileFilter {
    private static final String IMAGE_SUPERTYPE = "image";
    private static final String VIDEO_SUPERTYPE = "video";

    private HashSet<String> mExtensions = new HashSet<>();
    private HashSet<String> mMimeTypes = new HashSet<>();
    private HashSet<String> mMimeSupertypes = new HashSet<>();
    private MimeTypeMap mMimeTypeMap;

    public ImageFileFilter(@NonNull String acceptAttr) {
        for (String field : acceptAttr.toLowerCase(Locale.US).split(",")) {
            field = field.trim();
            if (field.startsWith(".")) {
                mExtensions.add(field.substring(1));
            } else if (field.endsWith("/*")) {
                mMimeSupertypes.add(field.substring(0, field.length() - 2));
            } else if (field.contains("/")) {
                mMimeTypes.add(field);
            } else {
                // Throw exception?
            }
        }

        mMimeTypeMap = MimeTypeMap.getSingleton();
    }

    @Override
    public boolean accept(@NonNull File file) {
        if (file.isDirectory()) {
            return true;
        }

        String uri = file.toURI().toString();
        String ext = MimeTypeMap.getFileExtensionFromUrl(uri).toLowerCase(Locale.US);
        if (mExtensions.contains(ext)) {
            return true;
        }

        String mimeType = getMimeTypeFromExtension(ext);
        if (mimeType != null) {
            if (mMimeTypes.contains(mimeType)) {
                return true;
            }
            if (mMimeSupertypes.contains(getMimeSupertype(mimeType))) {
                return true;
            }
        }

        return false;
    }

    public boolean acceptsImages() {
        return getAcceptedSupertypes().contains(IMAGE_SUPERTYPE);
    }

    public boolean acceptsVideos() {
        return getAcceptedSupertypes().contains(VIDEO_SUPERTYPE);
    }

    public boolean acceptsOther() {
        HashSet<String> supertypes = getAcceptedSupertypes();
        supertypes.remove(IMAGE_SUPERTYPE);
        supertypes.remove(VIDEO_SUPERTYPE);
        return !supertypes.isEmpty();
    }

    private HashSet<String> getAcceptedSupertypes() {
        HashSet<String> supertypes = new HashSet<>();
        supertypes.addAll(mMimeSupertypes);
        for (String mimeType : mMimeTypes) {
            supertypes.add(getMimeSupertype(mimeType));
        }
        for (String ext : mExtensions) {
            String mimeType = getMimeTypeFromExtension(ext);
            if (mimeType != null) {
                supertypes.add(getMimeSupertype(mimeType));
            }
        }
        return supertypes;
    }

    private String getMimeTypeFromExtension(@NonNull String ext) {
        String mimeType = mMimeTypeMap.getMimeTypeFromExtension(ext);
        return (mimeType != null) ? mimeType.toLowerCase(Locale.US) : null;
    }

    @NonNull
    private String getMimeSupertype(@NonNull String mimeType) {
        return mimeType.split("/", 2)[0];
    }
}
