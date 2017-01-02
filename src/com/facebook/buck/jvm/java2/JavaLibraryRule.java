package com.facebook.buck.jvm.java2;

import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.rules.AbstractBuildRule;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

import java.nio.file.Path;

import javax.annotation.Nullable;

public class JavaLibraryRule extends AbstractBuildRule {

  private final Path output;
  private final ImmutableSortedSet<SourcePath> sources;
  private final Path classesDir;

  protected JavaLibraryRule(
      BuildRuleParams params,
      SourcePathResolver resolver,
      ImmutableSortedSet<SourcePath> sources) {
    super(params, resolver);

    BuildTarget target = getBuildTarget();

    this.sources = sources;

    this.output = BuildTargets.getGenPath(
        getProjectFilesystem(),
        target,
        String.format("%%s/lib%s.jar", target.getShortName()));

    classesDir = BuildTargets.getScratchPath(
        getProjectFilesystem(),
        target,
        String.format("%%s/lib%s.classes", target.getShortName()));
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {
    ImmutableList.Builder<Step> steps = ImmutableList.builder();

    JavaCompilerTool javac = new ExternalJavac();

    steps.add(new MakeCleanDirectoryStep(getProjectFilesystem(), output.getParent()));
    steps.add(new MakeCleanDirectoryStep(getProjectFilesystem(), classesDir));
    steps.add(new CompileToJarStep(
        javac,
        getProjectFilesystem().resolve(classesDir),
        ImmutableSortedSet.of(),
        getResolver().getAllAbsolutePaths(sources),
        getProjectFilesystem().resolve(output)));

    return steps.build();
  }

  @Nullable
  @Override
  public Path getPathToOutput() {
    return output;
  }
}
