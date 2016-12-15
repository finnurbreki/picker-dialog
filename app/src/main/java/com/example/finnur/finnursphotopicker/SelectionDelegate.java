// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser.widget.selection;
package com.example.finnur.finnursphotopicker;

import java.util.List;

public interface SelectionDelegate<E> {
    /**
     * Observer interface to be notified of selection changes.
     */
    public interface SelectionObserver<E> {
        /**
         * Called when the set of selected items has changed.
         * @param selectedItems The list of currently selected items. An empty list indicates there
         *                      is no selection.
         */
        public void onSelectionStateChange(List<E> selectedItems);
    }

    /**
     * Toggles the selected state for the given item.
     * @param item The item to toggle.
     * @return Whether the item is selected.
     */
    public boolean toggleSelectionForItem(E item);

    /**
     * True if the item is selected. False otherwise.
     * @param item The item.
     * @return Whether the item is selected.
     */
    public boolean isItemSelected(E item);

    /**
     * @return Whether any items are selected.
     */
    public boolean isSelectionEnabled();

    /**
     * Clears all selected items.
     */
    public void clearSelection();

    /**
     * @return The list of selected items.
     */
    public List<E> getSelectedItems();

    /**
     * Adds a SelectionObserver to be notified of selection changes.
     * @param observer The SelectionObserver to add.
     */
    public void addObserver(SelectionObserver<E> observer);

    /**
     * Removes a SelectionObserver.
     * @param observer The SelectionObserver to remove.
     */
    public void removeObserver(SelectionObserver<E> observer);
}
