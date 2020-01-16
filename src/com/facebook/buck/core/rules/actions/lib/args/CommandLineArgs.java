/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.buck.core.rules.actions.lib.args;

import com.facebook.buck.core.artifact.Artifact;
import com.facebook.buck.core.artifact.ArtifactFilesystem;
import com.facebook.buck.core.artifact.BoundArtifact;
import com.facebook.buck.core.artifact.OutputArtifact;
import com.facebook.buck.core.rulekey.AddToRuleKey;
import com.facebook.buck.core.rulekey.AddsToRuleKey;
import com.facebook.buck.core.sourcepath.BuildTargetSourcePath;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.util.immutables.BuckStyleImmutable;
import com.facebook.buck.core.util.immutables.BuckStyleValue;
import com.google.common.collect.ImmutableSortedMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Container for a list of objects that can be stringified into command line arguments for an action
 * that executes a program.
 *
 * <p>In the future this will also let us more efficiently concatenate command line arguments that
 * are passed around as providers, as the {@link CommandLineArgs} objects may store the immutable
 * objects more efficiently, and just construct a stream to interate over those internal
 * collections.
 */
@BuckStyleImmutable
public interface CommandLineArgs extends AddsToRuleKey, CommandLineArgsApi {

  String DEFAULT_FORMAT_STRING = "%s";

  /**
   * Simple container that holds a single argument, and a formatting string that should be run after
   * {@link #getObject} has been stringified (containing a single %s).
   */
  @BuckStyleValue
  interface ArgAndFormatString extends AddsToRuleKey {
    /** The original raw argument */
    Object getObject();

    /** The format string to apply after stringifying {@link #getObject()} */
    @AddToRuleKey
    String getPostStringificationFormatString();

    @AddToRuleKey
    default Object getObjectForRuleKey() {
      Object o = getObject();
      if (o instanceof OutputArtifact) {
        o = ((OutputArtifact) o).getArtifact();
      }
      if (o instanceof Artifact) {
        Artifact artifact = (Artifact) o;
        if (artifact.isBound()) {
          SourcePath sourcePath = artifact.asBound().getSourcePath();
          if (sourcePath instanceof BuildTargetSourcePath) {
            // Just use the name so that we don't recursively look things up.
            // Otherwise just return the original argument. There shouldn't be any other
            // types that are accepted by CommandLineArgs that also causes these types
            // of cycles (e.g. even PathSourcePath we can safely hash its contents)
            return ((BuildTargetSourcePath) sourcePath).representationForRuleKey();
          }
        }
      }
      return o;
    }
  }

  /**
   * @return Get a map of all environment variables that need to be added to execute this program.
   */
  @AddToRuleKey
  ImmutableSortedMap<String, String> getEnvironmentVariables();

  /**
   * @return Get a stream of all raw argument objects that can be stringified with something like
   *     {@link CommandLineArgStringifier#asString(ArtifactFilesystem, boolean, Object)}
   */
  Stream<ArgAndFormatString> getArgsAndFormatStrings();

  /**
   * Get the approximate number of arguments that will be returned for {@link
   * #getArgsAndFormatStrings()}
   *
   * <p>This can be handy to pre-size destination collections
   *
   * @return the approximate number of arguments. If retrieving the accurate count is efficient, a
   *     correct number is preferred. However if getting a correct number is impossible or
   *     expensive, an approximation is acceptable.
   */
  int getEstimatedArgsCount();

  /**
   * Get the arguments specifically for the rule key hasher.
   *
   * <p>This is needed because {@link #getArgsAndFormatStrings()} by default can have {@link
   * Artifact}s in it. This causes issues because we use the {@link BoundArtifact#getSourcePath()}
   * to get the rule key for a given artifact. If that artifact is the output of a build target, we
   * /then/ go look up that rule. If the argument is, say, an output artifact generated by this
   * rule, we can end up creating infinite recursion.
   *
   * <p>An example: //foo:bar has Action1 that creates SomeArtifact. //foo:bar then has Action2,
   * which holds onto SomeArtifact in its CommandLineArguments.
   *
   * <p>When we get the rule key of Action2, we get the rulekey of the {@link CommandLineArgs}
   * object that contains SomeArtifact. We get the rule key of SomeArtifact, which has //foo:bar in
   * its SourcePath We then lookup //foo:bar, and... this is where the cycle is hit.
   *
   * <p>Each action should properly hold onto inputs/outputs, so this should not realistically be a
   * problem.
   *
   * @return A stream of objects that can safely be hashed by the rule key calculator.
   */
  @AddToRuleKey
  default Stream<ArgAndFormatString> getArgsForRuleKey() {
    return getArgsAndFormatStrings();
  }

  /**
   * Add any artifacts from {@link #getArgsAndFormatStrings()} to {@code inputs} and {@code
   * outputs}, inferring based on type
   */
  void visitInputsAndOutputs(Consumer<Artifact> inputs, Consumer<Artifact> outputs);
}
