/*
 * Copyright 2012-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java;

import com.facebook.buck.io.BuildCellRelativePath;
import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.rules.AbstractBuildRuleWithDeclaredAndExtraDeps;
import com.facebook.buck.rules.AddToRuleKey;
import com.facebook.buck.rules.BinaryBuildRule;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.CommandTool;
import com.facebook.buck.rules.ExplicitBuildTargetSourcePath;
import com.facebook.buck.rules.PathSourcePath;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.Tool;
import com.facebook.buck.rules.args.SourcePathArg;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.facebook.buck.step.fs.MkdirStep;
import com.facebook.buck.step.fs.SymlinkFileStep;
import com.facebook.buck.util.PatternsMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

@BuildsAnnotationProcessor
public class JavaBinary extends AbstractBuildRuleWithDeclaredAndExtraDeps
    implements BinaryBuildRule, HasClasspathEntries {

  // We're just propagating the runtime launcher through `getExecutiable`, so don't add it to the
  // rule key.
  private final Tool javaRuntimeLauncher;

  @AddToRuleKey @Nullable private final String mainClass;

  @AddToRuleKey @Nullable private final SourcePath manifestFile;
  private final boolean mergeManifests;

  @Nullable @AddToRuleKey private final SourcePath metaInfDirectory;

  @SuppressWarnings("PMD.UnusedPrivateField")
  @AddToRuleKey
  private final ImmutableSet<Pattern> blacklist;

  private final PatternsMatcher blacklistPatternsMatcher;

  private final ImmutableSet<JavaLibrary> transitiveClasspathDeps;
  private final ImmutableSet<SourcePath> transitiveClasspaths;

  private final boolean cache;

  public JavaBinary(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleParams params,
      Tool javaRuntimeLauncher,
      @Nullable String mainClass,
      @Nullable SourcePath manifestFile,
      boolean mergeManifests,
      @Nullable Path metaInfDirectory,
      ImmutableSet<Pattern> blacklist,
      ImmutableSet<JavaLibrary> transitiveClasspathDeps,
      ImmutableSet<SourcePath> transitiveClasspaths,
      boolean cache) {
    super(buildTarget, projectFilesystem, params);
    this.javaRuntimeLauncher = javaRuntimeLauncher;
    this.mainClass = mainClass;
    this.manifestFile = manifestFile;
    this.mergeManifests = mergeManifests;
    this.metaInfDirectory =
        metaInfDirectory != null
            ? new PathSourcePath(getProjectFilesystem(), metaInfDirectory)
            : null;
    this.blacklist = blacklist;
    blacklistPatternsMatcher = new PatternsMatcher(blacklist);
    this.transitiveClasspathDeps = transitiveClasspathDeps;
    this.transitiveClasspaths = transitiveClasspaths;
    this.cache = cache;
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {

    ImmutableList.Builder<Step> commands = ImmutableList.builder();

    Path outputDirectory = getOutputDirectory();
    Step mkdir =
        MkdirStep.of(
            BuildCellRelativePath.fromCellRelativePath(
                context.getBuildCellRootPath(), getProjectFilesystem(), outputDirectory));
    commands.add(mkdir);

    ImmutableSortedSet<Path> includePaths;
    if (metaInfDirectory != null) {
      Path stagingRoot = outputDirectory.resolve("meta_inf_staging");
      Path stagingTarget = stagingRoot.resolve("META-INF");

      commands.addAll(
          MakeCleanDirectoryStep.of(
              BuildCellRelativePath.fromCellRelativePath(
                  context.getBuildCellRootPath(), getProjectFilesystem(), stagingRoot)));

      commands.add(
          SymlinkFileStep.builder()
              .setFilesystem(getProjectFilesystem())
              .setExistingFile(context.getSourcePathResolver().getAbsolutePath(metaInfDirectory))
              .setDesiredLink(stagingTarget)
              .build());

      includePaths =
          ImmutableSortedSet.<Path>naturalOrder()
              .add(stagingRoot)
              .addAll(
                  context.getSourcePathResolver().getAllAbsolutePaths(getTransitiveClasspaths()))
              .build();
    } else {
      includePaths = context.getSourcePathResolver().getAllAbsolutePaths(getTransitiveClasspaths());
    }

    Path outputFile = context.getSourcePathResolver().getRelativePath(getSourcePathToOutput());
    Path manifestPath =
        manifestFile == null ? null : context.getSourcePathResolver().getAbsolutePath(manifestFile);
    Step jar =
        new JarDirectoryStep(
            getProjectFilesystem(),
            outputFile,
            includePaths,
            mainClass,
            manifestPath,
            mergeManifests,
            false,
            entry ->
                blacklistPatternsMatcher.hasPatterns()
                    && blacklistPatternsMatcher.substringMatches(entry.getName()));
    commands.add(jar);

    buildableContext.recordArtifact(outputFile);
    return commands.build();
  }

  @Override
  public ImmutableSet<SourcePath> getTransitiveClasspaths() {
    return transitiveClasspaths;
  }

  @Override
  public ImmutableSet<JavaLibrary> getTransitiveClasspathDeps() {
    return transitiveClasspathDeps;
  }

  @Override
  public ImmutableSet<SourcePath> getImmediateClasspaths() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<SourcePath> getOutputClasspaths() {
    // A binary has no exported deps or classpath contributions of its own
    return ImmutableSet.of();
  }

  private Path getOutputDirectory() {
    return BuildTargets.getGenPath(getProjectFilesystem(), getBuildTarget(), "%s").getParent();
  }

  @Override
  public SourcePath getSourcePathToOutput() {
    return new ExplicitBuildTargetSourcePath(
        getBuildTarget(),
        Paths.get(
            String.format(
                "%s/%s.jar",
                getOutputDirectory(), getBuildTarget().getShortNameAndFlavorPostfix())));
  }

  @Override
  public Tool getExecutableCommand() {
    Preconditions.checkState(
        mainClass != null,
        "Must specify a main class for %s in order to to run it.",
        getBuildTarget());

    return new CommandTool.Builder(javaRuntimeLauncher)
        .addArg("-jar")
        .addArg(SourcePathArg.of(getSourcePathToOutput()))
        .build();
  }

  @Override
  public boolean isCacheable() {
    return cache;
  }
}
