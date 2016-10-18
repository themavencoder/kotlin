/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class SpecialDescriptorsFactory(
        val psiSourceManager: PsiSourceManager,
        val builtIns: KotlinBuiltIns
) {
    private val singletonFieldDescriptors = HashMap<ClassDescriptor, PropertyDescriptor>()

    fun getFieldDescriptorForEnumEntry(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor =
            singletonFieldDescriptors.getOrPut(enumEntryDescriptor) {
                createEnumEntryFieldDescriptor(enumEntryDescriptor)
            }

    fun createFileClassDescriptor(fileEntry: SourceManager.FileEntry, packageFragment: PackageFragmentDescriptor): FileClassDescriptor {
        val ktFile = psiSourceManager.getKtFile(fileEntry as PsiSourceManager.PsiFileEntry)
                   ?: throw AssertionError("Unexpected file entry: $fileEntry")
        val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(ktFile)
        val sourceElement = KotlinSourceElement(ktFile)
        return FileClassDescriptorImpl(
                fileClassInfo.fileClassFqName.shortName(), packageFragment,
                listOf(builtIns.anyType),
                sourceElement,
                Annotations.EMPTY // TODO file annotations
        )
    }

    private fun createEnumEntryFieldDescriptor(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor {
        assert(enumEntryDescriptor.kind == ClassKind.ENUM_ENTRY) { "Should be enum entry: $enumEntryDescriptor" }

        val enumClassDescriptor = enumEntryDescriptor.containingDeclaration as ClassDescriptor
        assert(enumClassDescriptor.kind == ClassKind.ENUM_CLASS) { "Should be enum class: $enumClassDescriptor"}

        return JvmPropertyDescriptorImpl.createStaticVal(
                enumEntryDescriptor.name,
                enumClassDescriptor.defaultType,
                enumClassDescriptor,
                enumEntryDescriptor.annotations,
                Modality.FINAL,
                Visibilities.PUBLIC,
                Opcodes.ACC_ENUM,
                enumEntryDescriptor.source
        )
    }

    fun getFieldDescriptorForObjectInstance(objectDescriptor: ClassDescriptor): PropertyDescriptor =
            singletonFieldDescriptors.getOrPut(objectDescriptor) {
                createObjectInstanceFieldDescriptor(objectDescriptor)
            }

    private fun createObjectInstanceFieldDescriptor(objectDescriptor: ClassDescriptor): PropertyDescriptor {
        assert(objectDescriptor.kind == ClassKind.OBJECT) { "Should be an object: $objectDescriptor" }

        val instanceFieldDescriptor = PropertyDescriptorImpl.create(
                objectDescriptor,
                Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC, false,
                Name.identifier("INSTANCE"),
                CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false
        ).initialize(objectDescriptor.defaultType)

        return instanceFieldDescriptor
    }
}