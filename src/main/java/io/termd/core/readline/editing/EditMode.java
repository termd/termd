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
package io.termd.core.readline.editing;

import io.termd.core.readline.Function;
import io.termd.core.readline.KeyEvent;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface EditMode {

    Mode getMode();

    KeyEvent[] keys();

    Status getCurrentStatus();

    Function parse(KeyEvent event);


    void addAction(int[] input, String action);

    enum Status {
        DELETE,
        MOVE,
        YANK,
        CHANGE,
        EDIT,
        COMMAND,
        HISTORY,
        SEARCH,
        REPEAT,
        // MISC
        NEWLINE,
        PASTE,
        PASTE_FROM_CLIPBOARD,
        COMPLETE,
        UNDO,
        CASE,
        EXIT,
        CLEAR,
        ABORT,
        CHANGE_EDITMODE,
        NO_ACTION,
        REPLACE,
        INTERRUPT,
        IGNORE_EOF,
        EOF,
        UP_CASE,
        DOWN_CASE,
        CAPITALIZE,
    }

    enum Mode {
        EMACS, VI
    }
}
