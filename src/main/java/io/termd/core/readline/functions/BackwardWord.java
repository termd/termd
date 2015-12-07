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
package io.termd.core.readline.functions;

import io.termd.core.readline.Readline;
import io.termd.core.readline.editing.EditMode;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class BackwardWord extends ChangeFunction {

    private boolean viMode;

    public BackwardWord() {
        super(EditMode.Status.MOVE);
        this.viMode = false;
    }

    public BackwardWord(boolean viMode, EditMode.Status status) {
        super(status);
        this.viMode = viMode;
    }

    @Override
    public void apply(Readline.Interaction interaction) {
        int cursor = interaction.buffer().getCursor();
        //the cursor position might be > the buffer

        if(viMode) {
            //String buffer = interaction.buffer().toString();
            while(cursor > 0 && isSpace((char) interaction.buffer().getAt(cursor - 1)))
                cursor--;
            if(cursor > 0 && isDelimiter((char) interaction.buffer().getAt(cursor - 1))) {
                while(cursor > 0 && isDelimiter((char) interaction.buffer().getAt(cursor -1))
                         && !isSpace((char) interaction.buffer().getAt(cursor-1)))
                    cursor--;
            }
            else {
                while(cursor > 0 && !isDelimiter((char) interaction.buffer().getAt(cursor - 1))) {
                    cursor--;
                }
            }
        }
        else {
            while (cursor > 0 && isDelimiter((char) interaction.buffer().getAt(cursor - 1)))
                cursor--;
            while (cursor > 0 && !isDelimiter((char) interaction.buffer().getAt(cursor - 1)))
                cursor--;
        }

        apply(cursor, interaction);
    }

}
