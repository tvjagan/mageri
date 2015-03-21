/*
 * Copyright 2014 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.milaboratory.oncomigec.core.correct;

import com.milaboratory.core.sequence.nucleotide.NucleotideAlphabet;
import com.milaboratory.oncomigec.core.PipelineBlock;
import com.milaboratory.oncomigec.core.consalign.entity.AlignedConsensus;
import com.milaboratory.oncomigec.core.consalign.entity.AlignerReferenceLibrary;
import com.milaboratory.oncomigec.core.genomic.Reference;
import com.milaboratory.oncomigec.core.mutations.MigecMutation;
import com.milaboratory.oncomigec.core.mutations.MigecMutationsCollection;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class Corrector extends PipelineBlock {
    private final AtomicInteger goodConsensuses = new AtomicInteger(),
            totalConsensuses = new AtomicInteger();
    private final CorrectorReferenceLibrary correctorReferenceLibrary;

    public Corrector(AlignerReferenceLibrary referenceLibraryWithStatistics) {
        this(referenceLibraryWithStatistics, CorrectorParameters.DEFAULT);
    }

    public Corrector(AlignerReferenceLibrary referenceLibraryWithStatistics,
                     CorrectorParameters parameters) {
        super("corrector");
        this.correctorReferenceLibrary = new CorrectorReferenceLibrary(referenceLibraryWithStatistics,
                parameters);
    }

    public CorrectedConsensus correct(AlignedConsensus alignedConsensus) {
        totalConsensuses.incrementAndGet();

        Set<Integer> coverageMask = new HashSet<>();

        double maxPValue = 0;
        Reference reference = alignedConsensus.getReference();

        MigecMutationsCollection mutations = alignedConsensus.getMajorMutations();
        MutationFilter mutationFilter = correctorReferenceLibrary.getMutationFilter(reference);

        if (!mutationFilter.good())
            return null; // badly covered consensus

        // Update coverage mask
        for (int k = 0; k < reference.getSequence().size(); k++)
            if (!mutationFilter.passedFilter(k))
                coverageMask.add(k);

        // Filter substitutions and indels
        for (MigecMutation mutation : mutations) {
            // Check if that substitution passes coverage-quality filter 2nd step MIGEC
            if (mutation.isSubstitution()) {
                if (mutationFilter.hasSubstitution(mutation.pos(), mutation.to())) {
                    maxPValue = Math.max(maxPValue, correctorReferenceLibrary.getPValue(reference,
                            mutation.pos(),
                            mutation.to()));
                } else {
                    mutation.filter();
                }
            } else if (!mutationFilter.hasIndel(mutation.code())) {
                mutation.filter();
            } else if (mutation.isDeletion()) {
                coverageMask.remove(mutation.pos()); // no need to mask here
            }
        }

        goodConsensuses.incrementAndGet();

        return new CorrectedConsensus(reference, mutations.getMutationCodes(), // extract mutations that were not filtered
                coverageMask, maxPValue, alignedConsensus.getMigSize(), alignedConsensus.getRanges());
    }

    public CorrectorReferenceLibrary getCorrectorReferenceLibrary() {
        return correctorReferenceLibrary;
    }

    @Override
    public String getHeader() {
        String subst = "", substP = "", substV = "";
        for (byte i = 0; i < 4; i++) {
            char bp = NucleotideAlphabet.INSTANCE.symbolFromCode(i);
            subst += "\t" + bp;
            substP += "\t" + bp + ".prob";
            substV += "\t" + bp + ".varinat.pass";
        }
        return "reference\tpos\thas.reference\tgood.coverage\tgood.quality" +
                subst + substP + substV;
    }

    @Override
    public String getBody() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Reference reference : correctorReferenceLibrary.getReferenceLibrary().getReferences()) {
            MutationFilter mutationFilter = correctorReferenceLibrary.getMutationFilter(reference);
            if (mutationFilter.updated()) {
                for (int i = 0; i < reference.getSequence().size(); i++) {
                    stringBuilder.append(reference.getFullName()).append("\t").
                            append(i).append("\t").
                            append(mutationFilter.hasReference(i) ? 1 : 0).append("\t").
                            append(mutationFilter.goodCoverage(i) ? 1 : 0).append("\t").
                            append(mutationFilter.goodQuality(i) ? 1 : 0);

                    for (byte j = 0; j < 4; j++) {
                        stringBuilder.append("\t").append(correctorReferenceLibrary.getMajorCount(reference, i, j));
                    }
                    for (byte j = 0; j < 4; j++) {
                        stringBuilder.append("\t").append(1.0 - correctorReferenceLibrary.getPValue(reference, i, j));
                    }
                    for (byte j = 0; j < 4; j++) {
                        stringBuilder.append("\t").append(mutationFilter.hasSubstitution(i, j) ? 1 : 0);
                    }

                    stringBuilder.append("\n");
                }
            }
        }
        return stringBuilder.toString();
    }
}
