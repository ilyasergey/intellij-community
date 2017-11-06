/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.build;

import com.intellij.build.events.FailureResult;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public class SyncViewManager extends AbstractViewManager {
  public SyncViewManager(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @Override
  public String getViewName() {
    return "Sync";
  }

  @Nullable
  @Override
  protected Icon getContentIcon() {
    return AllIcons.Actions.Refresh;
  }

  @Override
  protected void onBuildFinish(BuildDescriptor buildDescriptor) {
    BuildInfo buildInfo = (BuildInfo)buildDescriptor;
    if (buildInfo.result instanceof FailureResult) {
      boolean activate = buildInfo.activateToolWindowWhenAdded;
      myBuildContentManager.setSelectedContent(buildInfo.content, activate, activate, activate, null);
    }
  }
}