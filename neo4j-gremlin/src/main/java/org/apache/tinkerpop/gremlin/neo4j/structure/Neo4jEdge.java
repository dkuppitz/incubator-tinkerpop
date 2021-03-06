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
package org.apache.tinkerpop.gremlin.neo4j.structure;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedEdge;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Neo4jEdge extends Neo4jElement implements Edge, WrappedEdge<Relationship> {

    public Neo4jEdge(final Relationship relationship, final Neo4jGraph graph) {
        super(relationship, graph);
    }

    @Override
    public void remove() {
        if (this.removed) throw Element.Exceptions.elementAlreadyRemoved(Edge.class, this.getBaseEdge().getId());
        this.removed = true;
        this.graph.tx().readWrite();
        try {
            ((Relationship) this.baseElement).delete();
        } catch (IllegalStateException | NotFoundException ignored) {
            // NotFoundException happens if the edge is committed
            // IllegalStateException happens if the edge is still chilling in the tx
        }
    }

    public String toString() {
        return StringFactory.edgeString(this);
    }

    @Override
    public String label() {
        this.graph.tx().readWrite();
        return this.getBaseEdge().getType().name();
    }

    @Override
    public Relationship getBaseEdge() {
        return (Relationship) this.baseElement;
    }

    @Override
    public <V> Iterator<Property<V>> properties(final String... propertyKeys) {
        return (Iterator) super.properties(propertyKeys);
    }

    @Override
    public Iterator<Vertex> vertices(final Direction direction) {
        this.graph.tx().readWrite();
        switch (direction) {
            case OUT:
                return IteratorUtils.of(new Neo4jVertex(this.getBaseEdge().getStartNode(), this.graph));
            case IN:
                return IteratorUtils.of(new Neo4jVertex(this.getBaseEdge().getEndNode(), this.graph));
            default:
                return IteratorUtils.of(new Neo4jVertex(this.getBaseEdge().getStartNode(), this.graph), new Neo4jVertex(this.getBaseEdge().getEndNode(), this.graph));
        }
    }
}