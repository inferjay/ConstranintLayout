/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.tnt.solver.widgets;

import java.util.ArrayList;

/**
 * Simple container class to hold a state snapshot of a widget
 */
public class Snapshot {

    private int mX;
    private int mY;
    private int mWidth;
    private int mHeight;

    private ArrayList<Connection> mConnections = new ArrayList<Connection>();

    /**
     * Utility inner class holding widgets' connections
     */
    class Connection {
        private ConstraintAnchor mAnchor;
        private ConstraintAnchor mTarget;
        private int mMargin;
        private ConstraintAnchor.Strength mStrengh;

        /**
         * Base constructor
         *
         * @param anchor the connection we need to save
         */
        public Connection(ConstraintAnchor anchor) {
            mAnchor = anchor;
            mTarget = anchor.getTarget();
            mMargin = anchor.getMargin();
            mStrengh = anchor.getStrength();
        }

        /**
         * Update the connection from the given widget
         *
         * @param widget the widget we update from
         */
        public void updateFrom(ConstraintWidget widget) {
            mAnchor = widget.getAnchor(mAnchor.getType());
            if (mAnchor != null) {
                mTarget = mAnchor.getTarget();
                mMargin = mAnchor.getMargin();
                mStrengh = mAnchor.getStrength();
            } else {
                mTarget = null;
                mMargin = 0;
                mStrengh = ConstraintAnchor.Strength.STRONG;
            }
        }

        /**
         * Apply the saved connection state to the given widget
         *
         * @param widget the widget we apply our snapshot on
         */
        public void applyTo(ConstraintWidget widget) {
            ConstraintAnchor anchor = widget.getAnchor(mAnchor.getType());
            anchor.connect(mTarget, mMargin, mStrengh);
        }
    }

    /**
     * Base constructor
     *
     * @param widget the widget we want to save
     */
    public Snapshot(ConstraintWidget widget) {
        mX = widget.getX();
        mY = widget.getY();
        mWidth = widget.getWidth();
        mHeight = widget.getHeight();
        for (ConstraintAnchor a : widget.getAnchors()) {
            mConnections.add(new Connection(a));
        }
    }

    /**
     * Update this snapshot with the given widget
     *
     * @param widget the widget we want to save
     */
    public void updateFrom(ConstraintWidget widget) {
        mX = widget.getX();
        mY = widget.getY();
        mWidth = widget.getWidth();
        mHeight = widget.getHeight();
        final int connections = mConnections.size();
        for (int i = 0; i < connections; i++) {
            Connection connection = mConnections.get(i);
            connection.updateFrom(widget);
        }
    }

    /**
     * Apply this snapshot to the given widget
     *
     * @param widget the widget we apply our snapshot on
     */
    public void applyTo(ConstraintWidget widget) {
        widget.setX(mX);
        widget.setY(mY);
        widget.setWidth(mWidth);
        widget.setHeight(mHeight);
        for (Connection connection : mConnections) {
            connection.applyTo(widget);
        }
    }
}
