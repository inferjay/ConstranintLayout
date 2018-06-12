/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.support.constraint.solver.widgets.ChainHead;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ChainHeadTest {

    @Test
    public void basicHorizontalChainHeadTest(){
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);

        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");

        root.add(A);
        root.add(B);
        root.add(C);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);

        ChainHead chainHead = new ChainHead(A, ConstraintWidget.HORIZONTAL, false);
        chainHead.define();

        assertEquals(chainHead.getHead(), A);
        assertEquals(chainHead.getFirst(), A);
        assertEquals(chainHead.getFirstVisibleWidget(), A);
        assertEquals(chainHead.getLast(), C);
        assertEquals(chainHead.getLastVisibleWidget(), C);

        A.setVisibility(ConstraintWidget.GONE);

        chainHead = new ChainHead(A, ConstraintWidget.HORIZONTAL, false);
        chainHead.define();

        assertEquals(chainHead.getHead(), A);
        assertEquals(chainHead.getFirst(), A);
        assertEquals(chainHead.getFirstVisibleWidget(), B);


        chainHead = new ChainHead(A, ConstraintWidget.HORIZONTAL, true);
        chainHead.define();

        assertEquals(chainHead.getHead(), C);
        assertEquals(chainHead.getFirst(), A);
        assertEquals(chainHead.getFirstVisibleWidget(), B);
    }

    @Test
    public void basicVerticalChainHeadTest(){
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);

        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");

        root.add(A);
        root.add(B);
        root.add(C);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        ChainHead chainHead = new ChainHead(A, ConstraintWidget.VERTICAL, false);
        chainHead.define();

        assertEquals(chainHead.getHead(), A);
        assertEquals(chainHead.getFirst(), A);
        assertEquals(chainHead.getFirstVisibleWidget(), A);
        assertEquals(chainHead.getLast(), C);
        assertEquals(chainHead.getLastVisibleWidget(), C);

        A.setVisibility(ConstraintWidget.GONE);

        chainHead = new ChainHead(A, ConstraintWidget.VERTICAL, false);
        chainHead.define();

        assertEquals(chainHead.getHead(), A);
        assertEquals(chainHead.getFirst(), A);
        assertEquals(chainHead.getFirstVisibleWidget(), B);


        chainHead = new ChainHead(A, ConstraintWidget.VERTICAL, true);
        chainHead.define();

        assertEquals(chainHead.getHead(), A);
        assertEquals(chainHead.getFirst(), A);
        assertEquals(chainHead.getFirstVisibleWidget(), B);
    }

    @Test
    public void basicMatchConstraintTest(){
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);

        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");

        root.add(A);
        root.add(B);
        root.add(C);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setHorizontalWeight(1f);
        B.setHorizontalWeight(2f);
        C.setHorizontalWeight(3f);

        ChainHead chainHead = new ChainHead(A, ConstraintWidget.HORIZONTAL, false);
        chainHead.define();

        assertEquals(chainHead.getFirstMatchConstraintWidget(), A);
        assertEquals(chainHead.getLastMatchConstraintWidget(), C);
        assertEquals(chainHead.getTotalWeight(), 6f);

        C.setVisibility(ConstraintWidget.GONE);

        chainHead = new ChainHead(A, ConstraintWidget.HORIZONTAL, false);
        chainHead.define();

        assertEquals(chainHead.getFirstMatchConstraintWidget(), A);
        assertEquals(chainHead.getLastMatchConstraintWidget(), B);
        assertEquals(chainHead.getTotalWeight(), 3f);
    }

}
