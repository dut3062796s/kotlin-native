/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit

private val valueTypes = ValueType.values().associate {
    it to when (it) {
        ValueType.BOOLEAN -> LLVMInt1Type()
        ValueType.BYTE -> LLVMInt8Type()
        ValueType.SHORT, ValueType.CHAR -> LLVMInt16Type()
        ValueType.INT -> LLVMInt32Type()
        ValueType.LONG -> LLVMInt64Type()
        ValueType.FLOAT -> LLVMFloatType()
        ValueType.DOUBLE -> LLVMDoubleType()

        ValueType.NATIVE_PTR, ValueType.NATIVE_POINTED, ValueType.C_POINTER -> int8TypePtr
    }!!
}

internal fun getLLVMType(type: ValueType, isStorage: Boolean = false): LLVMTypeRef =
        if (isStorage && type == ValueType.BOOLEAN) {
            LLVMInt8Type()!!
        } else {
            valueTypes[type]!!
        }

// Boolean type has different representations
// for memory layout and other cases (i8 and i1 respectively).
// TODO: better parameter name?
internal fun RuntimeAware.getLLVMType(type: KotlinType, isStorage: Boolean = false): LLVMTypeRef {
    for (valueType in valueTypes.keys) {
        if (type.isRepresentedAs(valueType)) {
            return getLLVMType(valueType, isStorage)
        }
    }

    return this.kObjHeaderPtr
}

internal fun RuntimeAware.getLLVMReturnType(type: KotlinType): LLVMTypeRef {
    return when {
        type.isUnit() -> LLVMVoidType()!!
        // TODO: stdlib have methods taking Nothing, such as kotlin.collections.EmptySet.contains().
        // KotlinBuiltIns.isNothing(type) -> LLVMVoidType()
        else -> getLLVMType(type)
    }
}

fun RuntimeAware.isObjectType(type: KotlinType) : Boolean = isObjectType(getLLVMType(type))