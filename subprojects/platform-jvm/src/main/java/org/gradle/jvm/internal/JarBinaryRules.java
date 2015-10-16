/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.jvm.internal;

import org.gradle.api.Action;
import org.gradle.jvm.JarBinarySpec;
import org.gradle.jvm.toolchain.JavaToolChainRegistry;
import org.gradle.model.Defaults;
import org.gradle.model.RuleSource;
import org.gradle.platform.base.ComponentSpec;

import java.io.File;

@SuppressWarnings("UnusedDeclaration")
public class JarBinaryRules extends RuleSource {
    @Defaults
    void configureJarBinaries(final ComponentSpec jvmLibrary, BuildDirHolder buildDirHolder, final JavaToolChainRegistry toolChains) {
        final File binariesDir = new File(buildDirHolder.getBuildDir(), "jars");
        final File classesDir = new File(buildDirHolder.getBuildDir(), "classes");
        jvmLibrary.getBinaries().withType(JarBinarySpec.class).beforeEach(new Action<JarBinarySpec>() {
            @Override
            public void execute(JarBinarySpec jarBinary) {
                File outputDir = new File(classesDir, jarBinary.getName());
                jarBinary.setClassesDir(outputDir);
                jarBinary.setResourcesDir(outputDir);
                jarBinary.setJarFile(new File(binariesDir, String.format("%s/%s.jar", jarBinary.getName(), jarBinary.getId().getLibraryName())));
                jarBinary.setApiJarFile(new File(binariesDir, String.format("%s/%s.jar", jarBinary.getName().replace("Jar", "ApiJar"), jarBinary.getId().getLibraryName())));
                jarBinary.setToolChain(toolChains.getForPlatform(jarBinary.getTargetPlatform()));
            }
        });
    }
}
