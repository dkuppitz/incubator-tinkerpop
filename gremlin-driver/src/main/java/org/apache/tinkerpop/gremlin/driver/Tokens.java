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
package org.apache.tinkerpop.gremlin.driver;

import java.util.Arrays;
import java.util.List;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Tokens {
    public static final String OPS_SHOW = "show";
    public static final String OPS_EVAL = "eval";
    public static final String OPS_IMPORT = "import";
    public static final String OPS_INVALID = "invalid";
    public static final String OPS_RESET = "reset";
    public static final String OPS_USE = "use";
    public static final String OPS_VERSION = "version";

    public static final String ARGS_BINDINGS = "bindings";
    public static final String ARGS_COORDINATES = "coordinates";
    public static final String ARGS_GRAPH_NAME = "graphName";
    public static final String ARGS_GREMLIN = "gremlin";
    public static final String ARGS_IMPORTS = "imports";
    public static final String ARGS_INFO_TYPE = "infoType";
    public static final String ARGS_LANGUAGE = "language";
    public static final String ARGS_BATCH_SIZE = "batchSize";
    public static final String ARGS_SESSION = "session";

    public static final String ARGS_COORDINATES_GROUP = "group";
    public static final String ARGS_COORDINATES_ARTIFACT = "artifact";
    public static final String ARGS_COORDINATES_VERSION = "version";

    public static final String ARGS_INFO_TYPE_DEPDENENCIES = "dependencies";
    public static final String ARGS_INFO_TYPE_IMPORTS = "imports";

    public static final List<String> INFO_TYPES = Arrays.asList(ARGS_INFO_TYPE_DEPDENENCIES,
            ARGS_INFO_TYPE_IMPORTS);
}
