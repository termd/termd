/*
 * Copyright 2015 Julien Viet
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

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class UndoAction {

    private int cursorPosition;
    private int[] buffer;

    public UndoAction(int cursorPosition, int[] buffer) {
        setCursorPosition(cursorPosition);
        setBuffer(buffer);
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    private void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public int[] getBuffer() {
        return buffer;
    }

    private void setBuffer(int[] buffer) {
        this.buffer = buffer;
    }
}
