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

package org.apache.streams.elasticsearch.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import org.apache.commons.lang.SerializationUtils;
import org.apache.streams.config.ComponentConfigurator;
import org.apache.streams.config.StreamsConfiguration;
import org.apache.streams.config.StreamsConfigurator;
import org.apache.streams.core.StreamsDatum;
import org.apache.streams.elasticsearch.ElasticsearchClientManager;
import org.apache.streams.elasticsearch.ElasticsearchReaderConfiguration;
import org.apache.streams.elasticsearch.ElasticsearchWriterConfiguration;
import org.apache.streams.elasticsearch.processor.DatumFromMetadataProcessor;
import org.elasticsearch.client.Client;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by sblackmon on 10/20/14.
 */
public class DatumFromMetadataProcessorIT {

    private ElasticsearchReaderConfiguration testConfiguration;
    protected Client testClient;

    @Test
    public void testSerializability() {
        DatumFromMetadataProcessor processor = new DatumFromMetadataProcessor(testConfiguration);

        DatumFromMetadataProcessor clone = (DatumFromMetadataProcessor) SerializationUtils.clone(processor);
    }

    @Before
    public void prepareTest() throws Exception {

        Config reference  = ConfigFactory.load();
        File conf_file = new File("target/test-classes/DatumFromMetadataProcessorIT.conf");
        assert(conf_file.exists());
        Config testResourceConfig  = ConfigFactory.parseFileAnySyntax(conf_file, ConfigParseOptions.defaults().setAllowMissing(false));
        Properties es_properties  = new Properties();
        InputStream es_stream  = new FileInputStream("elasticsearch.properties");
        es_properties.load(es_stream);
        Config esProps  = ConfigFactory.parseProperties(es_properties);
        Config typesafe  = testResourceConfig.withFallback(esProps).withFallback(reference).resolve();
        StreamsConfiguration streams  = StreamsConfigurator.detectConfiguration(typesafe);
        testConfiguration = new ComponentConfigurator<>(ElasticsearchReaderConfiguration.class).detectConfiguration(typesafe, "elasticsearch");
        testClient = new ElasticsearchClientManager(testConfiguration).getClient();

    }

    @Test
    public void testDatumFromMetadataProcessor() {

        Map<String, Object> metadata = Maps.newHashMap();

        metadata.put("index", testConfiguration.getIndexes().get(0));
        metadata.put("type", testConfiguration.getTypes().get(0));
        metadata.put("id", "post");

        DatumFromMetadataProcessor processor = new DatumFromMetadataProcessor(testConfiguration);

        StreamsDatum testInput = new StreamsDatum(null);

        testInput.setMetadata(metadata);

        Assert.assertNull(testInput.document);

        processor.prepare(null);

        StreamsDatum testOutput = processor.process(testInput).get(0);

        processor.cleanUp();

        Assert.assertNotNull(testOutput.document);

    }
}
