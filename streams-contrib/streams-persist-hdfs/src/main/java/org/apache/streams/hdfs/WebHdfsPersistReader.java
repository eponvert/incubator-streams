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

package org.apache.streams.hdfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.streams.config.ComponentConfigurator;
import org.apache.streams.config.StreamsConfiguration;
import org.apache.streams.config.StreamsConfigurator;
import org.apache.streams.converter.LineReadWriteUtil;
import org.apache.streams.core.DatumStatusCountable;
import org.apache.streams.core.DatumStatusCounter;
import org.apache.streams.core.StreamsDatum;
import org.apache.streams.core.StreamsPersistReader;
import org.apache.streams.core.StreamsResultSet;
import org.apache.streams.jackson.StreamsJacksonMapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by sblackmon on 2/28/14.
 */
public class WebHdfsPersistReader implements StreamsPersistReader, DatumStatusCountable {

    public final static String STREAMS_ID = "WebHdfsPersistReader";

    private final static Logger LOGGER = LoggerFactory.getLogger(WebHdfsPersistReader.class);

    protected final static char DELIMITER = '\t';

    protected FileSystem client;
    protected Path path;
    protected FileStatus[] status;

    protected volatile Queue<StreamsDatum> persistQueue;

    protected ObjectMapper mapper;
    protected LineReadWriteUtil lineReaderUtil;

    protected HdfsReaderConfiguration hdfsConfiguration;
    protected StreamsConfiguration streamsConfiguration;

    private ExecutorService executor;

    protected DatumStatusCounter countersTotal = new DatumStatusCounter();
    protected DatumStatusCounter countersCurrent = new DatumStatusCounter();
    private Future<?> task;

    public WebHdfsPersistReader() {
        this(new ComponentConfigurator<>(HdfsReaderConfiguration.class).detectConfiguration(StreamsConfigurator.getConfig().getConfig("hdfs")));
    }

    public WebHdfsPersistReader(HdfsReaderConfiguration hdfsConfiguration) {
        this.hdfsConfiguration = hdfsConfiguration;
    }

    public URI getURI() throws URISyntaxException {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(hdfsConfiguration.getScheme());
        uriBuilder.append("://");
        if( !Strings.isNullOrEmpty(hdfsConfiguration.getHost())) {
            uriBuilder.append(hdfsConfiguration.getHost());
            if (hdfsConfiguration.getPort() != null)
                uriBuilder.append(":" + hdfsConfiguration.getPort());
        } else {
            uriBuilder.append("/");
        }
        return new URI(uriBuilder.toString());
    }

    public boolean isConnected() 		                { return (client != null); }

    public final synchronized FileSystem getFileSystem()
    {
        // Check to see if we are connected.
        if(!isConnected())
            connectToWebHDFS();
        return this.client;
    }

    private synchronized void connectToWebHDFS()
    {
        try
        {
            LOGGER.info("User : {}", this.hdfsConfiguration.getUser());
            UserGroupInformation ugi = UserGroupInformation.createRemoteUser(this.hdfsConfiguration.getUser());
            ugi.setAuthenticationMethod(UserGroupInformation.AuthenticationMethod.SIMPLE);

            ugi.doAs(new PrivilegedExceptionAction<Void>() {
                public Void run() throws Exception {
                    Configuration conf = new Configuration();
                    conf.set(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION, "kerberos");
                    conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
                    conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
                    LOGGER.info("WebURI : {}", getURI().toString());
                    client = FileSystem.get(getURI(), conf);
                    LOGGER.info("Connected to WebHDFS");

                    /*
                    * ************************************************************************************************
                    * This code is an example of how you would work with HDFS and you weren't going over
                    * the webHDFS protocol.
                    *
                    * Smashew: 2013-10-01
                    * ************************************************************************************************
                    conf.set("fs.defaultFS", "hdfs://hadoop.mdigitallife.com:8020/user/" + userName);
                    conf.set("namenode.host","0.0.0.0");
                    conf.set("hadoop.job.ugi", userName);
                    conf.set(DFSConfigKeys.DFS_NAMENODE_USER_NAME_KEY, "runner");
                    fileSystem.createNewFile(new Path("/user/"+ userName + "/test"));
                    FileStatus[] status = fs.listStatus(new Path("/user/" + userName));
                    for(int i=0;i<status.length;i++)
                    {
                        LOGGER.info("Directory: {}", status[i].getPath());
                    }
                    */
                    return null;
                }
            });
        }
        catch (Exception e)
        {
            LOGGER.error("There was an error connecting to WebHDFS, please check your settings and try again");
            e.printStackTrace();
        }
    }

