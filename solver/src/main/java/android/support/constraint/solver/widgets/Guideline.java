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

import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.SolverVariable;

import java.util.ArrayList;

public class Guideline extends ConstraintWidget {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static final int RELATIVE_PERCENT = 0;
    public static final int RELATIVE_BEGIN = 1;
    public static final int RELATIVE_END = 2;
    public static final int RELATIVE_UNKNWON = -1;

    protected float mRelativePercent = -1;
    protected int mRelativeBegin = -1;
    protected int mRelativeEnd = -1;

    private ConstraintAnchor mAnchor = mTop;
    private int mOrientation = HORIZONTAL;
    private boolean mIsPositionRelaxed = false;
    private int mMinimumPosition = 0;

    private Rectangle mHead = new Rectangle();
    private int mHeadSize = 8;

    public Guideline() {
        mAnchors.clear();
        mAnchors.add(mAnchor);
    }

    public int getRelativeBehaviour() {
        if (mRelativePercent != -1) {
            return RELATIVE_PERCENT;
        }
        if (mRelativeBegin != -1) {
            return RELATIVE_BEGIN;
        }
        if (mRelativeEnd != -1) {
            return RELATIVE_END;
        }
        return RELATIVE_UNKNWON;
    }

    public Rectangle getHead() {
        mHead.setBounds(getDrawX() - mHeadSize, getDrawY() - 2 * mHeadSize, 2 * mHeadSize,
                2 * mHeadSize);
        if (getOrientation() == HORIZONTAL) {
            mHead.setBounds(getDrawX() - 2 * mHeadSize,
                    getDrawY() - mHeadSize,
                    2 * mHeadSize, 2 * mHeadSize);
        }
        return mHead;
    }

