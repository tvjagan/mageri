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

package com.antigenomics.mageri.pipeline.input;

import com.antigenomics.mageri.preprocessing.CheckoutProcessor;
import com.antigenomics.mageri.preprocessing.DemultiplexParameters;
import com.antigenomics.mageri.preprocessing.barcode.BarcodeListParser;

import java.io.IOException;
import java.util.List;

public abstract class AdapterRule extends CheckoutRule {
    protected final String index;
    protected final List<String> barcodes;
    protected final boolean paired;
    private DemultiplexParameters demultiplexParameters = DemultiplexParameters.DEFAULT;

    public AdapterRule(String index,
                       List<String> barcodes, boolean paired) throws IOException {
        this.index = index;
        this.paired = paired;
        this.barcodes = prepareBarcodes(barcodes);
    }

    protected abstract List<String> prepareBarcodes(List<String> barcodes);

    @Override
    public CheckoutProcessor getProcessor() {
        return paired ? BarcodeListParser.generatePCheckoutProcessor(barcodes, demultiplexParameters) :
                BarcodeListParser.generateSCheckoutProcessor(barcodes, demultiplexParameters);
    }

    @Override
    public abstract boolean hasSubMultiplexing();

    @Override
    public String toString() {
        String out = "adapter_rule\n-submultiplex:" + hasSubMultiplexing() + "\n-samples:";
        for (String str : getSampleNames()) {
            out += "\n--" + str;
        }
        return out;
    }
}
