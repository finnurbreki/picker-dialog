// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

// Chrome-specific imports:
// import org.chromium.base.ObserverList;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic delegate used to keep track of a single selected item.
 * @param <E> The type of the selectable items this delegate interacts with.
 */
public class SelectionDelegateSingle<E> implements SelectionDelegate<E> {

    private ObserverList<SelectionObserver<E>> mObservers = new ObserverList<>();
    private E mSelectedItem;

    @Override
    public boolean toggleSelectionForItem(E item) {
        if (item == mSelectedItem) mSelectedItem = null;
        else mSelectedItem = item;

        notifyObservers();

        return isItemSelected(item);
    }

    @Override
    public boolean isItemSelected(E item) {
        return mSelectedItem == item;
    }

    @Override
    public boolean isSelectionEnabled() {
        return mSelectedItem != null;
    }

    @Override
    public void clearSelection() {
        mSelectedItem = null;
        notifyObservers();
    }

    @Override
    public List<E> getSelectedItems() {
        List<E> list = new ArrayList<E>();
        if (mSelectedItem != null) {
            list.add(mSelectedItem);
        }

        return list;
    }

    @Override
    public void addObserver(SelectionObserver<E> observer) {
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
