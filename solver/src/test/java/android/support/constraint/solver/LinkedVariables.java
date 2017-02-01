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

/**
 * Store a set of variables and their values in a linked list.
 */
public class LinkedVariables {
    private static final boolean DEBUG = false;
    private final ArrayRow mRow;
    private final Cache mCache;

    public static Pools.Pool<LinkedVariables.Link> linkedVariablesPool = new Pools.SimplePool<>(256);

    static class Link {
        SolverVariable variable;
        float value;
        Link next;
        public Link() {
            sCreation++;
        }
        public String toString() {
            return "" + value + " " + variable;
        }
    }

    private Link head = null;

    int currentSize = 0;
    private SolverVariable candidate = null;

    float epsilon = 0.001f;
    public static int sCreation = 0;

    public LinkedVariables(ArrayRow arrayRow, Cache cache) {
        mRow = arrayRow;
        mCache = cache;
    }

    @Override
    public String toString() {
        String result = "";
        Link current = head;
        while (current != null) {
            result += " -> (" + current + ")";
            current = current.next;
        }
        return result;
    }

    public boolean hasAtLeastOnePositiveVariable() {
        Link current = head;
        while (current != null) {
            if (current.value > 0) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public void invert() {
        Link current = head;
        while (current != null) {
            current.value *= -1;
            current = current.next;
        }
    }

    public void divideByAmount(float amount) {
        Link current = head;
        while (current != null) {
            current.value /= amount;
            current = current.next;
        }
    }

    public void updateClientEquations(ArrayRow row) {
        Link current = head;
        while (current != null) {
            current.variable.addClientEquation(row);
            current = current.next;
        }
    }

    public SolverVariable pickPivotCandidate() {
        Link current = head;
        SolverVariable restrictedCandidate = null;
        SolverVariable unrestrictedCandidate = null;
        while (current != null) {
            float amount = current.value;
            if (amount < 0) {
                if (amount > -epsilon) {
                    current.value = 0;
                    amount = 0;
                }
            } else {
                if (amount < epsilon) {
                    current.value = 0;
                    amount = 0;
                }
            }
            if (amount != 0) {
                if (current.variable.mType == SolverVariable.Type.UNRESTRICTED) {
                    if (amount < 0) {
                        return current.variable;
                    } else if (unrestrictedCandidate == null) {
                        unrestrictedCandidate = current.variable;
                    }
                } else if (amount < 0 && restrictedCandidate == null) {
                    restrictedCandidate = current.variable;
                }
            }
            current = current.next;
        }
        if (unrestrictedCandidate != null) {
            return unrestrictedCandidate;
        }
        return restrictedCandidate;
    }

    public void updateFromRow(ArrayRow self, ArrayRow definition) {
        Link current = head;
        Link previous = null;
        Link newVariables = linkedVariablesPool.acquire();
        if (newVariables == null) {
            newVariables = new Link();
        }
        newVariables.next = null;
        Link lastOfNewVariables = newVariables;
        while (current != null) {
            if (current.variable == definition.variable) {
                float amount = current.value;
                if (!definition.isSimpleDefinition) {
                    Link definitionCurrent = ((LinkedVariables) (Object) definition.variables).head;
                    while (definitionCurrent != null) {
                        Link temp = linkedVariablesPool.acquire(); if (temp == null) { temp = new Link(); }
                        temp.variable = definitionCurrent.variable;
                        temp.value = definitionCurrent.value * amount;
                        temp.next = null;
                        lastOfNewVariables.next = temp;
                        lastOfNewVariables = temp;
                        definitionCurrent = definitionCurrent.next;
                    }
                }
                self.constantValue += definition.constantValue * amount;
                definition.variable.removeClientEquation(self);
                if (previous == null) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }
                linkedVariablesPool.release(current);
                currentSize--;
            } else {
                previous = current;
            }
            current = current.next;
        }
        current = newVariables.next;
        while (current != null) {
            add(current.variable, current.value);
            previous = current;
            current = current.next;
            linkedVariablesPool.release(previous);
        }
        linkedVariablesPool.release(newVariables);
    }

    public void updateFromSystem(ArrayRow self, ArrayRow[] rows) {
        Link current = head;
        Link previous = null;
        Link newVariables = linkedVariablesPool.acquire();
        if (newVariables == null) {
            newVariables = new Link();
        }
        newVariables.next = null;
        Link lastOfNewVariables = newVariables;
        while (current != null) {
            int definitionIndex = current.variable.definitionId;
            if (definitionIndex != -1) {
                float amount = current.value;
                ArrayRow definition = rows[definitionIndex];
                if (!definition.isSimpleDefinition) {
                    Link definitionCurrent = ((LinkedVariables) (Object) definition.variables).head;
                    while (definitionCurrent != null) {
                        Link temp = linkedVariablesPool.acquire(); if (temp == null) { temp = new Link(); }
                        temp.variable = definitionCurrent.variable;
                        temp.value = definitionCurrent.value * amount;
                        temp.next = null;
                        lastOfNewVariables.next = temp;
                        lastOfNewVariables = temp;
                        definitionCurrent = definitionCurrent.next;
                    }
                }
                self.constantValue += definition.constantValue * amount;
                definition.variable.removeClientEquation(self);
                if (previous == null) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }
                linkedVariablesPool.release(current);
                currentSize--;
            } else {
                previous = current;
            }
            current = current.next;
        }
        current = newVariables.next;
        while (current != null) {
            add(current.variable, current.value);
            previous = current;
            current = current.next;
            linkedVariablesPool.release(previous);
        }
        linkedVariablesPool.release(newVariables);
    }

