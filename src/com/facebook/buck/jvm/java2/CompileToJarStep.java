package com.facebook.buck.jvm.java2;

import static com.facebook.buck.step.StepExecutionResult.ERROR;
import static com.facebook.buck.step.StepExecutionResult.SUCCESS;

import com.facebook.buck.jvm.java2.JavaCompilerTool.CompilerInvocation;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.StepExecutionResult;
import com.google.common.collect.ImmutableSortedSet;

import java.io.IOException;
import java.nio.file.Path;

class CompileToJarStep implements Step {

  private final JavaCompilerTool compiler;
  private final ImmutableSortedSet<Path> classpath;
  private final ImmutableSortedSet<Path> sources;
  private final Path output;
  private final Path workingDirectory;

  CompileToJarStep(
      JavaCompilerTool compiler,
      Path workingDirectory,
      ImmutableSortedSet<Path> firstOrderClasspath,
      ImmutableSortedSet<Path> sources,
      Path output) {
    this.compiler = compiler;

    this.workingDirectory = workingDirectory;
    this.classpath = firstOrderClasspath;
    this.sources = sources;
    this.output = output;
  }

  @Override
  public StepExecutionResult execute(ExecutionContext context)
      throws IOException, InterruptedException {

    CompilerInvocation invocation = compiler.prepareInvocation(
        classpath,
        sources,
        workingDirectory,
        output);

    CompilerInvocation.Result result = invocation.invoke();

    if (result.isSuccessful()) {
      return SUCCESS;
    }

    context.getConsole().printBuildFailure(result.getExplanation());
    return ERROR;
  }

  @Override
  public String getShortName() {
    return "compile";
  }

  @Override
  public String getDescription(ExecutionContext context) {
    return "compile " + output;
  }
}
