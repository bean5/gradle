/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.internal.instantiation;

import org.gradle.internal.exceptions.ResolutionProvider;

import java.util.Collections;
import java.util.List;

public class IllegalConstructionException extends IllegalArgumentException implements ResolutionProvider {
    private final String resolutionMessage;

    public IllegalConstructionException(String displayName, Exception cause, String resolutionMessage) {
        super(displayName, cause);
        this.resolutionMessage = resolutionMessage;
    }
    public IllegalConstructionException(String displayName, String resolutionMessage) {
        super(displayName);
        this.resolutionMessage = resolutionMessage;
    }

    @Override
    public List<String> getResolutions() {
        return Collections.singletonList(resolutionMessage);
    }
}
