/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact;

import com.google.common.collect.Lists;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.internal.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CompositeArtifactSet implements ResolvedArtifactSet {
    private final List<ResolvedArtifactSet> sets;

    private CompositeArtifactSet(List<ResolvedArtifactSet> sets) {
        this.sets = sets;
    }

    public static ResolvedArtifactSet of(Collection<? extends ResolvedArtifactSet> sets) {
        List<ResolvedArtifactSet> filtered = new ArrayList<ResolvedArtifactSet>(sets.size());
        for (ResolvedArtifactSet set : sets) {
            if (set != ResolvedArtifactSet.EMPTY) {
                filtered.add(set);
            }
        }
        if (filtered.isEmpty()) {
            return EMPTY;
        }
        if (filtered.size() == 1) {
            return filtered.get(0);
        }
        return new CompositeArtifactSet(filtered);
    }

    @Override
    public Set<ResolvedArtifact> getArtifacts() {
        Set<ResolvedArtifact> allArtifacts = new LinkedHashSet<ResolvedArtifact>();
        for (ResolvedArtifactSet set : sets) {
            allArtifacts.addAll(set.getArtifacts());
        }
        return allArtifacts;
    }

    @Override
    public void collectBuildDependencies(Collection<? super TaskDependency> dest) {
        for (ResolvedArtifactSet set : sets) {
            set.collectBuildDependencies(dest);
        }
    }

    @Override
    public void visit(final ArtifactVisitor visitor) {
        final List<Pair<AttributeContainer, ResolvedArtifact>> visitedArtifacts = Lists.newArrayList();

        ArtifactVisitor deferredArtifactVisitor = new ArtifactVisitor() {
            @Override
            public void visitArtifact(AttributeContainer variant, ResolvedArtifact artifact) {
                visitedArtifacts.add(Pair.of(variant, artifact));
            }

            @Override
            public boolean includeFiles() {
                return visitor.includeFiles();
            }

            @Override
            public void visitFile(ComponentArtifactIdentifier artifactIdentifier, AttributeContainer variant, File file) {
                visitor.visitFile(artifactIdentifier, variant, file);
            }

            @Override
            public void visitFailure(Throwable failure) {
                visitor.visitFailure(failure);
            }
        };
        for (ResolvedArtifactSet set : sets) {
            set.visit(deferredArtifactVisitor);
        }

        for (Pair<AttributeContainer, ResolvedArtifact> visitedArtifact : visitedArtifacts) {
            visitor.visitArtifact(visitedArtifact.getLeft(), visitedArtifact.getRight());
        }
    }
}
