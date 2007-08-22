/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package el;

import org.jdesktop.el.impl.ExpressionFactoryImpl;
import org.jdesktop.el.impl.lang.EvaluationContext;
import org.jdesktop.el.impl.lang.VariableMapperImpl;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jdesktop.el.BeanELResolver;
import org.jdesktop.el.CompositeELResolver;
import org.jdesktop.el.ELContext;
import org.jdesktop.el.ELResolver;
import org.jdesktop.el.Expression;
import org.jdesktop.el.FunctionMapper;
import org.jdesktop.el.ValueExpression;
import org.jdesktop.el.VariableMapper;
import junit.framework.*;

/**
 *
 * @author sky
 */
public class EmptyTest extends TestCase {
    public EmptyTest(String testName) {
        super(testName);
    }
    
    public void testSetListSetValue() {
        Foo source = new Foo();
        List<Foo> children = new ArrayList<Foo>();
        children.add(new Foo());
        source.setFoo(children);
        ExpressionPair pair = createExpression("${foo.stringValue2}", source);
        pair.expression.setValue(pair.context, "foo");
        assertEquals("foo", children.get(0).getStringValue2());
    }

    public void testSetListSetValue2() {
        List<Foo> children = new ArrayList<Foo>();
        children.add(new Foo());
        children.add(new Foo());
        ExpressionPair pair = createExpression("${stringValue2}", children);
        pair.expression.setValue(pair.context, "foo");
        assertEquals("foo", children.get(0).getStringValue2());
        assertEquals("foo", children.get(1).getStringValue2());
    }

    public void testIncompletePath1() {
        Foo source = new Foo();
        Expression.Result result = evaluate("${foo.bar}", source);
        assertEquals(Expression.Result.Type.INCOMPLETE_PATH, result.getType());
        assertEquals(null, result.getResult());
        assertEquals(1, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(0));
    }

    public void testIsReadOnly2() {
        Foo source = new Foo();
        List<Foo> children = new ArrayList<Foo>();
        children.add(new Foo());
        children.add(new Foo());
        ExpressionPair pair = createExpression("${foo}", children);
        assertFalse(pair.expression.isReadOnly(pair.context));
    }
    
    public void testIsReadOnly() {
        Foo source = new Foo();
        List<Foo> children = new ArrayList<Foo>();
        children.add(new Foo());
        source.setFoo(children);
        ExpressionPair pair = createExpression("${foo.stringValue}", source);
        assertTrue(pair.expression.isReadOnly(pair.context));

        pair = createExpression("${foo.stringValue2}", source);
        assertFalse(pair.expression.isReadOnly(pair.context));
    }

    public void testGetTypeList() {
        Foo source = new Foo();
        List<Foo> children = new ArrayList<Foo>();
        children.add(new Foo());
        source.setFoo(children);
        ExpressionPair pair = createExpression("${foo.stringValue}", source);
        assertEquals(String.class, pair.expression.getType(pair.context));
    }

    public void testGetType2() {
        Foo source = new Foo();
        Foo source2 = new Foo();
        source.setFoo(source2);
        ExpressionPair pair = createExpression("${foo.stringValue}", source);
        assertEquals(String.class, pair.expression.getType(pair.context));
    }

    public void testGetType() {
        Foo source = new Foo();
        ExpressionPair pair = createExpression("${stringValue}", source);
        assertEquals(String.class, pair.expression.getType(pair.context));
    }

    public void testIncompletePath2() {
        Foo source = new Foo();
        Expression.Result result = evaluate("${foo.bar.baz}", source);
        assertEquals(Expression.Result.Type.INCOMPLETE_PATH, result.getType());
        assertEquals(null, result.getResult());
        assertEquals(1, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(0));
    }

    public void testMultiValue3() {
        Foo source = new Foo();
        Foo source2 = new Foo();
        source2.setBar("source2");
        Foo source3 = new Foo();
        source3.setBar("source3");

        List<Foo> fooList = Arrays.asList(source2, source3);
        source.setFoo(fooList);
        
        Expression.Result result = evaluate("${foo.bar}", source);
        assertEquals(Expression.Result.Type.MULTI_LIST_VALUE, result.getType());
        assertEquals(Arrays.asList("source2", "source3"), result.getResult());
        assertEquals(4, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(0));
        assertEquals(new Expression.ResolvedList(fooList),
                result.getResolvedObjects().get(1));
        assertEquals(new Expression.ResolvedProperty(source2, "bar"),
                result.getResolvedObjects().get(2));
        assertEquals(new Expression.ResolvedProperty(source3, "bar"),
                result.getResolvedObjects().get(3));
    }

