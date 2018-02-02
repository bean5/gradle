/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.nativeplatform.toolchain.internal.swift;

import org.gradle.api.Action;
import org.gradle.api.Transformer;
import org.gradle.internal.IoActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.JavaBeanDumper;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * The peculiars of the swiftc incremental compiler can be extracted from the Driver's source code:
 * https://github.com/apple/swift/tree/d139ab29681d679337245f399dd8c76d620aa1aa/lib/Driver
 * And docs:
 * https://github.com/apple/swift/blob/d139ab29681d679337245f399dd8c76d620aa1aa/docs/Driver.md
 *
 * The incremental compiler uses the timestamp of source files and the timestamp in module.swiftdeps to
 * determine which files should be considered for compilation initially.  The compiler then looks at the
 * individual object's .swiftdeps file to build a dependency graph between changed and unchanged files.
 *
 * The incremental compiler will rebuild everything when:
 * - A source file is removed
 * - A different version of swiftc is used
 * - Different compiler arguments are used
 *
 * We work around issues with timestamps by changing module.swiftdeps and setting any changed files to
 * a timestamp of 0.  swiftc then sees those source files as different from the last compilation.
 *
 * If we have any issues reading or writing the swiftdeps file, we bail out and disable incremental compilation.
 */
class SwiftDepsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftDepsHandler.class);
    static final List RESET_TIMESTAMP = Arrays.asList(0L, 0L);

    SwiftDeps parse(File moduleSwiftDeps) throws FileNotFoundException {
        return IoActions.withResource(new FileInputStream(moduleSwiftDeps), new Transformer<SwiftDeps, FileInputStream>() {
            @Override
            public SwiftDeps transform(FileInputStream fileInputStream) {
                Yaml yaml = new Yaml(new Loader(new Constructor(SwiftDeps.class)));
                return (SwiftDeps) yaml.load(fileInputStream);
            }
        });
    }

    private void adjustTimestamps(SwiftDeps swiftDeps, Collection<File> changedSources) {
        // Update any previously known files with a bogus timestamp to force a rebuild

        for (File changedSource : changedSources) {
            if (swiftDeps.inputs.containsKey(changedSource.getAbsolutePath())) {
                swiftDeps.inputs.put(changedSource.getAbsolutePath(), RESET_TIMESTAMP);
            }
        }
    }

    private void write(File moduleSwiftDeps, final SwiftDeps swiftDeps) {
        IoActions.writeTextFile(moduleSwiftDeps, new Action<BufferedWriter>() {
            @Override
            public void execute(BufferedWriter bufferedWriter) {
                JavaBeanDumper yaml = new JavaBeanDumper(false);
                yaml.dump(swiftDeps, bufferedWriter);
                if (LOGGER.isDebugEnabled()) {
                    StringWriter sw = new StringWriter();
                    yaml.dump(swiftDeps, sw);
                    LOGGER.debug(sw.toString());
                }
            }
        });
    }

    boolean adjustTimestampsFor(File moduleSwiftDeps, Collection<File> changedSources) {
        if (moduleSwiftDeps.exists() && !changedSources.isEmpty()) {
            try {
                SwiftDeps swiftDeps = parse(moduleSwiftDeps);
                adjustTimestamps(swiftDeps, changedSources);
                write(moduleSwiftDeps, swiftDeps);
            } catch (Exception e) {
                LOGGER.debug("could not update module.swiftdeps", e);
                return false;
            }
        }
        return true;
    }

    //CHECKSTYLE:OFF
    // This is used to parse a YAML file
    public static class SwiftDeps {
        private String version;
        private String options;
        private List<Long> build_time;
        private Map<String, List> inputs;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getOptions() {
            return options;
        }

        public void setOptions(String options) {
            this.options = options;
        }

        public List<Long> getBuild_time() {
            return build_time;
        }

        public void setBuild_time(List<Long> build_time) {
            this.build_time = build_time;
        }

        public Map<String, List> getInputs() {
            return inputs;
        }

        public void setInputs(Map<String, List> inputs) {
            this.inputs = inputs;
        }
    }
    //CHECKSTYLE:ON
}