    @Override
    public String getId() {
        return STREAMS_ID;
    }

    @Override
    public void prepare(Object configurationObject) {
        LOGGER.debug("Prepare");
        lineReaderUtil = LineReadWriteUtil.getInstance(hdfsConfiguration);
        connectToWebHDFS();
        String pathString = hdfsConfiguration.getPath() + "/" + hdfsConfiguration.getReaderPath();
        LOGGER.info("Path : {}", pathString);
        path = new Path(pathString);
        try {
            if( client.isFile(path)) {
                LOGGER.info("Found File");
                FileStatus fileStatus = client.getFileStatus(path);
                status = new FileStatus[1];
                status[0] = fileStatus;
            } else if( client.isDirectory(path)){
                status = client.listStatus(path);
                List<FileStatus> statusList = Lists.newArrayList(status);
                Collections.sort(statusList);
                status = statusList.toArray(new FileStatus[0]);
                LOGGER.info("Found Directory : {} files", status.length);
            } else {
                LOGGER.error("Neither file nor directory, wtf");
            }
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        }
        streamsConfiguration = StreamsConfigurator.detectConfiguration();
        persistQueue = Queues.synchronizedQueue(new LinkedBlockingQueue<StreamsDatum>(streamsConfiguration.getBatchSize().intValue()));
        //persistQueue = Queues.synchronizedQueue(new ConcurrentLinkedQueue());
        executor = Executors.newSingleThreadExecutor();
        mapper = StreamsJacksonMapper.getInstance();
    }

    @Override
    public void cleanUp() {

    }

    @Override
    public StreamsResultSet readAll() {
        WebHdfsPersistReaderTask readerTask = new WebHdfsPersistReaderTask(this);
        Thread readerThread = new Thread(readerTask);
        readerThread.start();
        try {
            readerThread.join();
        } catch (InterruptedException e) {}
        return new StreamsResultSet(persistQueue);
    }

    @Override
    public void startStream() {
        LOGGER.debug("startStream");
        task = executor.submit(new WebHdfsPersistReaderTask(this));
    }

    @Override
    public StreamsResultSet readCurrent() {

        StreamsResultSet current;

        synchronized( WebHdfsPersistReader.class ) {
            current = new StreamsResultSet(Queues.newConcurrentLinkedQueue(persistQueue));
            current.setCounter(new DatumStatusCounter());
            current.getCounter().add(countersCurrent);
            countersTotal.add(countersCurrent);
            countersCurrent = new DatumStatusCounter();
            persistQueue.clear();
        }

        return current;
    }

    protected void write( StreamsDatum entry ) {
        boolean success;
        do {
            synchronized( WebHdfsPersistReader.class ) {
                success = persistQueue.offer(entry);
            }
            Thread.yield();
        }
        while( !success );
    }

    @Override
    public StreamsResultSet readNew(BigInteger sequence) {
        return null;
    }

    @Override
    public StreamsResultSet readRange(DateTime start, DateTime end) {
        return null;
    }

    @Override
    public boolean isRunning() {
        if( task != null)
            return !task.isDone() && !task.isCancelled();
        else return true;
    }

    @Override
    public DatumStatusCounter getDatumStatusCounter() {
        return countersTotal;
    }
}
