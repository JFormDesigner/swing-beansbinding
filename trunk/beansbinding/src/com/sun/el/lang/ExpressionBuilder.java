/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.el.lang;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdesktop.el.ELContext;
import org.jdesktop.el.ELException;
import org.jdesktop.el.FunctionMapper;
import org.jdesktop.el.MethodExpression;
import org.jdesktop.el.ValueExpression;
import org.jdesktop.el.VariableMapper;

import com.sun.el.MethodExpressionImpl;
import com.sun.el.MethodExpressionLiteral;
import com.sun.el.ValueExpressionImpl;
import com.sun.el.parser.AstCompositeExpression;
import com.sun.el.parser.AstDeferredExpression;
import com.sun.el.parser.AstDynamicExpression;
import com.sun.el.parser.AstFunction;
import com.sun.el.parser.AstIdentifier;
import com.sun.el.parser.AstLiteralExpression;
import com.sun.el.parser.AstValue;
import com.sun.el.parser.ELParser;
import com.sun.el.parser.Node;
import com.sun.el.parser.NodeVisitor;
import com.sun.el.parser.ParseException;
import com.sun.el.util.MessageFactory;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class ExpressionBuilder implements NodeVisitor {

    private static final int SIZE = 5000;
    private static final Map cache = new ConcurrentHashMap(SIZE);
    private static final Map cache2 = new ConcurrentHashMap(SIZE);

    private FunctionMapper fnMapper;

    private VariableMapper varMapper;

    private String expression;

    /**
     * 
     */
    public ExpressionBuilder(String expression, ELContext ctx)
            throws ELException {
        this.expression = expression;

        FunctionMapper ctxFn = ctx.getFunctionMapper();
        VariableMapper ctxVar = ctx.getVariableMapper();

        if (ctxFn != null) {
            this.fnMapper = new FunctionMapperFactory(ctxFn);
        }
        if (ctxVar != null) {
            this.varMapper = new VariableMapperFactory(ctxVar);
        }
    }

    public final static Node createNode(String expr) throws ELException {
        Node n = createNodeInternal(expr);
        return n;
    }

    private final static Node createNodeInternal(String expr)
            throws ELException {
        if (expr == null) {
            throw new ELException(MessageFactory.get("error.null"));
        }

        Node n = (Node) cache.get(expr);
        if (n == null && (n = (Node) cache2.get(expr)) == null) {
            try {
                n = (new ELParser(new StringReader(expr)))
                        .CompositeExpression();

                // validate composite expression
                if (n instanceof AstCompositeExpression) {
                    int numChildren = n.jjtGetNumChildren();
                    if (numChildren == 1) {
                        n = n.jjtGetChild(0);
                    } else {
                        Class type = null;
                        Node child = null;
                        for (int i = 0; i < numChildren; i++) {
                            child = n.jjtGetChild(i);
                            if (child instanceof AstLiteralExpression)
                                continue;
                            if (type == null)
                                type = child.getClass();
                            else {
                                if (!type.equals(child.getClass())) {
                                    throw new ELException(MessageFactory.get(
                                            "error.mixed", expr));
                                }
                            }
                        }
                    }
                }
                if (n instanceof AstDeferredExpression
                        || n instanceof AstDynamicExpression) {
                    n = n.jjtGetChild(0);
                }
                if (cache.size() > SIZE) {
                    cache2.clear();
                    cache2.putAll(cache);
                    cache.clear();
                }
                cache.put(expr, n);
            } catch (ParseException pe) {
                throw new ELException("Error Parsing: " + expr, pe);
            }
        }
        return n;
    }

    private void prepare(Node node) throws ELException {
        node.accept(this);
        if (this.fnMapper instanceof FunctionMapperFactory) {
            this.fnMapper = ((FunctionMapperFactory) this.fnMapper).create();
        }
        if (this.varMapper instanceof VariableMapperFactory) {
            this.varMapper = ((VariableMapperFactory) this.varMapper).create();
        }
    }

    private Node build() throws ELException {
        Node n = createNodeInternal(this.expression);
        this.prepare(n);
        if (n instanceof AstDeferredExpression
                || n instanceof AstDynamicExpression) {
            n = n.jjtGetChild(0);
        }
        return n;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.el.parser.NodeVisitor#visit(com.sun.el.parser.Node)
     */
    public void visit(Node node) throws ELException {
        if (node instanceof AstFunction) {

            AstFunction funcNode = (AstFunction) node;

            if (this.fnMapper == null) {
                throw new ELException(MessageFactory.get("error.fnMapper.null"));
            }
            Method m = fnMapper.resolveFunction(funcNode.getPrefix(), funcNode
                    .getLocalName());
            if (m == null) {
                throw new ELException(MessageFactory.get(
                        "error.fnMapper.method", funcNode.getOutputName()));
            }
            int pcnt = m.getParameterTypes().length;
            if (node.jjtGetNumChildren() != pcnt) {
                throw new ELException(MessageFactory.get(
                        "error.fnMapper.paramcount", funcNode.getOutputName(),
                        "" + pcnt, "" + node.jjtGetNumChildren()));
            }
        } else if (node instanceof AstIdentifier && this.varMapper != null) {
            String variable = ((AstIdentifier) node).getImage();

            // simply capture it
            this.varMapper.resolveVariable(variable);
        }
    }

    public ValueExpression createValueExpression(Class expectedType)
            throws ELException {
        Node n = this.build();
        return new ValueExpressionImpl(this.expression, n, this.fnMapper,
                this.varMapper, expectedType);
    }

    public MethodExpression createMethodExpression(Class expectedReturnType,
            Class[] expectedParamTypes) throws ELException {
        Node n = this.build();
        if (n instanceof AstValue || n instanceof AstIdentifier) {
            return new MethodExpressionImpl(expression, n,
                    this.fnMapper, this.varMapper, expectedReturnType,
                    expectedParamTypes);
        } else if (n instanceof AstLiteralExpression) {
            return new MethodExpressionLiteral(expression, expectedReturnType,
                    expectedParamTypes);
        } else {
            throw new ELException("Not a Valid Method Expression: "
                    + expression);
        }
    }
}
