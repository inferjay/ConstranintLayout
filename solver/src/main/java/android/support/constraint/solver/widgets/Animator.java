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

public class Animator {
    private static final boolean DEBUG = false;
    private static final boolean USE_EASE_IN_OUT = true;

    private final ConstraintWidget mWidget;
    private Frame animTarget = new Frame();
    private Frame animStart = new Frame();
    private Frame animCurrent = new Frame();

    private long animStartTime = 0;
    private long animDuration = 350; // ms

    private boolean mAnimating = false;

    private static boolean sAllowsAnimation = false;

    public static void setAnimationEnabled(boolean value) {
        sAllowsAnimation = value;
    }

    public static boolean doAnimation() {
        return sAllowsAnimation;
    }

    public Animator(ConstraintWidget widget) {
        mWidget = widget;
    }

    public boolean isAnimating() {
        return mAnimating;
    }

    public void start() {
        animStartTime = System.currentTimeMillis();
        mAnimating = true;
    }

    public static double EaseInOutinterpolator(double progress, double begin, double end) {
        double change = (end - begin) / 2f;
        progress *= 2f;
        if (progress < 1f) {
            return (change * progress * progress + begin);
        }
        progress -= 1f;
        return (-change * (progress * (progress - 2f) - 1f) + begin);
    }

    private static float linearInterpolator(float progress, float begin, float end) {
        return (end * progress + begin * (1 - progress));
    }

    private static int interpolator(float progress, float begin, float end) {
        if (USE_EASE_IN_OUT) {
            return (int) EaseInOutinterpolator(progress, begin, end);
        }
        return (int) linearInterpolator(progress, begin, end);
    }

    public void step() {
        long currentTime = System.currentTimeMillis();
        if (currentTime <= animStartTime + animDuration && currentTime >= animStartTime) {
            float progress =
                    (float) (currentTime - animStartTime) / (float) animDuration;
            animCurrent.left = interpolator(progress, animStart.left, animTarget.left);
            animCurrent.right = interpolator(progress, animStart.right, animTarget.right);
            animCurrent.top = interpolator(progress, animStart.top, animTarget.top);
            animCurrent.bottom = interpolator(progress, animStart.bottom, animTarget.bottom);
        } else {
            animCurrent.left = animTarget.left;
            animCurrent.top = animTarget.top;
            animCurrent.right = animTarget.right;
            animCurrent.bottom = animTarget.bottom;
            mAnimating = false;
        }
    }

    /**
     * Receives the current system values for the given widget
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void animate(int left, int top, int right, int bottom) {
        animCurrent.set(left, top, right, bottom);
        if (!isAnimating()) {
            // check if we need to start an animation
            if (left != mWidget.getInternalDrawX() || top != mWidget.getInternalDrawY()
                    || right != mWidget.getInternalDrawRight() ||
                    bottom != mWidget.getInternalDrawBottom()) {
                if (DEBUG) {
                    System.out.println("Widget animating " + mWidget
                            + " from (" + mWidget.getDrawX() + ", " + mWidget.getDrawY() + ")"
                            + " to (" + left + ", " + top + ")");
                }
                animStart.set(mWidget.getInternalDrawX(), mWidget.getInternalDrawY(),
                        mWidget.getInternalDrawRight(),
                        mWidget.getInternalDrawBottom());
                start();
            }
        }
        if (isAnimating()) {
            // always update the target with the current system resolution
            animTarget.set(left, top, right, bottom);
            step();
        }
    }

    public int getCurrentLeft() {
        return animCurrent.left;
    }

    public int getCurrentTop() {
        return animCurrent.top;
    }

    public int getCurrentRight() {
        return animCurrent.right;
    }

    public int getCurrentBottom() {
        return animCurrent.bottom;
    }

    static class Frame {
        int left;
        int right;
        int top;
        int bottom;

        void set(int l, int t, int r, int b) {
            left = l;
            top = t;
            right = r;
            bottom = b;
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getRight() {
            return right;
        }

        public int getBottom() {
            return bottom;
        }
    }

}
