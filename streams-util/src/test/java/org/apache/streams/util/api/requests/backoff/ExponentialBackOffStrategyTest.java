/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.streams.util.api.requests.backoff;

import org.apache.streams.util.api.requests.backoff.impl.ExponentialBackOffStrategy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit Tests
 */
public class ExponentialBackOffStrategyTest {

    @Test
    public void exponentialTimeBackOffStrategyTest() {
        AbstractBackOffStrategy backOff = new ExponentialBackOffStrategy(1);
        assertEquals(5000, backOff.calculateBackOffTime(1,5));
        assertEquals(25000, backOff.calculateBackOffTime(2,5));
        assertEquals(125000, backOff.calculateBackOffTime(3,5));
        assertEquals(2000, backOff.calculateBackOffTime(1,2));
        assertEquals(16000, backOff.calculateBackOffTime(4,2));
    }

}
