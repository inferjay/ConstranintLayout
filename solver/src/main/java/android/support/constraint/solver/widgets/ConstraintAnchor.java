/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.constraint.solver.widgets;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Model a constraint relation. Widgets contains anchors, and a constraint relation between
 * two widgets is made by connecting one anchor to another. The anchor will contains a pointer
 * to the target anchor if it is connected.
 */
public class ConstraintAnchor {

    public static final boolean USE_CENTER_ANCHOR = false;

    /**
     * Define the type of anchor
     */
    public enum Type { NONE, LEFT, TOP, RIGHT, BOTTOM, BASELINE, CENTER, CENTER_X, CENTER_Y }

    /**
     * Define the strength of an anchor connection
     */
    public enum Strength { NONE, STRONG, WEAK }

    /**
     * Define the type of connection - either relaxed (allow +/- errors) or strict (only allow positive errors)
     */
    public enum ConnectionType { RELAXED, STRICT }

    /**
     * Type of creator
     */
    public static final int USER_CREATOR = 0;
    public static final int SCOUT_CREATOR = 1;
    public static final int AUTO_CONSTRAINT_CREATOR = 2;

    private final ConstraintWidget mOwner;
    private final Type mType;
    private ConstraintAnchor mTarget;
    private int mMargin;
    private Strength mStrength = Strength.NONE;
    private ConnectionType mConnectionType = ConnectionType.RELAXED;
    private int mConnectionCreator = USER_CREATOR;

    /**
     * Constructor
     * @param owner the widget owner of this anchor.
     * @param type the anchor type.
     */
    public ConstraintAnchor(ConstraintWidget owner, Type type) {
        mOwner = owner;
        mType = type;
    }

    /**
     * Return the anchor's owner
     * @return the Widget owning the anchor
     */
    public ConstraintWidget getOwner() { return mOwner; }

    /**
     * Return the type of the anchor
     * @return type of the anchor.
     */
    public Type getType() { return mType; }

    /**
     * Return the connection's margin from this anchor to its target.
     * @return the margin value. 0 if not connected.
     */
    public int getMargin() {
        if (mOwner.getVisibility() == ConstraintWidget.GONE) {
            return 0;
        }
        return mMargin;
    }

    /**
     * Return the connection's strength (NONE if not connected)
     */
    public Strength getStrength() { return mStrength; }

    /**
     * Return the connection's target (null if not connected)
     * @return the ConstraintAnchor target
     */
    public ConstraintAnchor getTarget() { return mTarget; }

    /**
     * Return the type of connection
     * @return type connection type (relaxed or strict)
     */
    public ConnectionType getConnectionType() { return mConnectionType; }

    /**
     * Set the type of connection, either relaxed or strict
     * @param type
     */
    public void setConnectionType(ConnectionType type ) {
        mConnectionType = type;
    }

    /**
     * Return the creator of this connection
     */
    public int getConnectionCreator() { return mConnectionCreator; }

    /**
     * Set the creator for this connection
     * @param creator For now, values can be USER_CREATOR or SCOUT_CREATOR
     */
    public void setConnectionCreator(int creator) { mConnectionCreator = creator; }

    /**
     * Resets the anchor's connection.
     */
    public void reset() {
        mTarget = null;
        mMargin = 0;
        mStrength = Strength.NONE;
        mConnectionCreator = USER_CREATOR;
        mConnectionType = ConnectionType.RELAXED;
    }

    /**
     * Connects this anchor to another one.
     * @param toAnchor
     * @param margin
     * @param strength
     * @param creator
     * @return true if the connection succeeds.
     */
    public boolean connect(ConstraintAnchor toAnchor, int margin, Strength strength,
            int creator) {
        return connect(toAnchor, margin, strength, creator, false);
    }

    /**
     * Connects this anchor to another one.
     * @param toAnchor
     * @param margin
     * @param strength
     * @param creator
     * @param forceConnection
     * @return true if the connection succeeds.
     */
    public boolean connect(ConstraintAnchor toAnchor, int margin, Strength strength,
            int creator, boolean forceConnection) {
        if (toAnchor == null) {
            mTarget = null;
            mMargin = 0;
            mStrength = Strength.NONE;
            mConnectionCreator = AUTO_CONSTRAINT_CREATOR;
            return true;
        }
        if (!forceConnection && !isValidConnection(toAnchor)) {
            return false;
        }
        mTarget = toAnchor;
        if (margin > 0) {
            mMargin = margin;
        } else {
            mMargin = 0;
        }
        mStrength = strength;
        mConnectionCreator = creator;
        return true;
    }

