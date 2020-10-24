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

package com.facebook.buck.intellij.ideabuck.configurations;

import com.facebook.buck.intellij.ideabuck.endtoend.BuckTestCase;
import com.intellij.execution.configurations.ConfigurationFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.jdom.Element;
import org.junit.Assert;

public class BuckInstallConfigurationTest extends BuckTestCase {

  public void testPersistency() throws Exception {
    final ConfigurationFactory factory =
        BuckInstallConfigurationType.getInstance().getConfigurationFactories()[0];
    final BuckInstallConfiguration cfg =
        new BuckInstallConfiguration(getProject(), factory, "test serialization");
    cfg.data.targets = "//src/com/facebook/buck:test";
    cfg.data.activitySetting = "Specify Activity";
    cfg.data.activityClass = "com.facebook.katana.activity.FbMainTabActivity";
    cfg.data.additionalParams = "--num-threads 239";
    cfg.data.buckExecutablePath = "foo/bar/buck";
    final Element testElement = new Element("test_element");
    cfg.writeExternal(testElement);

    final BuckInstallConfiguration cfg2 =
        new BuckInstallConfiguration(getProject(), factory, "test serialization");
    cfg2.readExternal(testElement);
    Assert.assertEquals("//src/com/facebook/buck:test", cfg2.data.targets);
    Assert.assertEquals("Specify Activity", cfg2.data.activitySetting);
    Assert.assertEquals("com.facebook.katana.activity.FbMainTabActivity", cfg2.data.activityClass);
    Assert.assertEquals("--num-threads 239", cfg2.data.additionalParams);
    Assert.assertEquals("foo/bar/buck", cfg2.data.buckExecutablePath);
  }

  public void testAllFieldsPublic() throws Exception {
    final Field[] fs = BuckInstallConfiguration.Data.class.getDeclaredFields();
    for (Field f : fs) {
      final int modifiers = f.getModifiers();
      Assert.assertTrue(Modifier.isPublic(modifiers));
    }
  }
}