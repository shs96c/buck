package com.facebook.buck.jvm.java2;

import static org.testng.Assert.*;

import com.facebook.buck.testutil.Zip;
import com.facebook.buck.testutil.integration.ProjectWorkspace;
import com.facebook.buck.testutil.integration.TemporaryPaths;
import com.facebook.buck.testutil.integration.TestDataHelper;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class JavaLibrary2DescriptionIntegrationTest {

  @Rule
  public TemporaryPaths tmp = new TemporaryPaths();

  @Test
  public void shouldCompileABasicClass() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "compile",
        tmp);
    workspace.setUp();

    Path output = workspace.buildAndReturnOutput("//:lib");

    try (Zip zip = new Zip(output, /* for writing */ false)) {
      Set<String> fileNames = zip.getFileNames();

      assertTrue(fileNames.contains("com/example/project/A.class"));
      assertTrue(fileNames.contains("com/example/project/B.class"));
    }

    // Ensure that the manifest is readable using built-in java classes
    try (JarInputStream is = new JarInputStream(Files.newInputStream(output))) {
      Manifest manifest = is.getManifest();

      assertNotNull(manifest);
    }
  }
}