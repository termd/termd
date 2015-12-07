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

import io.termd.core.readline.editing.EditMode;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class UpCaseForwardWord extends ForwardWord {

    public UpCaseForwardWord() {
        super(false, EditMode.Status.UP_CASE);
    }

    public UpCaseForwardWord(boolean viMode) {
        super(viMode, EditMode.Status.UP_CASE);
    }

    @Override
    public String name() {
        return "upcase-word";
    }
}