    /**
     * Connects this anchor to another one.
     * @param toAnchor
     * @param margin
     * @param creator
     * @return true if the connection succeeds.
     */
    public boolean connect(ConstraintAnchor toAnchor, int margin, int creator) {
        return connect(toAnchor, margin, Strength.STRONG, creator, false);
    }

    /**
     * Connects this anchor to another one.
     * @param toAnchor
     * @param margin
     * @return true if the connection succeeds.
     */
    public boolean connect(ConstraintAnchor toAnchor, int margin) {
        return connect(toAnchor, margin, Strength.STRONG, USER_CREATOR, false);
    }

    /**
     * Returns the connection status of this anchor
     * @return true if the anchor is connected to another one.
     */
    public boolean isConnected() {
        return mTarget != null;
    }

    /**
     * Checks if the connection to a given anchor is valid.
     * @param anchor the anchor we want to connect to
     * @return true if it's a compatible anchor
     */
    public boolean isValidConnection(ConstraintAnchor anchor) {
        if (anchor == null) {
            return false;
        }
        Type target = anchor.getType();
        if (target == mType) {
            if (!USE_CENTER_ANCHOR) {
                if (mType == Type.CENTER) {
                    return false;
                }
            }
            if (mType == Type.BASELINE
                    && (!anchor.getOwner().hasBaseline() || !getOwner().hasBaseline())) {
                return false;
            }
            return true;
        }
        switch (mType) {
            case CENTER: {
                // allow everything but baseline and center_x/center_y
                return target != Type.BASELINE && target != Type.CENTER_X
                        && target != Type.CENTER_Y;
            }
            case LEFT:
            case RIGHT: {
                boolean isCompatible = target == Type.LEFT || target == Type.RIGHT;
                if (anchor.getOwner() instanceof Guideline) {
                    isCompatible = isCompatible || target == Type.CENTER_X;
                }
                return isCompatible;
            }
            case TOP:
            case BOTTOM: {
                boolean isCompatible = target == Type.TOP || target == Type.BOTTOM;
                if (anchor.getOwner() instanceof Guideline) {
                    isCompatible = isCompatible || target == Type.CENTER_Y;
                }
                return isCompatible;
            }
        }
        return false;
    }

    /**
     * Return true if this anchor is a side anchor
     *
     * @return true if side anchor
     */
    public boolean isSideAnchor() {
        switch (mType) {
            case LEFT:
            case RIGHT:
            case TOP:
            case BOTTOM:
                return true;
        }
        return false;
    }

    /**
     * Return true if the connection to the given anchor is in the
     * same dimension (horizontal or vertical)
     *
     * @param anchor the anchor we want to connect to
     * @return true if it's an anchor on the same dimension
     */
    public boolean isSimilarDimensionConnection(ConstraintAnchor anchor) {
        Type target = anchor.getType();
        if (target == mType) {
            return true;
        }
        switch (mType) {
            case CENTER: {
                return target != Type.BASELINE;
            }
            case LEFT:
            case RIGHT:
            case CENTER_X: {
                return target == Type.LEFT || target == Type.RIGHT || target == Type.CENTER_X;
            }
            case TOP:
            case BOTTOM:
            case CENTER_Y:
            case BASELINE: {
                return target == Type.TOP || target == Type.BOTTOM || target == Type.CENTER_Y || target == Type.BASELINE;
            }
        }
        return false;
    }

    /**
     * Set the strength of the connection (if there's one)
     * @param strength the new strength of the connection.
     */
    public void setStrength(Strength strength) {
        if (isConnected()) {
            mStrength = strength;
        }
    }

    /**
     * Set the margin of the connection (if there's one)
     * @param margin the new margin of the connection
     */
    public void setMargin(int margin) {
        if (isConnected()) {
            mMargin = margin;
        }
    }

    /**
     * Utility function returning true if this anchor is a vertical one.
     *
     * @return true if vertical anchor, false otherwise
     */
    public boolean isVerticalAnchor() {
        switch (mType) {
            case LEFT:
            case RIGHT:
            case CENTER:
            case CENTER_X:
                return false;
        }
        return true;
    }

    /**
     * Return a string representation of this anchor
     *
     * @return string representation of the anchor
     */
    @Override
    public String toString() {
        return mOwner.getDebugName() + ":" + mType.toString();
    }

    /**
     * Return the priority level of the anchor (higher is stronger).
     * This method is used to pick an anchor among many when there's a choice (we use it
     * for the snapping decisions)
     *
     * @return priority level
     */
    public int getSnapPriorityLevel() {
        switch (mType) {
            case LEFT: return 1;
            case RIGHT: return 1;
            case CENTER_X: return 0;
            case TOP: return 0;
            case BOTTOM: return 0;
            case CENTER_Y: return 1;
            case BASELINE: return 2;
            case CENTER: return 3;
        }
        return 0;
    }

