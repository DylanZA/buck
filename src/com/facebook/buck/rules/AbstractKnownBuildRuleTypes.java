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

package com.facebook.buck.rules;

import com.facebook.buck.apple.AppleBinaryDescription;
import com.facebook.buck.apple.AppleBundleDescription;
import com.facebook.buck.apple.AppleConfig;
import com.facebook.buck.apple.AppleLibraryDescription;
import com.facebook.buck.apple.ApplePackageDescription;
import com.facebook.buck.apple.AppleTestDescription;
import com.facebook.buck.apple.PrebuiltAppleFrameworkDescription;
import com.facebook.buck.apple.SceneKitAssetsDescription;
import com.facebook.buck.config.BuckConfig;
import com.facebook.buck.cxx.CxxBinaryDescription;
import com.facebook.buck.cxx.CxxGenruleDescription;
import com.facebook.buck.cxx.CxxLibraryDescription;
import com.facebook.buck.cxx.CxxPrecompiledHeaderDescription;
import com.facebook.buck.cxx.CxxTestDescription;
import com.facebook.buck.cxx.PrebuiltCxxLibraryDescription;
import com.facebook.buck.cxx.PrebuiltCxxLibraryGroupDescription;
import com.facebook.buck.cxx.toolchain.CxxBuckConfig;
import com.facebook.buck.cxx.toolchain.InferBuckConfig;
import com.facebook.buck.jvm.java.JavaAnnotationProcessorDescription;
import com.facebook.buck.jvm.java.JavaBinaryDescription;
import com.facebook.buck.jvm.java.JavaBuckConfig;
import com.facebook.buck.jvm.java.JavaLibraryDescription;
import com.facebook.buck.jvm.java.JavaTestDescription;
import com.facebook.buck.jvm.java.KeystoreDescription;
import com.facebook.buck.sandbox.SandboxExecutionStrategy;
import com.facebook.buck.sandbox.SandboxExecutionStrategyFactory;
import com.facebook.buck.swift.SwiftBuckConfig;
import com.facebook.buck.swift.SwiftLibraryDescription;
import com.facebook.buck.toolchain.ToolchainProvider;
import com.facebook.buck.util.HumanReadableException;
import com.facebook.buck.util.ProcessExecutor;
import com.facebook.buck.util.immutables.BuckStyleImmutable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value;
import org.pf4j.PluginManager;

/** A registry of all the build rules types understood by Buck. */
@Value.Immutable
@BuckStyleImmutable
abstract class AbstractKnownBuildRuleTypes {

  /** @return all the underlying {@link Description}s. */
  @Value.Parameter
  abstract ImmutableList<Description<?>> getDescriptions();

  // Verify that there are no duplicate rule types being defined.
  @Value.Check
  protected void check() {
    Set<BuildRuleType> types = new HashSet<>();
    for (Description<?> description : getDescriptions()) {
      BuildRuleType type = Description.getBuildRuleType(description);
      if (!types.add(Description.getBuildRuleType(description))) {
        throw new IllegalStateException(String.format("multiple descriptions with type %s", type));
      }
    }
  }

  @Value.Lazy
  protected ImmutableMap<BuildRuleType, Description<?>> getDescriptionsByType() {
    return getDescriptions()
        .stream()
        .collect(ImmutableMap.toImmutableMap(Description::getBuildRuleType, d -> d));
  }

  @Value.Lazy
  protected ImmutableMap<String, BuildRuleType> getTypesByName() {
    return getDescriptions()
        .stream()
        .map(Description::getBuildRuleType)
        .collect(ImmutableMap.toImmutableMap(BuildRuleType::getName, t -> t));
  }

  public BuildRuleType getBuildRuleType(String named) {
    BuildRuleType type = getTypesByName().get(named);
    if (type == null) {
      throw new HumanReadableException("Unable to find build rule type: " + named);
    }
    return type;
  }

  public Description<?> getDescription(BuildRuleType buildRuleType) {
    Description<?> description = getDescriptionsByType().get(buildRuleType);
    if (description == null) {
      throw new HumanReadableException(
          "Unable to find description for build rule type: " + buildRuleType);
    }
    return description;
  }

