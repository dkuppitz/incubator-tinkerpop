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
package org.apache.tinkerpop.gremlin.process.traversal.step.filter;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.CollectingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.lambda.ConstantTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalParent;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalUtil;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class SampleGlobalStep<S> extends CollectingBarrierStep<S> implements TraversalParent {

    private Traversal.Admin<S, Number> probabilityTraversal = new ConstantTraversal<>(1.0d);
    private final int amountToSample;
    private static final Random RANDOM = new Random();

    public SampleGlobalStep(final Traversal.Admin traversal, final int amountToSample) {
        super(traversal);
        this.amountToSample = amountToSample;
    }

    @Override
    public List<Traversal.Admin<S, Number>> getLocalChildren() {
        return Collections.singletonList(this.probabilityTraversal);
    }

    @Override
    public void addLocalChild(final Traversal.Admin<?, ?> probabilityTraversal) {
        this.probabilityTraversal = this.integrateChild(probabilityTraversal);
    }

    @Override
    public String toString() {
        return TraversalHelper.makeStepString(this, this.amountToSample, this.probabilityTraversal);
    }

    @Override
    public void barrierConsumer(final TraverserSet<S> traverserSet) {
        // return the entire traverser set if the set is smaller than the amount to sample
        if (traverserSet.bulkSize() <= this.amountToSample)
            return;
        //////////////// else sample the set
        double totalWeight = 0.0d;
        for (final Traverser<S> s : traverserSet) {
            totalWeight = totalWeight + TraversalUtil.apply(s.asAdmin(), this.probabilityTraversal).doubleValue() * s.bulk();
        }
        ///////
        final TraverserSet<S> sampledSet = new TraverserSet<>();
        int runningAmountToSample = 0;
        while (runningAmountToSample < this.amountToSample) {
            boolean reSample = false;
            double runningWeight = 0.0d;
            for (final Traverser.Admin<S> s : traverserSet) {
                long sampleBulk = sampledSet.contains(s) ? sampledSet.get(s).bulk() : 0;
                if (sampleBulk < s.bulk()) {
                    final double currentWeight = TraversalUtil.apply(s, this.probabilityTraversal).doubleValue();
                    for (int i = 0; i < (s.bulk() - sampleBulk); i++) {
                        runningWeight = runningWeight + currentWeight;
                        if (RANDOM.nextDouble() <= (runningWeight / totalWeight)) {
                            final Traverser.Admin<S> split = s.asAdmin().split();
                            split.asAdmin().setBulk(1l);
                            sampledSet.add(split);
                            runningAmountToSample++;
                            totalWeight = totalWeight - currentWeight;
                            reSample = true;
                            break;
                        }
                    }
                    if (reSample || (runningAmountToSample >= this.amountToSample))
                        break;
                }
            }
        }
        traverserSet.clear();
        traverserSet.addAll(sampledSet);
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return this.getSelfAndChildRequirements(TraverserRequirement.BULK);
    }

    @Override
    public SampleGlobalStep<S> clone() {
        final SampleGlobalStep<S> clone = (SampleGlobalStep<S>) super.clone();
        clone.probabilityTraversal = clone.integrateChild(this.probabilityTraversal.clone());
        return clone;
    }
}
