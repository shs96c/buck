package com.facebook.buck.jvm.java2;

import com.facebook.buck.io.MorePaths;
import com.facebook.buck.util.Ansi;
import com.facebook.buck.util.CapturingPrintStream;
import com.facebook.buck.util.Console;
import com.facebook.buck.util.DefaultProcessExecutor;
import com.facebook.buck.util.HumanReadableException;
import com.facebook.buck.util.ProcessExecutor;
import com.facebook.buck.util.ProcessExecutorParams;
import com.facebook.buck.util.Verbosity;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExternalJavac implements JavaCompilerTool {

  @Override
  public CompilerInvocation prepareInvocation(
      Iterable<Path> classpath,
      Iterable<Path> sources,
      Path workingDirectory,
      Path output) {
    return new CommandLineJavac(workingDirectory, classpath, sources, output);
  }

  private static class CommandLineJavac implements CompilerInvocation {

    private final Iterable<Path> classpath;
    private final Iterable<Path> sources;
    private final Path output;
    private final Path workingDirectory;

    public CommandLineJavac(
        Path workingDirectory,
        Iterable<Path> classpath,
        Iterable<Path> sources,
        Path output) {
      Preconditions.checkState(
          output.isAbsolute(),
          "Output is expected to be absolute: %s",
          output);

      Preconditions.checkState(
          workingDirectory.isAbsolute(),
          "Working directory is expected to be absolute: %s",
          output);

      this.workingDirectory = workingDirectory;
      this.classpath = classpath;
      this.sources = sources;
      this.output = output;
    }

    @Override
    public Result invoke() throws IOException, InterruptedException {
      try (
          CapturingPrintStream stdout = new CapturingPrintStream();
          CapturingPrintStream stderr = new CapturingPrintStream()) {

        Console console = new Console(Verbosity.ALL, stdout, stderr, Ansi.forceTty());
        DefaultProcessExecutor executor = new DefaultProcessExecutor(console);

        ProcessExecutorParams params = ProcessExecutorParams.builder()
            .addCommand("javac")
            .addCommand("-classpath", Joiner.on(':').join(classpath))
            .addCommand("-d", workingDirectory.toString())
            .addAllCommand(buildSourceArgs(sources))
            .build();
        ProcessExecutor.Result compileResult = executor.launchAndExecute(params);
        if (compileResult.getExitCode() != 0) {
          return new FailedCompilation(compileResult.getMessageForUnexpectedResult("Compilation"));
        }
      }

      try (
          CapturingPrintStream stdout = new CapturingPrintStream();
          CapturingPrintStream stderr = new CapturingPrintStream()) {

        Console console = new Console(Verbosity.ALL, stdout, stderr, Ansi.forceTty());
        DefaultProcessExecutor executor = new DefaultProcessExecutor(console);

        ProcessExecutorParams params = ProcessExecutorParams.builder()
            .addCommand("jar")
            .addCommand("-cvf", output.toString())
            .addCommand("-C", workingDirectory.toString(), ".")
            .build();
        ProcessExecutor.Result jarResult = executor.launchAndExecute(params);
        if (jarResult.getExitCode() != 0) {
          return new FailedCompilation(jarResult.getMessageForUnexpectedResult("Building jar"));
        }

        return new SuccessfulCompilation();
      }

    }

    private Iterable<String> buildSourceArgs(Iterable<Path> sources) {
      Set<String> sourceJars = new LinkedHashSet<>(); // Maintain insertion order
      ImmutableList.Builder<String> args = ImmutableList.builder();

      for (Path source : sources) {
        Preconditions.checkState(source.isAbsolute(), "Expected source to be absolute: %s", source);

        switch (MorePaths.getFileExtension(source)) {
          case "java":
            args.add(source.toString());
            break;

          case "jar":
          case "zip":
            sourceJars.add(source.toString());
            break;

          default:
            throw new HumanReadableException("Unexpected source extension: " + source);
        }
      }

      // TODO(shs): Prevent the classpath being used to try and find sources.
      if (!sourceJars.isEmpty()) {
        args.add("-sourcepath", Joiner.on(File.pathSeparator).join(sourceJars));
      }

      return args.build();
    }
  }

  private static class SuccessfulCompilation implements CompilerInvocation.Result {

    @Override
    public boolean isSuccessful() {
      return true;
    }

    @Override
    public String getExplanation() {
      return "Compile succeeded";
    }
  }

  private static class FailedCompilation implements CompilerInvocation.Result {

    private final String reason;

    public FailedCompilation(String reason) {
      this.reason = reason;
    }

    @Override
    public boolean isSuccessful() {
      return false;
    }

    @Override
    public String getExplanation() {
      return reason;
    }
  }
}
