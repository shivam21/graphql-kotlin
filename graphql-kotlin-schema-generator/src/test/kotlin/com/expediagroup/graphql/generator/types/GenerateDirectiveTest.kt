/*
 * Copyright 2020 Expedia, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.expediagroup.graphql.generator.types

import com.expediagroup.graphql.annotations.GraphQLDescription
import com.expediagroup.graphql.annotations.GraphQLDirective
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.SchemaGenerator
import com.expediagroup.graphql.generator.extensions.isTrue
import com.expediagroup.graphql.getTestSchemaConfigWithMockedDirectives
import com.expediagroup.graphql.test.utils.SimpleDirective
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateDirectiveTest {

    @GraphQLDirective
    annotation class DirectiveWithString(val string: String)

    @GraphQLDirective
    annotation class DirectiveWithIgnoredArgs(
        val string: String,
        @get:GraphQLIgnore
        val ignoreMe: String
    )

    enum class Type {
        @DirectiveWithString("my string")
        @DirectiveWithClass(SimpleDirective::class)
        ONE,

        @GraphQLDescription("my description")
        TWO
    }

    @GraphQLDirective
    annotation class DirectiveWithEnum(val type: Type)

    @GraphQLDirective
    annotation class DirectiveWithClass(val kclass: KClass<*>)

    class MyClass {

        fun noAnnotation(string: String) = string

        @GraphQLDescription("test")
        fun noDirective(string: String) = string

        @SimpleDirective
        fun simpleDirective(string: String) = string

        @DirectiveWithString(string = "foo")
        fun directiveWithString(string: String) = string

        @DirectiveWithString(string = "bar")
        fun directiveWithAnotherString(string: String) = string

        @DirectiveWithEnum(type = Type.TWO)
        fun directiveWithEnum(string: String) = string

        @DirectiveWithClass(kclass = Type::class)
        fun directiveWithClass(string: String) = string

        @DirectiveWithIgnoredArgs(string = "foo", ignoreMe = "bar")
        fun directiveWithIgnoredArgs(string: String) = string
    }

    data class MyClassWithConstructorArgs(
        @SimpleDirective val noPrefix: String,
        @property:SimpleDirective val propertyPrefix: String,
        val noDirective: String
    )

    private val basicGenerator = SchemaGenerator(getTestSchemaConfigWithMockedDirectives())

    @Test
    fun `no annotation`() {
        val noAnnotation: KFunction<String> = MyClass::noAnnotation
        assertTrue(generateDirectives(basicGenerator, noAnnotation).isEmpty().isTrue())
    }

    @Test
    fun `no directive`() {
        val noDirective: KFunction<String> = MyClass::noDirective
        assertTrue(generateDirectives(basicGenerator, noDirective).isEmpty().isTrue())
    }

    @Test
    fun `has directive`() {
        val simpleDirective: KFunction<String> = MyClass::simpleDirective
        assertEquals(expected = 1, actual = generateDirectives(basicGenerator, simpleDirective).size)
    }

    @Test
    fun `has directive with string`() {
        val directiveWithString: KFunction<String> = MyClass::directiveWithString
        assertEquals(expected = 1, actual = generateDirectives(basicGenerator, directiveWithString).size)
    }

    @Test
    fun `has directive with enum`() {
        val directiveWithEnum: KFunction<String> = MyClass::directiveWithEnum
        assertEquals(expected = 1, actual = generateDirectives(basicGenerator, directiveWithEnum).size)
    }

    @Test
    fun `has directive with class`() {
        val directiveWithClass: KFunction<String> = MyClass::directiveWithClass
        assertEquals(expected = 1, actual = generateDirectives(basicGenerator, directiveWithClass).size)
    }

    @Test
    fun `directives are only added to the schema once`() {
        val initialCount = basicGenerator.directives.size
        val simpleDirective: KFunction<String> = MyClass::simpleDirective
        val firstInvocation = generateDirectives(basicGenerator, simpleDirective)
        assertEquals(1, firstInvocation.size)
        val secondInvocation = generateDirectives(basicGenerator, simpleDirective)
        assertEquals(1, secondInvocation.size)
        assertEquals(firstInvocation.first(), secondInvocation.first())
        assertEquals(initialCount + 1, basicGenerator.directives.size)
    }

    @Test
    fun `directives are valid on fields (enum values)`() {
        val field = Type::class.java.getField("ONE")

        val directives = generateFieldDirectives(basicGenerator, field)

        assertEquals(2, directives.size)
        assertEquals("directiveWithString", directives.first().name)
        assertEquals("directiveWithClass", directives.last().name)
    }

    @Test
    fun `directives are empty on an enum with no valid annotations`() {
        val field = Type::class.java.getField("TWO")

        val directives = generateFieldDirectives(basicGenerator, field)

        assertEquals(0, directives.size)
    }

    @Test
    fun `directives are created per each declaration`() {
        val initialCount = basicGenerator.directives.size
        val directiveWithString: KFunction<String> = MyClass::directiveWithString
        val directiveWithAnotherString: KFunction<String> = MyClass::directiveWithAnotherString
        val directivesOnFirstField = generateDirectives(basicGenerator, directiveWithString)
        val directivesOnSecondField = generateDirectives(basicGenerator, directiveWithAnotherString)
        assertEquals(expected = 1, actual = directivesOnFirstField.size)
        assertEquals(expected = 1, actual = directivesOnSecondField.size)

        val firstDirective = directivesOnFirstField.first()
        val seconDirective = directivesOnSecondField.first()
        assertEquals("directiveWithString", firstDirective.name)
        assertEquals("directiveWithString", seconDirective.name)
        assertEquals("foo", firstDirective.getArgument("string")?.value)
        assertEquals("bar", seconDirective.getArgument("string")?.value)

        assertEquals(initialCount + 1, basicGenerator.directives.size)
    }

    @Test
    fun `directives on constructor arguments can be used with or without annotation prefix`() {
        val noDirectiveResult = generateDirectives(basicGenerator, MyClassWithConstructorArgs::noDirective)
        assertEquals(expected = 0, actual = noDirectiveResult.size)

        val propertyPrefixResult = generateDirectives(basicGenerator, MyClassWithConstructorArgs::propertyPrefix)
        assertEquals(expected = 1, actual = propertyPrefixResult.size)
        assertEquals(expected = "simpleDirective", actual = propertyPrefixResult.first().name)

        val noPrefixResult = generateDirectives(basicGenerator, MyClassWithConstructorArgs::noPrefix, MyClassWithConstructorArgs::class)
        assertEquals(expected = 1, actual = noPrefixResult.size)
        assertEquals(expected = "simpleDirective", actual = noPrefixResult.first().name)
    }

    @Test
    fun `directives on constructor arguments only works with parent class`() {
        val noPrefixResult = generateDirectives(basicGenerator, MyClassWithConstructorArgs::noPrefix, null)
        assertEquals(expected = 0, actual = noPrefixResult.size)
    }

    @Test
    fun `exlude directive arguments @GraphQLIgnore`() {
        val directiveWithIgnoredArgs: KFunction<String> = MyClass::directiveWithIgnoredArgs
        val directives = generateDirectives(basicGenerator, directiveWithIgnoredArgs)
        assertEquals(expected = 1, actual = directives.size)
        assertEquals(expected = 1, actual = directives.first().arguments.size)
        assertEquals(expected = "string", actual = directives.first().arguments.first().name)
    }

    companion object {
        @AfterAll
        fun cleanUp(generateDirectiveTest: GenerateDirectiveTest) {
            generateDirectiveTest.basicGenerator.close()
        }
    }
}
