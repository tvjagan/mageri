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

package com.antigenomics.mageri.core.variant.filter;

import com.antigenomics.mageri.core.variant.Variant;

public class SingletonFilter implements VariantFilter {
    public final int frequencyThreshold;

    public SingletonFilter(int frequencyThreshold) {
        this.frequencyThreshold = frequencyThreshold;
    }

    @Override
    public boolean pass(Variant variant) {
        int count = variant.getCount();
        return count != 1 ||
                count * frequencyThreshold >= variant.getDepth();
    }

    @Override
    public String getDescription() {
        return "Singleton, frequency below " + frequencyThreshold;
    }

    @Override
    public String getId() {
        return "si" + frequencyThreshold;
    }
}
