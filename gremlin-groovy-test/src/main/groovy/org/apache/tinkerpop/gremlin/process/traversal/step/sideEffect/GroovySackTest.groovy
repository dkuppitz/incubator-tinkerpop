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
package org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop.gremlin.process.computer.ComputerTestHelper
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.TraversalEngine
import org.apache.tinkerpop.gremlin.process.UseEngine
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.structure.Vertex

import static org.apache.tinkerpop.gremlin.structure.Operator.sum

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class GroovySackTest {

    @UseEngine(TraversalEngine.Type.STANDARD)
    public static class StandardTraversals extends SackTest {

        @Override
        public Traversal<Vertex, Double> get_g_V_withSackX0X_outE_sackXsum_weightX_inV_sack_sum() {
            g.V().withSack(0.0f).outE.sack(sum, 'weight').inV.sack.sum()
        }

        @Override
        public Traversal<Vertex, Float> get_g_V_withSackX0X_repeatXoutE_sackXsum_weightX_inVX_timesX2X_sack() {
            g.V.withSack(0.0f).repeat(__.outE.sack(sum, 'weight').inV).times(2).sack
        }

        @Override
        public Traversal<Vertex, Map> get_g_V_withSackXmap__map_cloneX_out_out_sackXmap_a_nameX_sack() {
            g.V.withSack { [:] } { m -> m.clone() }.out().out().sack { m, v -> m['a'] = v.name; m }.sack()
        }
    }

    @UseEngine(TraversalEngine.Type.COMPUTER)
    public static class ComputerTraversals extends SackTest {

        @Override
        public Traversal<Vertex, Double> get_g_V_withSackX0X_outE_sackXsum_weightX_inV_sack_sum() {
            ComputerTestHelper.compute("g.V().withSack(0.0f).outE.sack(sum, 'weight').inV.sack.sum()", g);
        }

        @Override
        public Traversal<Vertex, Float> get_g_V_withSackX0X_repeatXoutE_sackXsum_weightX_inVX_timesX2X_sack() {
            ComputerTestHelper.compute("g.V.withSack(0.0f).repeat(__.outE.sack(sum, 'weight').inV).times(2).sack", g)
        }

        @Override
        public Traversal<Vertex, Map> get_g_V_withSackXmap__map_cloneX_out_out_sackXmap_a_nameX_sack() {
            ComputerTestHelper.compute("g.V.withSack { [:] } { m -> m.clone() }.out().out().sack { m, v -> m['a'] = v.name; m }.sack()", g);
        }
    }
}