    public SolverVariable getPivotCandidate() {
        if (candidate == null) {
            Link current = head;
            while (current != null) {
                if (current.value < 0) {
                    if (candidate == null || current.variable.definitionId < candidate.definitionId) {
                        candidate = current.variable;
                    }
                }
                current = current.next;
            }
        }
        return candidate;
    }

    public final int size() {
        return currentSize;
    }

    public final SolverVariable getVariable(int index) {
        Link current = head;
        int count = 0;
        while (count != index) {
            current = current.next;
            count++;
        }
        return current != null ? current.variable : null;
    }

    public final float getVariableValue(int index) {
        Link current = head;
        int count = 0;
        while (count != index) {
            current = current.next;
            count++;
        }
        return current != null ? current.value : 0;
    }

    public final void updateArray(LinkedVariables target, float amount) {
        if (amount == 0) {
            return;
        }
        Link current = head;
        while (current != null) {
            target.put(current.variable, target.get(current.variable) + (current.value * amount));
            current = current.next;
        }
    }

    public final void setVariable(int index, float value) {
        Link current = head;
        int count = 0;
        while (count != index) {
            current = current.next;
            count++;
        }
        current.value = value;
    }

    public final float get(SolverVariable v) {
        Link current = head;
        while (current != null) {
            if (current.variable == v) {
                return current.value;
            }
            current = current.next;
        }
        return 0;
    }

    public final void put(SolverVariable variable, float value) {
        if (value == 0) {
            remove(variable);
            return;
        }
        Link current = head;
        Link previous = null;
        while (current != null) {
            if (current.variable == variable) {
                current.value = value;
                return;
            }
            if (current.variable.id < variable.id) {
                previous = current;
            }
            current = current.next;
        }
        current = linkedVariablesPool.acquire();
        if (current == null) {
            current = new Link();
        }
        current.value = value;
        current.variable = variable;
        current.next = null;
        if (previous != null) {
            current.next = previous.next;
            previous.next = current;
        } else {
            current.next = head;
            head = current;
        }
        if (head == null) {
            head = current;
        }
        currentSize++;
    }

    public final void add(SolverVariable variable, float value) {
        if (value == 0) {
            remove(variable);
            return;
        }
        Link current = head;
        Link previous = null;
        while (current != null) {
            if (current.variable == variable) {
                current.value += value;
                if (current.value == 0) {
                    if (current == head) {
                        head = current.next;
                    } else {
                        previous.next = current.next;
                    }
                    current.variable.removeClientEquation(mRow);
                    linkedVariablesPool.release(current);
                    currentSize--;
                }
                return;
            }
            if (current.variable.id < variable.id) {
                previous = current;
            }
            current = current.next;
        }
        current = linkedVariablesPool.acquire();
        if (current == null) {
            current = new Link();
        }
        current.value = value;
        current.variable = variable;
        current.next = null;
        if (previous != null) {
            current.next = previous.next;
            previous.next = current;
        } else {
            current.next = head;
            head = current;
        }
        if (head == null) {
            head = current;
        }
        currentSize++;
    }

    public final void clear() {
        Link current = head;
        while (current != null) {
            Link previous = current;
            current = current.next;
            linkedVariablesPool.release(previous);
        }
        head = null;
        currentSize = 0;
    }

    public final boolean containsKey(SolverVariable variable) {
        Link current = head;
        while (current != null) {
            if (current.variable == variable) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public final float remove(SolverVariable variable) {
        if (candidate == variable) {
            candidate = null;
        }
        Link current = head;
        Link previous = null;
        while (current != null) {
            if (current.variable == variable) {
                float value = current.value;
                if (current == head) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }
                current.variable.removeClientEquation(mRow);
                linkedVariablesPool.release(current);
                currentSize--;
                return value;
            }
            previous = current;
            current = current.next;
        }
        return 0;
    }

    public int sizeInBytes() {
        int size = 0;
        size += 4 + 4 + 4 + 4;
        return size;
    }

    public void display() {
        int count = size();
        System.out.print("{ ");
        for (int i = 0; i < count; i++) {
            SolverVariable v = getVariable(i);
            if (v == null) {
                continue;
            }
            System.out.print(v + " = " + getVariableValue(i) + " ");
        }
        System.out.println(" }");
    }
}
