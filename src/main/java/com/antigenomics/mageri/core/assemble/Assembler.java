/*
 * Copyright 2014-2016 Mikhail Shugay
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

package com.antigenomics.mageri.core.assemble;

import cc.redberry.pipe.Processor;
import com.antigenomics.mageri.core.PipelineBlock;
import com.antigenomics.mageri.misc.FastqWriter;
import com.antigenomics.mageri.misc.PFastqWriterWrapper;
import com.milaboratory.core.sequence.quality.QualityFormat;
import com.milaboratory.core.sequencing.io.fastq.PFastqWriter;
import com.milaboratory.core.sequencing.io.fastq.SFastqWriter;
import com.antigenomics.mageri.core.Mig;
import com.antigenomics.mageri.core.ReadSpecific;
import com.antigenomics.mageri.misc.ProcessorResultWrapper;
import com.antigenomics.mageri.misc.SFastqWriterWrapper;
import com.antigenomics.mageri.pipeline.Speaker;
import com.milaboratory.util.CompressionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Assembler<ConsensusType extends Consensus, MigType extends Mig> extends PipelineBlock
        implements Processor<MigType, ProcessorResultWrapper<ConsensusType>>, ReadSpecific {
    protected final AtomicLong readsTotal = new AtomicLong(), readsAssembled = new AtomicLong();
    protected final AtomicInteger migsTotal = new AtomicInteger(), migsAssembled = new AtomicInteger();
    protected final List<ConsensusType> consensusList = Collections.synchronizedList(new ArrayList<ConsensusType>());
    protected boolean storeConsensuses = true, cleared = false;

    protected Assembler() {
        super("assemble");
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProcessorResultWrapper<ConsensusType> process(MigType mig) {
        ConsensusType consensus = assemble(mig);
        if (consensus == null) {
            return ProcessorResultWrapper.BLANK;
        } else {
            return new ProcessorResultWrapper<>(consensus);
        }
    }

    public abstract ConsensusType assemble(MigType mig);

    public long getReadsTotal() {
        return readsTotal.get();
    }

    public long getReadsAssembled() {
        return readsAssembled.get();
    }

    public long getMigsTotal() {
        return migsTotal.get();
    }

    public long getMigsAssembled() {
        return migsAssembled.get();
    }

    public abstract long getReadsRescuedR1();

    public abstract long getReadsRescuedR2();

    public abstract long getReadsDroppedShortR1();

    public abstract long getReadsDroppedErrorR1();

    public abstract long getReadsDroppedShortR2();

    public abstract long getReadsDroppedErrorR2();

    public void clear() {
        consensusList.clear();
        cleared = true;
    }

    @Override
    public String getHeader() {
        return "umi\treads.assembled\treads.total";
    }

    @Override
    public String getBody() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Consensus consensus : consensusList)
            stringBuilder.append(consensus.getUmi()).
                    append('\t').append(consensus.getAssembledSize()).
                    append('\t').append(consensus.getTrueSize()).
                    append('\n');

        return stringBuilder.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writePlainText(String pathPrefix) throws IOException {
        super.writePlainText(pathPrefix);

        if (cleared) {
            Speaker.INSTANCE.sout("WARNING: Calling output for Assembler that was cleared", 1);
            return;
        }

        FastqWriter writer = isPairedEnd() ?
                new PFastqWriterWrapper(
                        new PFastqWriter(pathPrefix + ".assemble.R1.fastq.gz", pathPrefix + ".assemble.R2.fastq.gz",
                                QualityFormat.Phred33, CompressionType.GZIP)
                ) :
                new SFastqWriterWrapper(
                        new SFastqWriter(pathPrefix + ".assemble.R1.fastq.gz", QualityFormat.Phred33, CompressionType.GZIP)
                );
        for (Consensus consensus : consensusList) {
            writer.write(consensus.asRead());
        }

        writer.close();
    }

    public abstract MinorCaller getMinorCaller();
}
