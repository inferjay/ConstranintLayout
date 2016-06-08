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
package android.support.constraint.solver;

import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class GroupingTest {

    @Test
    public void testBasicGrouping() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(37, 52, 1000, 1000);
        final ConstraintWidget A = new ConstraintWidget(20, 20, 200, 100);
        final ConstraintWidget B = new ConstraintWidget(200, 200, 200, 100);
        final ConstraintWidget C = new ConstraintWidget(400, 400, 200, 100);

        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.setDebugName("root");
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        // root <- A.left
        //         A.right <- B.left
        //                    B.right <- C.left
        // root <- A.top
        //         A.bottom <- B.bottom
        //      <- C.top

        root.add(A);
        root.add(B);
        root.add(C);

        ArrayList<ConstraintWidget> widgets = new ArrayList();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            root.layout();
        }
        long time = System.currentTimeMillis() - start;
        int leftA = A.getLeft();
        int topA = A.getTop();
        int leftB = B.getLeft();
        int topB = B.getTop();
        int leftC = C.getLeft();
        int topC = C.getTop();
        System.out.println("A: " + A + " leftA: " + leftA + " topA: " + topA);
        System.out.println("B: " + B + " leftB: " + leftB + " topB: " + topB);
        System.out.println("C: " + C + " leftC: " + leftC + " topC: " + topC);

        start = System.currentTimeMillis();
        int group = root.layoutFindGroupsSimple();
        for (int i = 0; i < 1000; i++) {
            root.layoutWithGroup(group);
        }
        long time2 = System.currentTimeMillis() - start;
        System.out.println("layout: " + time + " ms "  + " group layout: " + time2 + " ms");
        assertEquals(A.getAnchor(ConstraintAnchor.Type.LEFT).getGroup(), 0,"A left group");
        assertEquals(B.getAnchor(ConstraintAnchor.Type.LEFT).getGroup(), 0,"B left group");
        assertEquals(C.getAnchor(ConstraintAnchor.Type.LEFT).getGroup(), 0,"A left group");
        assertEquals(A.getAnchor(ConstraintAnchor.Type.TOP).getGroup(), 1,"B top group");
        assertEquals(B.getAnchor(ConstraintAnchor.Type.BOTTOM).getGroup(), 1,"B bottom group");
        assertEquals(C.getAnchor(ConstraintAnchor.Type.TOP).getGroup(), 1,"c top group");
        assertEquals(leftA, A.getLeft());
        assertEquals(topA, A.getTop());
        assertEquals(leftB, B.getLeft());
        assertEquals(topB, B.getTop());
        assertEquals(leftC, C.getLeft());
        assertEquals(topC, C.getTop());
    }

    @Test
    public void testGroupSolver() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(37, 52, 1000, 1000);
        final ConstraintWidget A = new ConstraintWidget(20, 20, 200, 100);
        final ConstraintWidget B = new ConstraintWidget(200, 200, 200, 100);
        final ConstraintWidget C = new ConstraintWidget(400, 400, 200, 100);

        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.setDebugName("root");
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);

        // root <- A.left
        //         A.right <- B.left
        //                    B.right <- C.left
        // root <- A.top
        //         A.bottom <- B.bottom
        //      <- C.top

        root.add(A);
        root.add(B);
        root.add(C);

        ArrayList<ConstraintWidget> widgets = new ArrayList();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            root.layout();
        }
        long time = System.currentTimeMillis() - start;
        int leftA = A.getLeft();
        int topA = A.getTop();
        int leftB = B.getLeft();
        int topB = B.getTop();
        int leftC = C.getLeft();
        int topC = C.getTop();
        System.out.println("A: " + A + " leftA: " + leftA + " topA: " + topA);
        System.out.println("B: " + B + " leftB: " + leftB + " topB: " + topB);
        System.out.println("C: " + C + " leftC: " + leftC + " topC: " + topC);

        start = System.currentTimeMillis();
        B.setOrigin(0, 0);
        C.setOrigin(0, 0);
        int group = root.layoutFindGroupsSimple();
        root.layoutWithGroup(group);
        long time2 = System.currentTimeMillis() - start;
        System.out.println("layout: " + time + " ms "  + " group layout: " + time2 + " ms");
        System.out.println("A: " + A);
        System.out.println("B: " + B);
        System.out.println("C: " + C);
        assertEquals(A.getAnchor(ConstraintAnchor.Type.LEFT).getGroup(), 0,"A left group");
        assertEquals(B.getAnchor(ConstraintAnchor.Type.LEFT).getGroup(), 0,"B left group");
        assertEquals(C.getAnchor(ConstraintAnchor.Type.LEFT).getGroup(), 0,"A left group");
        assertEquals(A.getAnchor(ConstraintAnchor.Type.TOP).getGroup(), 1,"B top group");
        assertEquals(B.getAnchor(ConstraintAnchor.Type.BOTTOM).getGroup(), 1,"B bottom group");
        assertEquals(C.getAnchor(ConstraintAnchor.Type.TOP).getGroup(), 1,"C top group");
        assertEquals(A.getLeft(), leftA);
        assertEquals(A.getTop(), topA);
        assertEquals(B.getLeft(), leftB);
        assertEquals(B.getTop(), topB);
        assertEquals(C.getLeft(),leftC);
        assertEquals(C.getTop(), topC);
    }
}
