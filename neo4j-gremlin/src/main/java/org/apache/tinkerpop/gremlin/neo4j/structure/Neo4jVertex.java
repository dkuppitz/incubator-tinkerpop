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

import org.apache.tinkerpop.gremlin.process.traversal.T;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;
import org.apache.tinkerpop.gremlin.util.StreamFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Neo4jVertex extends Neo4jElement implements Vertex, WrappedVertex<Node> {

    protected static final String LABEL_DELIMINATOR = "::";

    public Neo4jVertex(final Node node, final Neo4jGraph graph) {
        super(node, graph);
    }

    @Override
    public <V> VertexProperty<V> property(final String key) {
        if (this.removed) throw Element.Exceptions.elementAlreadyRemoved(Vertex.class, this.getBaseVertex().getId());
        this.graph.tx().readWrite();
        if (!this.graph.supportsMultiProperties) {
            return existsInNeo4j(key) ? new Neo4jVertexProperty<V>(this, key, (V) this.getBaseVertex().getProperty(key)) : VertexProperty.<V>empty();
        } else {
            if (existsInNeo4j(key)) {
                if (this.getBaseVertex().getProperty(key).equals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN)) {
                    if (this.getBaseVertex().getDegree(DynamicRelationshipType.withName(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat(key)), org.neo4j.graphdb.Direction.OUTGOING) > 1)
                        throw Vertex.Exceptions.multiplePropertiesExistForProvidedKey(key);
                    else
                        return new Neo4jVertexProperty<>(this, this.getBaseVertex().getRelationships(org.neo4j.graphdb.Direction.OUTGOING, DynamicRelationshipType.withName(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat(key))).iterator().next().getEndNode());
                } else {
                    return new Neo4jVertexProperty<>(this, key, (V) this.getBaseVertex().getProperty(key));
                }
            } else
                return VertexProperty.<V>empty();
        }
    }

    @Override
    public <V> VertexProperty<V> property(final String key, final V value) {
        return this.property(VertexProperty.Cardinality.single, key, value);
    }

    @Override
    public <V> VertexProperty<V> property(final VertexProperty.Cardinality cardinality, final String key, final V value, final Object... keyValues) {
        if (this.removed) throw Element.Exceptions.elementAlreadyRemoved(Vertex.class, this.getBaseVertex().getId());
        ElementHelper.validateProperty(key, value);
        if (ElementHelper.getIdValue(keyValues).isPresent())
            throw VertexProperty.Exceptions.userSuppliedIdsNotSupported();
        this.graph.tx().readWrite();
        try {
            if (!this.graph.supportsMultiProperties) {
                this.getBaseVertex().setProperty(key, value);
                return new Neo4jVertexProperty<>(this, key, value);
            } else {
                final Optional<VertexProperty<V>> optionalVertexProperty = ElementHelper.stageVertexProperty(this, cardinality, key, value, keyValues);
                if (optionalVertexProperty.isPresent()) return optionalVertexProperty.get();

                final String prefixedKey = Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat(key);
                if (this.getBaseVertex().hasProperty(key)) {
                    if (this.getBaseVertex().getProperty(key).equals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN)) {
                        final Node node = this.graph.getBaseGraph().createNode(Neo4jVertexProperty.VERTEX_PROPERTY_LABEL, DynamicLabel.label(key));
                        node.setProperty(T.key.getAccessor(), key);
                        node.setProperty(T.value.getAccessor(), value);
                        this.getBaseVertex().createRelationshipTo(node, DynamicRelationshipType.withName(prefixedKey));
                        final Neo4jVertexProperty<V> property = new Neo4jVertexProperty<>(this, node);
                        ElementHelper.attachProperties(property, keyValues); // TODO: make this inlined
                        return property;
                    } else {
                        Node node = this.graph.getBaseGraph().createNode(Neo4jVertexProperty.VERTEX_PROPERTY_LABEL, DynamicLabel.label(key));
                        node.setProperty(T.key.getAccessor(), key);
                        node.setProperty(T.value.getAccessor(), this.getBaseVertex().removeProperty(key));
                        this.getBaseVertex().createRelationshipTo(node, DynamicRelationshipType.withName(prefixedKey));
                        this.getBaseVertex().setProperty(key, Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN);
                        node = this.graph.getBaseGraph().createNode(Neo4jVertexProperty.VERTEX_PROPERTY_LABEL, DynamicLabel.label(key));
                        node.setProperty(T.key.getAccessor(), key);
                        node.setProperty(T.value.getAccessor(), value);
                        this.getBaseVertex().createRelationshipTo(node, DynamicRelationshipType.withName(prefixedKey));
                        final Neo4jVertexProperty<V> property = new Neo4jVertexProperty<>(this, node);
                        ElementHelper.attachProperties(property, keyValues); // TODO: make this inlined
                        return property;
                    }
                } else {
                    this.getBaseVertex().setProperty(key, value);
                    final Neo4jVertexProperty<V> property = new Neo4jVertexProperty<>(this, key, value);
                    ElementHelper.attachProperties(property, keyValues); // TODO: make this inlined
                    return property;
                }
            }
        } catch (IllegalArgumentException iae) {
            throw Property.Exceptions.dataTypeOfPropertyValueNotSupported(value);
        }
    }

    @Override
    public void remove() {
        if (this.removed) throw Element.Exceptions.elementAlreadyRemoved(Vertex.class, this.getBaseVertex().getId());
        this.removed = true;
        this.graph.tx().readWrite();
        try {
            final Node node = this.getBaseVertex();
            for (final Relationship relationship : node.getRelationships(org.neo4j.graphdb.Direction.BOTH)) {
                final Node otherNode = relationship.getOtherNode(node);
                if (otherNode.hasLabel(Neo4jVertexProperty.VERTEX_PROPERTY_LABEL)) {
                    otherNode.getRelationships().forEach(Relationship::delete);
                    otherNode.delete(); // meta property node
                } else
                    relationship.delete();
            }
            node.delete();
        } catch (final NotFoundException ignored) {
            // this one happens if the vertex is committed
        } catch (final IllegalStateException ignored) {
            // this one happens if the vertex is still chilling in the tx
        }
    }

    @Override
    public Edge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
        if (null == inVertex) throw Graph.Exceptions.argumentCanNotBeNull("vertex");
        if (this.removed) throw Element.Exceptions.elementAlreadyRemoved(Vertex.class, this.getBaseVertex().getId());
        ElementHelper.validateLabel(label);
        ElementHelper.legalPropertyKeyValueArray(keyValues);
        if (ElementHelper.getIdValue(keyValues).isPresent())
            throw Edge.Exceptions.userSuppliedIdsNotSupported();

        this.graph.tx().readWrite();
        final Node node = (Node) this.baseElement;
        final Neo4jEdge edge = new Neo4jEdge(node.createRelationshipTo(((Neo4jVertex) inVertex).getBaseVertex(),
                DynamicRelationshipType.withName(label)), this.graph);
        ElementHelper.attachProperties(edge, keyValues);
        return edge;
    }

    @Override
    public Node getBaseVertex() {
        return (Node) this.baseElement;
    }

    @Override
    public String label() {
        this.graph.tx().readWrite();
        return String.join(LABEL_DELIMINATOR, this.labels());
    }

    /////////////// Neo4jVertex Specific Methods for Multi-Label Support ///////////////
    public Set<String> labels() {
        this.graph.tx().readWrite();
        final Set<String> labels = new TreeSet<>();
        final Iterator<String> itty = IteratorUtils.map(this.getBaseVertex().getLabels().iterator(), Label::name);
        while (itty.hasNext()) {
            labels.add(itty.next());
        }
        return Collections.unmodifiableSet(labels);
    }

    public void addLabel(final String label) {
        this.graph.tx().readWrite();
        this.getBaseVertex().addLabel(DynamicLabel.label(label));
    }

    public void removeLabel(final String label) {
        this.graph.tx().readWrite();
        this.getBaseVertex().removeLabel(DynamicLabel.label(label));
    }
    //////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return StringFactory.vertexString(this);
    }

    @Override
    public Iterator<Vertex> vertices(final Direction direction, final String... edgeLabels) {
        this.graph.tx().readWrite();
        return new Iterator<Vertex>() {
            final Iterator<Relationship> relationshipIterator = IteratorUtils.filter(0 == edgeLabels.length ?
                    getBaseVertex().getRelationships(Neo4jHelper.mapDirection(direction)).iterator() :
                    getBaseVertex().getRelationships(Neo4jHelper.mapDirection(direction), Neo4jHelper.mapEdgeLabels(edgeLabels)).iterator(), r -> !r.getType().name().startsWith(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX));

            @Override
            public boolean hasNext() {
                return this.relationshipIterator.hasNext();
            }

            @Override
            public Neo4jVertex next() {
                return new Neo4jVertex(this.relationshipIterator.next().getOtherNode(getBaseVertex()), graph);
            }
        };
    }

    @Override
    public Iterator<Edge> edges(final Direction direction, final String... edgeLabels) {
        this.graph.tx().readWrite();
        return new Iterator<Edge>() {
            final Iterator<Relationship> relationshipIterator = IteratorUtils.filter(0 == edgeLabels.length ?
                    getBaseVertex().getRelationships(Neo4jHelper.mapDirection(direction)).iterator() :
                    getBaseVertex().getRelationships(Neo4jHelper.mapDirection(direction), Neo4jHelper.mapEdgeLabels(edgeLabels)).iterator(), r -> !r.getType().name().startsWith(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX));

            @Override
            public boolean hasNext() {
                return this.relationshipIterator.hasNext();
            }

            @Override
            public Neo4jEdge next() {
                return new Neo4jEdge(this.relationshipIterator.next(), graph);
            }
        };
    }

    @Override
    public <V> Iterator<VertexProperty<V>> properties(final String... propertyKeys) {
        this.graph.tx().readWrite();
        return StreamFactory.stream(getBaseVertex().getPropertyKeys())
                .filter(key -> ElementHelper.keyExists(key, propertyKeys))
                .flatMap(key -> {
                    if (getBaseVertex().getProperty(key).equals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN))
                        return StreamFactory.stream(getBaseVertex().getRelationships(org.neo4j.graphdb.Direction.OUTGOING, DynamicRelationshipType.withName(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat(key))))
                                .map(relationship -> (VertexProperty<V>) new Neo4jVertexProperty(Neo4jVertex.this, relationship.getEndNode()));
                    else
                        return Stream.of(new Neo4jVertexProperty<>(Neo4jVertex.this, key, (V) this.getBaseVertex().getProperty(key)));
                }).iterator();
    }

    private boolean existsInNeo4j(final String key) {
        try {
            return this.getBaseVertex().hasProperty(key);
        } catch (IllegalStateException | NotFoundException ex) {
            // if vertex is removed before/after transaction close
            throw Element.Exceptions.elementAlreadyRemoved(Vertex.class, this.id());
        }
    }
}
