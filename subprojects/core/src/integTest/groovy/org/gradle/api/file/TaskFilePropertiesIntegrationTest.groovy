/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.file

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class TaskFilePropertiesIntegrationTest extends AbstractIntegrationSpec {
    def "creates task output file and directory locations specified using annotated properties"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @OutputFile
                File outputFile
                @OutputDirectory
                File outputDir
                @OutputFiles
                List<File> outputFiles = []
                @OutputFiles
                Map<String, File> outputFilesMap = [:]
                @OutputDirectories
                List<File> outputDirs = []
                @OutputDirectories
                Map<String, File> outputDirsMap = [:]
                
                @TaskAction
                def go() {
                    assert outputFile.parentFile.directory 
                    assert outputDir.directory 
                    outputFiles.each { assert it.parentFile.directory }
                    outputFilesMap.values().each { assert it.parentFile.directory }
                    outputDirs.each { assert it.directory }
                    outputDirsMap.values().each { assert it.directory }
                }
            }
            
            task someTask(type: TransformTask) {
                outputFile = file("build/files1/file1.txt")
                outputDir = file("build/dir1")
                outputFiles = [file("build/files2/file2.txt"), file("build/files3/file3.txt")]
                outputFilesMap = [a: file("build/files4/file4.txt"), b: file("build/files5/file5.txt")]
                outputDirs = [file("build/dir2"), file("build/dir3")]
                outputDirsMap = [a: file("build/dir4"), b: file("build/dir5")]
            }
"""

        when:
        run("someTask")

        then:
        file("build/files1").directory
        file("build/files2").directory
        file("build/files3").directory
        file("build/files4").directory
        file("build/files5").directory
        file("build/dir1").directory
        file("build/dir2").directory
        file("build/dir3").directory
        file("build/dir4").directory
        file("build/dir5").directory
    }

    def "creates task output file and directory locations specified using ad hoc properties"() {
        buildFile << """
            task someTask {
                outputs.file("build/files1/file1.txt")
                outputs.dir("build/dir1")
                outputs.files("build/files2/file2.txt", "build/files3/file3.txt")
                outputs.dirs("build/dir2", "build/dir3")
                outputs.files(a: "build/files4/file4.txt", b: "build/files5/file5.txt")
                outputs.dirs(a: "build/dir4", b: "build/dir5")
                doLast { }
            }
"""

        when:
        run("someTask")

        then:
        file("build/files1").directory
        file("build/files2").directory
        file("build/files3").directory
        file("build/files4").directory
        file("build/files5").directory
        file("build/dir1").directory
        file("build/dir2").directory
        file("build/dir3").directory
        file("build/dir4").directory
        file("build/dir5").directory
    }

    def "does not create output locations for task with no action"() {
        buildFile << """
            class TransformTask extends DefaultTask {
                @OutputFile
                File outputFile
                @OutputDirectory
                File outputDir
                @OutputFiles
                List<File> outputFiles = []
                @OutputFiles
                Map<String, File> outputFilesMap = [:]
                @OutputDirectories
                List<File> outputDirs = []
                @OutputDirectories
                Map<String, File> outputDirsMap = [:]
            }
            
            task someTask(type: TransformTask) {
                outputFile = file("build/files1/file1.txt")
                outputDir = file("build/dir1")
                outputFiles = [file("build/files2/file2.txt"), file("build/files3/file3.txt")]
                outputFilesMap = [a: file("build/files4/file4.txt"), b: file("build/files5/file5.txt")]
                outputDirs = [file("build/dir2"), file("build/dir3")]
                outputDirsMap = [a: file("build/dir4"), b: file("build/dir5")]
            }
"""

        when:
        run("someTask")

        then:
        !file("build").exists()
    }

    def "task can use Path to represent input and output locations on annotated properties"() {
        buildFile << """
            import java.nio.file.Path
            import java.nio.file.Files
            
            class TransformTask extends DefaultTask {
                @InputFile
                Path inputFile
                @InputDirectory
                Path inputDir
                @OutputFile
                Path outputFile
                @OutputDirectory
                Path outputDir
                
                @TaskAction
                def go() {
                    outputFile.text = inputFile.text
                    inputDir.toFile().listFiles().each { f -> outputDir.resolve(f.name).text = f.text }
                }
            }
            
            task transform(type: TransformTask) {
                inputFile = file("file1.txt").toPath()
                inputDir = file("dir1").toPath()
                outputFile = file("build/file1.txt").toPath()
                outputDir = file("build/dir1").toPath()
            }
"""

        when:
        file("file1.txt").text = "123"
        file("dir1/file2.txt").text = "1234"
        run("transform")

        then:
        file("build/file1.txt").text == "123"
        file("build/dir1/file2.txt").text == "1234"

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("file1.txt").text = "321"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("dir1/file3.txt").text = "new"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")
    }

    def "task can use Path to represent input and output locations on ad hoc properties"() {
        buildFile << """
            import java.nio.file.Path
            import java.nio.file.Files
            
            task transform {
                def inputFile = file("file1.txt").toPath()
                def inputDir = file("dir1").toPath()
                def outputFile = file("build/file1.txt").toPath()
                def outputDir = file("build/dir1").toPath()
                inputs.file(inputFile)
                inputs.dir(inputDir)
                outputs.file(outputFile)
                outputs.dir(outputDir)
                doLast {
                    outputFile.text = inputFile.text
                    inputDir.toFile().listFiles().each { f -> outputDir.resolve(f.name).text = f.text }
                }
            }
"""

        when:
        file("file1.txt").text = "123"
        file("dir1/file2.txt").text = "1234"
        run("transform")

        then:
        file("build/file1.txt").text == "123"
        file("build/dir1/file2.txt").text == "1234"

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("file1.txt").text = "321"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")

        when:
        run("transform")

        then:
        result.assertTasksSkipped(":transform")

        when:
        file("dir1/file3.txt").text = "new"
        run("transform")

        then:
        result.assertTasksNotSkipped(":transform")
    }
}
