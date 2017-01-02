package com.facebook.buck.jvm.java2;

import java.io.IOException;
import java.nio.file.Path;

public interface JavaCompilerTool {
  CompilerInvocation prepareInvocation(
      Iterable<Path> classpath,
      Iterable<Path> sources,
      Path workingDirectory,
      Path output);

  interface CompilerInvocation {
    Result invoke() throws IOException, InterruptedException;

    interface Result {
      boolean isSuccessful();

      String getExplanation();
    }
  }
}
