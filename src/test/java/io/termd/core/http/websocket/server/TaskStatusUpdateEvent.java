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

package io.termd.core.http.websocket.server;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.termd.core.pty.PtyStatusEvent;
import io.termd.core.pty.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@JsonDeserialize(using = TaskStatusUpdateEventDeserializer.class)
public class TaskStatusUpdateEvent implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusUpdateEvent.class);

    private final String taskId;
    private final Status oldStatus;
    private final Status newStatus;
    private final String context;

    public TaskStatusUpdateEvent(PtyStatusEvent taskStatusUpdateEvent) {
        taskId = taskStatusUpdateEvent.getProcess().getId() + "";
        oldStatus = taskStatusUpdateEvent.getOldStatus();
        newStatus = taskStatusUpdateEvent.getNewStatus();
        context = taskStatusUpdateEvent.getContext();
    }

    public TaskStatusUpdateEvent(String taskId, Status oldStatus, Status newStatus, String context) {
        this.taskId = taskId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.context = context;
    }

    public String getTaskId() {
        return taskId;
    }

    public Status getOldStatus() {
        return oldStatus;
    }

    public Status getNewStatus() {
        return newStatus;
    }

    public String getContext() {
        return context;
    }

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize object.", e);
        }
        return null;
    }

    public static TaskStatusUpdateEvent fromJson(String serialized) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(serialized, TaskStatusUpdateEvent.class);
        } catch (JsonParseException | JsonMappingException e) {
            log.error("Cannot deserialize object from json", e);
            throw e;
        }
    }

}
