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
package com.intellij.lang.jvm.actions

import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.types.JvmSubstitutor
import com.intellij.lang.jvm.types.JvmType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJvmSubstitutor
import com.intellij.psi.PsiSubstitutor

private class SimpleMethodRequest(
  override val methodName: String,
  override val modifiers: Collection<JvmModifier> = emptyList(),
  override val returnType: ExpectedTypes = emptyList(),
  override val annotations: Collection<AnnotationRequest> = emptyList(),
  override val parameters: List<ExpectedParameter> = emptyList(),
  override val targetSubstitutor: JvmSubstitutor
) : CreateMethodRequest {
  override val isValid: Boolean = true
}

fun methodRequest(project: Project, methodName: String, modifier: JvmModifier, returnType: JvmType): CreateMethodRequest {
  return SimpleMethodRequest(
    methodName = methodName,
    modifiers = listOf(modifier),
    returnType = listOf(expectedType(returnType)),
    targetSubstitutor = PsiJvmSubstitutor(project, PsiSubstitutor.EMPTY)
  )
}
