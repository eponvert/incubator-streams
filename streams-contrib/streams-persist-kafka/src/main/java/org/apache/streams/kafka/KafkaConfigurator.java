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

package org.apache.streams.kafka;

import com.typesafe.config.Config;
import org.apache.streams.config.StreamsConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sblackmon on 12/10/13.
 */
public class KafkaConfigurator {

    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaConfigurator.class);

    public static KafkaConfiguration detectConfiguration(Config kafka) {
        String brokerlist = StreamsConfigurator.config.getString("kafka.metadata.broker.list");
        String zkconnect = StreamsConfigurator.config.getString("kafka.zkconnect");
        String topic = StreamsConfigurator.config.getString("kafka.topic");
        String groupId = StreamsConfigurator.config.getString("kafka.groupid");

        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();

        kafkaConfiguration.setBrokerlist(brokerlist);
        kafkaConfiguration.setZkconnect(zkconnect);
        kafkaConfiguration.setTopic(topic);
        kafkaConfiguration.setGroupId(groupId);

        return kafkaConfiguration;
    }

}
