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

package com.facebook.buck.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FakeProcessExecutor extends ProcessExecutor {

  private final ImmutableMap<ProcessExecutorParams, FakeProcess> processMap;
  private final Set<ProcessExecutorParams> launchedProcesses;

  public FakeProcessExecutor() {
    this(ImmutableMap.<ProcessExecutorParams, FakeProcess>of());
  }

  public FakeProcessExecutor(Map<ProcessExecutorParams, FakeProcess> processMap) {
    super(new Console(Verbosity.ALL,
        System.out,
        System.err,
        Ansi.withoutTty()));
    this.processMap = ImmutableMap.copyOf(processMap);
    this.launchedProcesses = new HashSet<>();
  }

  @Override
  public Result launchAndExecute(
      ProcessExecutorParams params,
      Set<Option> options,
      Optional<String> stdin) {
    FakeProcess fakeProcess = processMap.get(params);
    if (fakeProcess == null) {
      throw new RuntimeException(String.format("Unexpected params: %s", params));
    }
    launchedProcesses.add(params);
    try {
      String stderr = new String(ByteStreams.toByteArray(fakeProcess.getErrorStream()), UTF_8);
      String stdout = new String(ByteStreams.toByteArray(fakeProcess.getInputStream()), UTF_8);
      return new Result(fakeProcess.waitFor(), stdout, stderr);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Result execute(Process process, Set<Option> options, Optional<String> stdin) {
    return new Result(0, "", "");
  }

  public boolean isProcessLaunched(ProcessExecutorParams params) {
    return launchedProcesses.contains(params);
  }
}
