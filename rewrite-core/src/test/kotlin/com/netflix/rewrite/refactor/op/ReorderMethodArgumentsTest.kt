/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Test

abstract class ReorderMethodArgumentsTest(p: Parser): Parser by p {

    @Test
    fun refactorReorderArguments() {
        val a = """
            |package a;
            |public class A {
            |   public void foo(String s, Integer m, Integer n) {}
            |   public void foo(Integer n, Integer m, String s) {}
            |}
        """

        val b = """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo(
            |           "mystring",
            |           1,
            |           2
            |       );
            |   }
            |}
        """

        val cu = parse(b, a)
        val fixed = cu.refactor {
            cu.findMethodCalls("a.A foo(..)").forEach {
                it.args.args.firstOrNull()?.let { changeLiteral(it) { "anotherstring" } }
                reorderArguments(it, "n", "m", "s")
            }
        }.fix()

        assertRefactored(fixed, """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo(
            |           2,
            |           1,
            |           "anotherstring"
            |       );
            |   }
            |}
        """)
    }

    @Test
    fun refactorReorderArgumentsWithNoSourceAttachment() {
        val a = """
            |package a;
            |public class A {
            |   public void foo(String arg0, Integer... arg1) {}
            |   public void foo(Integer arg0, Integer arg1, String arg2) {}
            |}
        """

        val b = """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo("s", 0, 1);
            |   }
            |}
        """

        val cu = parse(b, a)
        val fixed = cu.refactor {
            cu.findMethodCalls("a.A foo(..)").forEach {
                reorderArguments(it, "n", "s").setOriginalParamNames("s", "n")
            }
        }.fix()

        assertRefactored(fixed, """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo(0, 1, "s");
            |   }
            |}
        """)
    }

    @Test
    fun refactorReorderArgumentsWhereOneOfTheOriginalArgumentsIsVararg() {
        val a = """
            |package a;
            |public class A {
            |   public void foo(String s, Integer n, Object... o) {}
            |   public void bar(String s, Object... o) {}
            |}
        """

        val b = """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo("mystring", 0, "a", "b");
            |   }
            |}
        """

        val cu = parse(b, a)
        val fixed = cu.refactor {
            cu.findMethodCalls("a.A foo(..)").forEach {
                reorderArguments(it, "s", "o", "n")
            }
        }.fix()

        assertRefactored(fixed, """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo("mystring", "a", "b", 0);
            |   }
            |}
        """)
    }

    @Test
    fun refactorReorderArgumentsWhereTheLastArgumentIsVarargAndNotPresentInInvocation() {
        val a = """
            |package a;
            |public class A {
            |   public void foo(String s, Object... o) {}
            |}
        """

        val b = """
            |import a.*;
            |public class B {
            |   public void test() {
            |       new A().foo("mystring");
            |   }
            |}
        """

        val cu = parse(b, a)
        val fixed = cu.refactor {
            cu.findMethodCalls("a.A foo(..)").forEach {
                reorderArguments(it, "o", "s")
            }
        }.fix()

        assertRefactored(fixed, """
            |import a.*;
            |public class B {
            |   public void test() {
            |       new A().foo("mystring");
            |   }
            |}
        """)
    }
}

class OracleReorderMethodArgumentsTest: ReorderMethodArgumentsTest(OracleJdkParser())