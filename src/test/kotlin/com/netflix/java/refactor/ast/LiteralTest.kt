package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.test.AstTest
import org.junit.Test
import org.junit.Assert.assertEquals

abstract class LiteralTest(parser: Parser): AstTest(parser) {
    
    @Test
    fun literalField() {
        val a = parse("""
            public class A {
                int n = 0;
            }
        """)

        val literal = a.classDecls[0].fields()[0].initializer as Tr.Literal
        assertEquals(0, literal.value)
        assertEquals(Type.Tag.Int, literal.typeTag)
        assertEquals("0", literal.print())
    }
    
    @Test
    fun transformString() {
        val a = parse("""
            public class A {
                String s = "foo ''";
            }
        """)

        val literal = a.classDecls[0].fields()[0].initializer as Tr.Literal
        assertEquals("\"foo\"", literal.transformValue<String>() { it.substringBefore(' ') })
    }

    @Test
    fun transformLong() {
        val a = parse("""
            public class A {
                Long l = 2L;
            }
        """)

        val literal = a.classDecls[0].fields()[0].initializer as Tr.Literal
        assertEquals("4L", literal.transformValue<Long>() { it * 2 })
    }
}