  static KnownBuildRuleTypes createInstance(
      BuckConfig config,
      ProcessExecutor processExecutor,
      ToolchainProvider toolchainProvider,
      PluginManager pluginManager,
      SandboxExecutionStrategyFactory sandboxExecutionStrategyFactory)
      throws InterruptedException, IOException {

    SwiftBuckConfig swiftBuckConfig = new SwiftBuckConfig(config);

    CxxBuckConfig cxxBuckConfig = new CxxBuckConfig(config);

    KnownBuildRuleTypes.Builder builder = KnownBuildRuleTypes.builder();

    JavaBuckConfig javaConfig = config.getView(JavaBuckConfig.class);

    InferBuckConfig inferBuckConfig = new InferBuckConfig(config);

    CxxBinaryDescription cxxBinaryDescription =
        new CxxBinaryDescription(toolchainProvider, cxxBuckConfig, inferBuckConfig);

    CxxLibraryDescription cxxLibraryDescription =
        new CxxLibraryDescription(toolchainProvider, cxxBuckConfig, inferBuckConfig);

    SwiftLibraryDescription swiftLibraryDescription =
        new SwiftLibraryDescription(toolchainProvider, cxxBuckConfig, swiftBuckConfig);
    builder.addDescriptions(swiftLibraryDescription);

    AppleConfig appleConfig = config.getView(AppleConfig.class);

    AppleLibraryDescription appleLibraryDescription =
        new AppleLibraryDescription(
            toolchainProvider,
            cxxLibraryDescription,
            swiftLibraryDescription,
            appleConfig,
            swiftBuckConfig);
    builder.addDescriptions(appleLibraryDescription);
    PrebuiltAppleFrameworkDescription appleFrameworkDescription =
        new PrebuiltAppleFrameworkDescription(toolchainProvider, cxxBuckConfig);
    builder.addDescriptions(appleFrameworkDescription);

    AppleBinaryDescription appleBinaryDescription =
        new AppleBinaryDescription(
            toolchainProvider, cxxBinaryDescription, swiftLibraryDescription, appleConfig);
    builder.addDescriptions(appleBinaryDescription);

    SandboxExecutionStrategy sandboxExecutionStrategy =
        sandboxExecutionStrategyFactory.create(processExecutor, config);

    builder.addDescriptions(
        new ApplePackageDescription(toolchainProvider, sandboxExecutionStrategy, appleConfig));
    AppleBundleDescription appleBundleDescription =
        new AppleBundleDescription(
            toolchainProvider, appleBinaryDescription, appleLibraryDescription, appleConfig);
    builder.addDescriptions(appleBundleDescription);
    builder.addDescriptions(
        new AppleTestDescription(toolchainProvider, appleConfig, appleLibraryDescription));
    builder.addDescriptions(cxxBinaryDescription);
    builder.addDescriptions(cxxLibraryDescription);
    builder.addDescriptions(
        new CxxGenruleDescription(cxxBuckConfig, toolchainProvider, sandboxExecutionStrategy));
    builder.addDescriptions(new CxxTestDescription(toolchainProvider, cxxBuckConfig));
    builder.addDescriptions(new JavaBinaryDescription(toolchainProvider, javaConfig));
    builder.addDescriptions(new JavaAnnotationProcessorDescription());
    builder.addDescriptions(new JavaLibraryDescription(toolchainProvider, javaConfig));
    builder.addDescriptions(new JavaTestDescription(toolchainProvider, javaConfig));
    builder.addDescriptions(new KeystoreDescription());
    builder.addDescriptions(new PrebuiltCxxLibraryDescription(toolchainProvider, cxxBuckConfig));
    builder.addDescriptions(PrebuiltCxxLibraryGroupDescription.of());
    builder.addDescriptions(new CxxPrecompiledHeaderDescription());
    builder.addDescriptions(new SceneKitAssetsDescription());

    DescriptionCreationContext descriptionCreationContext =
        DescriptionCreationContext.of(config, toolchainProvider, sandboxExecutionStrategy);
    List<DescriptionProvider> descriptionProviders =
        pluginManager.getExtensions(DescriptionProvider.class);
    for (DescriptionProvider provider : descriptionProviders) {
      for (Description<?> description : provider.getDescriptions(descriptionCreationContext)) {
        builder.addDescriptions(description);
      }
    }

    return builder.build();
  }
}
