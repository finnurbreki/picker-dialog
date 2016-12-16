// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

//package org.chromium.chrome.browser.widget.selection;
package com.example.finnur.finnursphotopicker;

// FLIP
//import org.chromium.base.ObserverList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A generic delegate used to keep track of multiple selected items.
 * @param <E> The type of the selectable items this delegate interacts with.
 */
public class SelectionDelegateMulti<E> implements SelectionDelegate<E> {

    private Set<E> mSelectedItems = new HashSet<>();
    private ObserverList<SelectionObserver<E>> mObservers = new ObserverList<>();

    @Override
    public boolean toggleSelectionForItem(E item) {
        if (mSelectedItems.contains(item)) mSelectedItems.remove(item);
        else mSelectedItems.add(item);

        notifyObservers();

        return isItemSelected(item);
    }

    @Override
    public boolean isItemSelected(E item) {
        return mSelectedItems.contains(item);
    }

    @Override
    public boolean isSelectionEnabled() {
        return !mSelectedItems.isEmpty();
    }

    @Override
    public void clearSelection() {
        mSelectedItems.clear();
        notifyObservers();
    }

    @Override
    public List<E> getSelectedItems() {
        return new ArrayList<E>(mSelectedItems);
    }

    @Override
    public void addObserver(SelectionDelegate.SelectionObserver<E> observer) {
        mObservers.addObserver(observer);
    }

    @Override
    public void removeObserver(SelectionObserver<E> observer) {
        mObservers.removeObserver(observer);
    }

    private void notifyObservers() {
        List<E> selectedItems = getSelectedItems();
        for (SelectionObserver<E> observer : mObservers) {
            observer.onSelectionStateChange(selectedItems);
        }
    }
}