    public void setOrientation(int orientation) {
        if (mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;
        mAnchors.clear();
        if (mOrientation == VERTICAL) {
            mAnchor = mLeft;
        } else {
            mAnchor = mTop;
        }
        mAnchors.add(mAnchor);
    }

    public ConstraintAnchor getAnchor() {
        return mAnchor;
    }

    /**
     * Specify the xml type for the container
     *
     * @return
     */
    @Override
    public String getType() {
        return "Guideline";
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setMinimumPosition(int minimum) {
        mMinimumPosition = minimum;
    }

    public void setPositionRelaxed(boolean value) {
        if (mIsPositionRelaxed == value) {
            return;
        }
        mIsPositionRelaxed = value;
    }

    @Override
    public ConstraintAnchor getAnchor(ConstraintAnchor.Type anchorType) {
        switch (anchorType) {
            case LEFT:
            case RIGHT: {
                if (mOrientation == VERTICAL) {
                    return mAnchor;
                }
            }
            break;
            case TOP:
            case BOTTOM: {
                if (mOrientation == HORIZONTAL) {
                    return mAnchor;
                }
            }
            break;
        }
        return null;
    }

    @Override
    public ArrayList<ConstraintAnchor> getAnchors() {
        return mAnchors;
    }

    public void setGuidePercent(int value) {
        setGuidePercent(value / 100f);
    }

    public void setGuidePercent(float value) {
        if (value > -1) {
            mRelativePercent = value;
            mRelativeBegin = -1;
            mRelativeEnd = -1;
        }
    }

    public void setGuideBegin(int value) {
        if (value > -1) {
            mRelativePercent = -1;
            mRelativeBegin = value;
            mRelativeEnd = -1;
        }
    }

    public void setGuideEnd(int value) {
        if (value > -1) {
            mRelativePercent = -1;
            mRelativeBegin = -1;
            mRelativeEnd = value;
        }
    }

    public float getRelativePercent() {
        return mRelativePercent;
    }

    public int getRelativeBegin() {
        return mRelativeBegin;
    }

    public int getRelativeEnd() {
        return mRelativeEnd;
    }

    @Override
    public void addToSolver(LinearSystem system, int group) {
        ConstraintWidgetContainer parent = (ConstraintWidgetContainer) getParent();
        if (parent == null) {
            return;
        }
        ConstraintAnchor begin = parent.getAnchor(ConstraintAnchor.Type.LEFT);
        ConstraintAnchor end = parent.getAnchor(ConstraintAnchor.Type.RIGHT);
        if (mOrientation == HORIZONTAL) {
            begin = parent.getAnchor(ConstraintAnchor.Type.TOP);
            end = parent.getAnchor(ConstraintAnchor.Type.BOTTOM);
        }
        if (mRelativeBegin != -1) {
            SolverVariable guide = system.createObjectVariable(mAnchor);
            SolverVariable parentLeft = system.createObjectVariable(begin);
            system.addConstraint(
                    LinearSystem
                            .createRowEquals(system, guide, parentLeft, mRelativeBegin, false));
        } else if (mRelativeEnd != -1) {
            SolverVariable guide = system.createObjectVariable(mAnchor);
            SolverVariable parentRight = system.createObjectVariable(end);
            system.addConstraint(
                    LinearSystem
                            .createRowEquals(system, guide, parentRight, -mRelativeEnd, false));
        } else if (mRelativePercent != -1) {
            SolverVariable guide = system.createObjectVariable(mAnchor);
            SolverVariable parentLeft = system.createObjectVariable(begin);
            SolverVariable parentRight = system.createObjectVariable(end);
            system.addConstraint(LinearSystem
                    .createRowDimensionPercent(system, guide, parentLeft, parentRight,
                            mRelativePercent, mIsPositionRelaxed));
            if (mMinimumPosition > 0) {
//                system.addConstraint(system.createRowGreaterThan(guide, parentLeft, mMinimumPosition, false));
            }
        } else {
            /*
            float currentValue = mX;
            if (mOrientation == VERTICAL) {
                currentValue = mY;
            }
            float parentValue = 0;
            if (mParent != null) {
                if (mOrientation == HORIZONTAL) {
                    parentValue = mParent.getWidth();
                    mHeight = mParent.getHeight();
                } else if (mOrientation == VERTICAL) {
                    parentValue = mParent.getHeight();
                    mWidth = mParent.getWidth();
                }
            }
            addToSolver(system, currentValue, parentValue, mAnchor);
            */
        }
    }

    @Override
    public void updateFromSolver(LinearSystem system, int group) {
        if (getParent() == null) {
            return;
        }
        int value = system.getObjectVariableValue(mAnchor);
        if (mOrientation == VERTICAL) {
            setX(value);
            setY(0);
            setHeight(getParent().getHeight());
            setWidth(0);
        } else {
            setX(0);
            setY(value);
            setWidth(getParent().getWidth());
            setHeight(0);
        }
    }

    @Override
    public void setDrawOrigin(int x, int y) {
        if (mOrientation == VERTICAL) {
            int position = x - mOffsetX;
            if (mRelativeBegin != -1) {
                setGuideBegin(position);
            } else if (mRelativeEnd != -1) {
                setGuideEnd(getParent().getWidth() - position);
            } else if (mRelativePercent != -1) {
                float percent = (position / (float) getParent().getWidth());
                setGuidePercent(percent);
            }
        } else {
            int position = y - mOffsetY;
            if (mRelativeBegin != -1) {
                setGuideBegin(position);
            } else if (mRelativeEnd != -1) {
                setGuideEnd(getParent().getHeight() - position);
            } else if (mRelativePercent != -1) {
                float percent = (position / (float) getParent().getHeight());
                setGuidePercent(percent);
            }
        }
    }

    void inferRelativePercentPosition() {
        float percent = (getX() / (float) getParent().getWidth());
        if (mOrientation == HORIZONTAL) {
            percent = (getY() / (float) getParent().getHeight());
        }
        setGuidePercent(percent);
    }

    void inferRelativeBeginPosition() {
        int position = getX();
        if (mOrientation == HORIZONTAL) {
            position = getY();
        }
        setGuideBegin(position);
    }

    void inferRelativeEndPosition() {
        int position = getParent().getWidth() - getX();
        if (mOrientation == HORIZONTAL) {
            position = getParent().getHeight() - getY();
        }
        setGuideEnd(position);
    }

    public void cyclePosition() {
        if (mRelativeBegin != -1) {
            // cycle to percent-based position
            inferRelativePercentPosition();
        } else if (mRelativePercent != -1) {
            // cycle to end-based position
            inferRelativeEndPosition();
        } else if (mRelativeEnd != -1) {
            // cycle to begin-based position
            inferRelativeBeginPosition();
        }
    }
}
