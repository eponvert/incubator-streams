/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.streams.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang.NotImplementedException;
import org.apache.streams.data.ActivityConverter;
import org.apache.streams.data.ActivityObjectConverter;
import org.apache.streams.exceptions.ActivityConversionException;
import org.apache.streams.jackson.StreamsJacksonMapper;
import org.apache.streams.pojo.json.Activity;
import org.apache.streams.pojo.json.ActivityObject;

import java.util.List;

/**
 * BaseObjectNodeActivityConverter is included by default in all
 * @see {@link ActivityConverterProcessor}
 *
 * Ensures generic ObjectNode representation of an Activity can be converted to Activity
 *
 */
public class BaseObjectNodeActivityObjectConverter implements ActivityObjectConverter<ObjectNode> {

    public static Class requiredClass = ObjectNode.class;

    private ObjectMapper mapper = StreamsJacksonMapper.getInstance();

    @Override
    public Class requiredClass() {
        return requiredClass;
    }

    @Override
    public String serializationFormat() {
        return null;
    }

    @Override
    public ObjectNode fromActivityObject(ActivityObject deserialized) throws ActivityConversionException {
        try {
            return mapper.convertValue(deserialized, ObjectNode.class);
        } catch (Exception e) {
            throw new ActivityConversionException();
        }
    }

    @Override
    public ActivityObject toActivityObject(ObjectNode serialized) throws ActivityConversionException {
        try {
            return mapper.convertValue(serialized, ActivityObject.class);
        } catch (Exception e) {
            throw new ActivityConversionException();
        }
    }

}
