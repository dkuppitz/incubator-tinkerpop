/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.structure.io;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoReader;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoWriter;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * {@link GraphMigrator} takes the data in one graph and pipes it to another graph.  Uses the {@link org.apache.tinkerpop.gremlin.structure.io.gryo.GryoReader}
 * and {@link org.apache.tinkerpop.gremlin.structure.io.gryo.GryoWriter} by default.
 *
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class GraphMigrator {

    private static final GryoReader DEFAULT_GRYO_READER = GryoReader.build().create();
    private static final GryoWriter DEFAULT_GRYO_WRITER = GryoWriter.build().create();

    /**
     * Use Gryo to pipe the data from one graph to another graph.  Uses all default settings for reader/writers.
     * Refer to {@link org.apache.tinkerpop.gremlin.structure.io.gryo.GryoReader} and {@link org.apache.tinkerpop.gremlin.structure.io.gryo.GryoWriter} for those settings.  To use features like incremental
     * loading, construct the reader/writers manually and utilize
     * {@link #migrateGraph(org.apache.tinkerpop.gremlin.structure.Graph, org.apache.tinkerpop.gremlin.structure.Graph, GraphReader, GraphWriter)}
     */
    public static void migrateGraph(final Graph fromGraph, final Graph toGraph) throws IOException {
        migrateGraph(fromGraph, toGraph, DEFAULT_GRYO_READER, DEFAULT_GRYO_WRITER);
    }

    /**
     * Pipe the data from one graph to another graph.  It is important that the reader and writer utilize the
     * same format.
     *
     * @param fromGraph the graph to take data from
     * @param toGraph   the graph to take data to
     * @param reader    reads from the graph written by the writer
     * @param writer    writes the graph to be read by the reader
     * @throws java.io.IOException thrown if there is an error in steam between the two graphs
     */
    public static void migrateGraph(final Graph fromGraph, final Graph toGraph,
                                    final GraphReader reader, final GraphWriter writer) throws IOException {
        final PipedInputStream inPipe = new PipedInputStream(1024);

        final PipedOutputStream outPipe = new PipedOutputStream(inPipe) {
            @Override
            public void close() throws IOException {
                while (inPipe.available() > 0) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
                super.close();
            }
        };

        new Thread(() -> {
            try {
                writer.writeGraph(outPipe, fromGraph);
                outPipe.flush();
                outPipe.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (fromGraph.features().graph().supportsTransactions()) fromGraph.tx().rollback();
                if (toGraph.features().graph().supportsTransactions()) toGraph.tx().rollback();
            }
        }).start();

        reader.readGraph(inPipe, toGraph);
    }
}
