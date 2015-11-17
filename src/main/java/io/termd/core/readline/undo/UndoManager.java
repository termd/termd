/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.termd.core.readline.undo;

import java.util.Stack;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class UndoManager {

    private static short UNDO_SIZE = 50;

    private Stack<UndoAction> undoStack;
    private int counter;

    public UndoManager() {
        undoStack = new Stack<>();
        undoStack.setSize(UNDO_SIZE);
        counter = 0;
    }

    public UndoAction getNext() {
        if(counter > 0) {
            counter--;
            return undoStack.pop();
        }
        else
            return null;
    }

    public void addUndo(UndoAction u) {
        if(counter <= UNDO_SIZE) {
            counter++;
            undoStack.push(u);
        }
        else {
            undoStack.remove(UNDO_SIZE);
            undoStack.push(u);
        }

    }

    public void clear() {
        undoStack.clear();
        counter = 0;
    }

    public boolean isEmpty() {
        return (counter == 0);
    }

    public int size() {
        return counter;
    }
}
