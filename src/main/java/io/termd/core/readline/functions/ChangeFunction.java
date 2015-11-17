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
package io.termd.core.readline.functions;

import io.termd.core.readline.LineBuffer;
import io.termd.core.readline.Readline;
import io.termd.core.readline.editing.EditMode;
import io.termd.core.readline.undo.UndoAction;
import io.termd.core.util.Helper;

import java.util.Arrays;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
abstract class ChangeFunction extends MovementFunction {

    private EditMode.Status status;
    protected boolean viMode;

    ChangeFunction(EditMode.Status  status) {
        this.status = status;
        viMode = false;
    }

    ChangeFunction(boolean viMode, EditMode.Status status) {
        this.status = status;
        this.viMode = viMode;
    }

    protected EditMode.Status getStatus() {
        return status;
    }

    protected final void apply(int cursor, Readline.Interaction interaction) {
        apply(cursor, interaction.buffer().getCursor(), interaction);
    }

    protected final void apply(int cursor, int oldCursor, Readline.Interaction interaction) {
        if(status == EditMode.Status.DELETE || status == EditMode.Status.CHANGE) {
            addActionToUndoStack(interaction);
            LineBuffer buf = interaction.buffer().copy();
            if(cursor < oldCursor) {
                //add to pastemanager
                interaction.getPasteManager().addText(
                        Arrays.copyOfRange(interaction.buffer().toArray(), cursor, oldCursor));
                //delete buffer
                buf.delete(cursor - oldCursor);
                //buf.moveCursor(cursor - oldCursor);
            }
            else {
                //add to pastemanager
                interaction.getPasteManager().addText(
                        Arrays.copyOfRange(buf.toArray(), oldCursor, cursor));
                //delete buffer
                buf.delete(cursor - oldCursor);
            }

            //TODO: must check if we're in edit mode
            if(viMode && status == EditMode.Status.DELETE &&
                    oldCursor == buf.getSize())
                buf.moveCursor(-1);

            interaction.refresh(buf);
        }
        else if(status == EditMode.Status.MOVE) {
            LineBuffer buf = interaction.buffer().copy();
            buf.moveCursor(cursor-oldCursor);
            interaction.refresh(buf);
        }
        else if(status == EditMode.Status.YANK) {
            if(cursor < oldCursor)
                interaction.getPasteManager().addText(
                        Arrays.copyOfRange(interaction.buffer().toArray(), cursor, oldCursor));
            else if(cursor > oldCursor)
                interaction.getPasteManager().addText(
                        Arrays.copyOfRange(interaction.buffer().toArray(), oldCursor, cursor));
        }

        else if(status == EditMode.Status.UP_CASE) {
            LineBuffer buf = interaction.buffer().copy();
            if(cursor < oldCursor) {
                addActionToUndoStack(interaction);
                for( int i = cursor; i < oldCursor; i++) {
                    interaction.buffer().replace(i,
                            Character.toUpperCase(buf.getAt(i)));
                }
            }
            else {
                addActionToUndoStack(interaction);
                for( int i = oldCursor; i < cursor; i++) {
                    interaction.buffer().replace(i, Character.toUpperCase(
                            buf.getAt(i)));
                }
            }
            buf.moveCursor(cursor - oldCursor);
            interaction.refresh(buf);
        }
        else if(status == EditMode.Status.DOWN_CASE) {
            LineBuffer buf = interaction.buffer().copy();
            if(cursor < oldCursor) {
                addActionToUndoStack(interaction);
                for( int i = cursor; i < oldCursor; i++) {
                    buf.replace(i, Character.toLowerCase(interaction.buffer().getAt(i)));
                }
            }
            else {
                addActionToUndoStack(interaction);
                for( int i = oldCursor; i < cursor; i++) {
                    buf.replace(i, Character.toLowerCase(interaction.buffer().getAt(i)));
                }
            }
            buf.moveCursor(cursor - oldCursor);
            interaction.refresh(buf);
        }
        else if(status == EditMode.Status.CAPITALIZE) {
            LineBuffer buf = interaction.buffer().copy();
            String word = Helper.findWordClosestToCursor(buf.toString(), oldCursor);
            if(word.length() > 0) {
                addActionToUndoStack(interaction);
                int pos = buf.toString().indexOf(word, oldCursor - word.length());
                if(pos < 0)
                    pos = 0;
                buf.replace(pos, Character.toUpperCase(interaction.buffer().getAt(pos)));

                buf.moveCursor(cursor - oldCursor);
                interaction.refresh(buf);
            }
        }
        interaction.resume();
    }

    protected final void addActionToUndoStack(Readline.Interaction interaction) {
        interaction.getUndoManager().addUndo(new UndoAction(
                interaction.buffer().getCursor(),
                interaction.buffer().toArray()));
    }

}
