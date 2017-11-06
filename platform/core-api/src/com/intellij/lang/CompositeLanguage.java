/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.lang;

import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CompositeLanguage extends Language {
  private final List<LanguageFilter> myFilters = ContainerUtil.createLockFreeCopyOnWriteList();

  protected CompositeLanguage(final String id) {
    super(id);
  }

  protected CompositeLanguage(final String ID, final String... mimeTypes) {
    super(ID, mimeTypes);
  }

  protected CompositeLanguage(Language baseLanguage, final String ID, final String... mimeTypes) {
    super(baseLanguage, ID, mimeTypes);
  }

  public void registerLanguageExtension(LanguageFilter filter) {
    if (!myFilters.contains(filter)) myFilters.add(filter);
  }

  public boolean unregisterLanguageExtension(LanguageFilter filter) {
    return myFilters.remove(filter);
  }

  @NotNull
  public Language[] getLanguageExtensionsForFile(@NotNull PsiFile psi) {
    final List<Language> extensions = new ArrayList<>(1);
    for (LanguageFilter filter : myFilters) {
      if (filter.isRelevantForFile(psi)) extensions.add(filter.getLanguage());
    }
    return extensions.toArray(new Language[extensions.size()]);
  }

  @NotNull
  public LanguageFilter[] getLanguageExtensions() {
    return ArrayUtil.stripTrailingNulls(myFilters.toArray(new LanguageFilter[myFilters.size()]));
  }
}