    /**
     * Return the priority level of the anchor (higher is stronger).
     * This method is used to pick an anchor among many when there's a choice (we use it
     * for finding the closest anchor)
     *
     * @return priority level
     */
    public int getPriorityLevel() {
        switch (mType) {
            case CENTER_X: return 0;
            case CENTER_Y: return 0;
            case BASELINE: return 1;
            case LEFT: return 2;
            case RIGHT: return 2;
            case TOP: return 2;
            case BOTTOM: return 2;
            case CENTER: return 2;
        }
        return 0;
    }

    /**
     * Utility function to check if the anchor is compatible with another one.
     * Used for snapping.
     *
     * @param anchor the anchor we are checking against
     * @return true if compatible, false otherwise
     */
    public boolean isSnapCompatibleWith(ConstraintAnchor anchor) {
        if (mType == Type.CENTER) {
            return false;
        }
        if (mType == anchor.getType()) {
            return true;
        }
        switch (mType) {
            case LEFT: {
                switch (anchor.getType()) {
                    case RIGHT: return true;
                    case CENTER_X: return true;
                    default: return false;
                }
            }
            case RIGHT: {
                switch (anchor.getType()) {
                    case LEFT: return true;
                    case CENTER_X: return true;
                    default: return false;
                }
            }
            case CENTER_X: {
                switch (anchor.getType()) {
                    case LEFT: return true;
                    case RIGHT: return true;
                    default: return false;
                }
            }
            case TOP: {
                switch (anchor.getType()) {
                    case BOTTOM: return true;
                    case CENTER_Y: return true;
                    default: return false;
                }
            }
            case BOTTOM: {
                switch (anchor.getType()) {
                    case TOP: return true;
                    case CENTER_Y: return true;
                    default: return false;
                }
            }
            case CENTER_Y: {
                switch (anchor.getType()) {
                    case TOP: return true;
                    case BOTTOM: return true;
                    default: return false;
                }
            }
        }
        return false;
    }

    /**
     * Return true if we can connect this anchor to this target.
     * We recursively follow connections in order to detect eventual cycles; if we
     * do we disallow the connection.
     * We also only allow connections to direct parent, siblings, and descendants.
     *
     * @param target the ConstraintWidget we are trying to connect to
     * @return true if the connection is allowed, false otherwise
     */
    public boolean isConnectionAllowed(ConstraintWidget target) {
        HashSet<ConstraintWidget> checked = new HashSet<>();
        checked.add(getOwner());
        return isConnectionAllowedRecursive(target, checked);
    }

    /**
     * Recursive with check for loop
     *
     * @param target
     * @param checked
     * @return
     */
    private boolean isConnectionAllowedRecursive(ConstraintWidget target, HashSet<ConstraintWidget> checked) {
        if (checked.contains(target)) {
            // we connect back to ourselves
            return false;
        }
        if (getOwner().hasAncestor(target)) {
            // the target is one of our ancestor, we should only allow the connection
            // if it's a direct ancestor
            if (target != getOwner().getParent()) {
                return false;
            }
        }
        if (target.hasAncestor(getOwner())) {
            // the target is one of our children, we won't allow the connection
            return false;
        }
        ArrayList<ConstraintAnchor> targetAnchors = target.getAnchors();
        for (int i = 0, targetAnchorsSize = targetAnchors.size(); i < targetAnchorsSize; i++) {
            final ConstraintAnchor anchor = targetAnchors.get(i);
            if (anchor.isSimilarDimensionConnection(this) && anchor.isConnected()) {
                ConstraintWidget nextTarget = anchor.getTarget().getOwner();
                if (nextTarget == anchor.getOwner()) {
                    return false;
                }
                if (nextTarget == getOwner()) {
                    return false;
                }
                if (!isConnectionAllowedRecursive(nextTarget, checked)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the opposite anchor to this one
     * @return opposite anchor
     */
    public ConstraintAnchor getOpposite() {
        switch (mType) {
            case LEFT: {
                return mOwner.getAnchor(Type.RIGHT);
            }
            case RIGHT: {
                return mOwner.getAnchor(Type.LEFT);
            }
            case TOP: {
                return mOwner.getAnchor(Type.BOTTOM);
            }
            case BOTTOM: {
                return mOwner.getAnchor(Type.TOP);
            }
        }
        return null;
    }
}
