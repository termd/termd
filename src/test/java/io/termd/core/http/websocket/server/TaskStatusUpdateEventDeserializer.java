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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.termd.core.pty.Status;

import java.io.IOException;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
class TaskStatusUpdateEventDeserializer extends JsonDeserializer<TaskStatusUpdateEvent> {
  @Override
  public TaskStatusUpdateEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonNode node = jp.getCodec().readTree(jp);
    String taskId = node.get("taskId").asText();
    String oldStatus = node.get("oldStatus").asText();
    String newStatus = node.get("newStatus").asText();
    String context = node.get("context").asText();

    return new TaskStatusUpdateEvent(taskId, Status.valueOf(oldStatus), Status.valueOf(newStatus), context);
  }
}
