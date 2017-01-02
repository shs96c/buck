package com.facebook.buck.jvm.java2;

import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.parser.NoSuchBuildTargetException;
import com.facebook.buck.rules.AbstractDescriptionArg;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.SourcePathRuleFinder;
import com.facebook.buck.rules.TargetGraph;
import com.google.common.collect.ImmutableSortedSet;

public class JavaLibrary2Description implements Description<JavaLibrary2Description.Arg> {

  @Override
  public Arg createUnpopulatedConstructorArg() {
    return new Arg();
  }

  @Override
  public <A extends Arg> BuildRule createBuildRule(
      TargetGraph targetGraph,
      BuildRuleParams params,
      BuildRuleResolver resolver,
      A args) throws NoSuchBuildTargetException {
    SourcePathRuleFinder finder = new SourcePathRuleFinder(resolver);
    SourcePathResolver pathResolver = new SourcePathResolver(finder);

    return new JavaLibraryRule(params, pathResolver, args.srcs);
  }

  public static class Arg extends AbstractDescriptionArg {
    public ImmutableSortedSet<SourcePath> resources = ImmutableSortedSet.of();
    public ImmutableSortedSet<SourcePath> srcs = ImmutableSortedSet.of();

    public ImmutableSortedSet<BuildTarget> providedDeps = ImmutableSortedSet.of();
    public ImmutableSortedSet<BuildTarget> exportedDeps = ImmutableSortedSet.of();
    public ImmutableSortedSet<BuildTarget> deps = ImmutableSortedSet.of();
  }
}