    public void testMultiValue2() {
        Foo source = new Foo();
        source.setFoo("X");
        Foo source2 = new Foo();
        source2.setFoo("Y");
        List<Foo> sourceList = Arrays.asList(source, source2);
        Expression.Result result = evaluate("${foo}", sourceList);
        assertEquals(Expression.Result.Type.MULTI_LIST_VALUE, result.getType());
        assertEquals(Arrays.asList("X", "Y"), result.getResult());
        assertEquals(3, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedList(sourceList),
                result.getResolvedObjects().get(0));
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(1));
        assertEquals(new Expression.ResolvedProperty(source2, "foo"),
                result.getResolvedObjects().get(2));
    }

    public void testMultiValue1() {
        Foo source = new Foo();
        source.setFoo("X");
        List<Foo> sourceList = Arrays.asList(source);
        Expression.Result result = evaluate("${foo}", sourceList);
        assertEquals(Expression.Result.Type.SINGLE_VALUE, result.getType());
        assertEquals("X", result.getResult());
        assertEquals(2, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedList(sourceList),
                result.getResolvedObjects().get(0));
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(1));
    }

    public void testSingleValue1() {
        Object source = new Foo();
        Expression.Result result = evaluate("${foo}", source);
        assertEquals(Expression.Result.Type.SINGLE_VALUE, result.getType());
        assertEquals(null, result.getResult());
        assertEquals(1, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(0));
    }
    
    public void testSingleValue2() {
        Foo source = new Foo();
        Foo source2 = new Foo();
        source.setFoo(source2);
        Expression.Result result = evaluate("${foo.bar}", source);
        assertEquals(Expression.Result.Type.SINGLE_VALUE, result.getType());
        assertEquals(null, result.getResult());
        assertEquals(2, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(0));
        assertEquals(new Expression.ResolvedProperty(source2, "bar"),
                result.getResolvedObjects().get(1));
    }

    public void testSingleValue3() {
        Foo source = new Foo();
        Foo source2 = new Foo();
        Foo source3 = new Foo();
        source.setFoo(source2);
        source2.setBar(source3);
        Expression.Result result = evaluate("${foo.bar}", source);
        assertEquals(Expression.Result.Type.SINGLE_VALUE, result.getType());
        assertEquals(source3, result.getResult());
        assertEquals(2, result.getResolvedObjects().size());
        assertEquals(new Expression.ResolvedProperty(source, "foo"),
                result.getResolvedObjects().get(0));
        assertEquals(new Expression.ResolvedProperty(source2, "bar"),
                result.getResolvedObjects().get(1));
    }
    
    private Expression.Result evaluate(String expression, Object source) {
        ExpressionPair pair = createExpression(expression, source);
        return pair.expression.getResult(pair.context);
    }
    
    private ExpressionPair createExpression(String expression, Object source) {
        ExpressionFactoryImpl factory = new ExpressionFactoryImpl();
        ELContext context = new TestContext();
        ValueExpression exp = factory.createValueExpression(
                context, expression, Object.class);
        exp.setSource(source);
        return new ExpressionPair(context, exp);
    }
    
    
    private static final class ExpressionPair {
        public final ELContext context;
        public final ValueExpression expression;
        
        ExpressionPair(ELContext context, ValueExpression expression) {
            this.context = context;
            this.expression = expression;
        }
    }


    public static final class Foo {
        private Object foo;
        private Object bar;
        private String stringValue2;
        
        public Foo() {
        }
        
        public void setFoo(Object foo) {
            this.foo = foo;
        }
        
        public Object getFoo() {
            return foo;
        }
        
        public void setBar(Object bar) {
            this.bar = bar;
        }
        
        public Object getBar() {
            return bar;
        }
        
        public String getStringValue() {
            return null;
        }

        public void setStringValue2(String value) {
            this.stringValue2 = value;
        }
        
        public String getStringValue2() {
            return stringValue2;
        }
    }
    
    private static final class BeanResolver2 extends BeanELResolver {
        public Object getValue(ELContext context, Object base, Object property) {
            System.err.printf("BR.getValue(%1$s, %2$s)\n", base, property);
            Object retValue;
            retValue = super.getValue(context, base, property);
            System.err.println("resolved value=" + retValue);
            return retValue;
        }

        public Class<?> getType(ELContext context, Object base, Object property) {
            System.err.println("getType");
            Class retValue;
            
            retValue = super.getType(context, base, property);
            System.err.println("get type, " + base + " " + property + " result=" + retValue);
            return retValue;
        }
        
    }
    

    private static final class TestContext extends ELContext {
        private final ELResolver resolver;
        private final VariableMapper mapper;
        
        TestContext() {
            CompositeELResolver compositeResolver = new CompositeELResolver();
            compositeResolver.add(new BeanResolver2());
            resolver = compositeResolver;
            mapper = new VariableMapperImpl();
        }
        
        public ELResolver getELResolver() {
            return resolver;
        }

        public FunctionMapper getFunctionMapper() {
            return new TestFunctionMapper();
        }

        public VariableMapper getVariableMapper() {
            return mapper;
        }
    }
    
    
    private static final class TestFunctionMapper extends FunctionMapper {
        public Method resolveFunction(String prefix, String localName) {
            System.err.println("resolve function, prefix=" + prefix +
                    " localName=" + localName);
            try {
                return EmptyTest.class.getMethod("testFunction", Number.class);
            } catch (SecurityException ex) {
            } catch (NoSuchMethodException ex) {
                System.err.println("nsme " + ex);
                ex.printStackTrace();
            }
            return null;
        }
    }
    
    public static String testFunction(Number value) {
        return NumberFormat.getIntegerInstance().format(value);
    }
}
