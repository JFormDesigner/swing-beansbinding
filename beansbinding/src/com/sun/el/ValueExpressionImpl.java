/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.el;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.Expression;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.sun.el.lang.ELSupport;
import com.sun.el.lang.EvaluationContext;
import com.sun.el.lang.ExpressionBuilder;
import com.sun.el.parser.AstLiteralExpression;
import com.sun.el.parser.Node;
import com.sun.el.util.ReflectionUtil;

/**
 * An <code>Expression</code> that can get or set a value.
 * 
 * <p>
 * In previous incarnations of this API, expressions could only be read.
 * <code>ValueExpression</code> objects can now be used both to retrieve a
 * value and to set a value. Expressions that can have a value set on them are
 * referred to as l-value expressions. Those that cannot are referred to as
 * r-value expressions. Not all r-value expressions can be used as l-value
 * expressions (e.g. <code>"${1+1}"</code> or
 * <code>"${firstName} ${lastName}"</code>). See the EL Specification for
 * details. Expressions that cannot be used as l-values must always return
 * <code>true</code> from <code>isReadOnly()</code>.
 * </p>
 * 
 * <p>
 * <code>The {@link ExpressionFactory#createValueExpression} method
 * can be used to parse an expression string and return a concrete instance
 * of <code>ValueExpression</code> that encapsulates the parsed expression.
 * The {@link FunctionMapper} is used at parse time, not evaluation time, 
 * so one is not needed to evaluate an expression using this class.  
 * However, the {@link ELContext} is needed at evaluation time.</p>
 *
 * <p>The {@link #getValue}, {@link #setValue}, {@link #isReadOnly} and
 * {@link #getType} methods will evaluate the expression each time they are
 * called. The {@link ELResolver} in the <code>ELContext</code> is used to 
 * resolve the top-level variables and to determine the behavior of the
 * <code>.</code> and <code>[]</code> operators. For any of the four methods,
 * the {@link ELResolver#getValue} method is used to resolve all properties 
 * up to but excluding the last one. This provides the <code>base</code> 
 * object. At the last resolution, the <code>ValueExpression</code> will 
 * call the corresponding {@link ELResolver#getValue}, 
 * {@link ELResolver#setValue}, {@link ELResolver#isReadOnly} or 
 * {@link ELResolver#getType} method, depending on which was called on 
 * the <code>ValueExpression</code>.
 * </p>
 *
 * <p>See the notes about comparison, serialization and immutability in 
 * the {@link Expression} javadocs.
 *
 * @see javax.el.ELResolver
 * @see javax.el.Expression
 * @see javax.el.ExpressionFactory
 * @see javax.el.ValueExpression
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class ValueExpressionImpl extends ValueExpression implements
        Externalizable {

    private Class expectedType;

    private String expr;

    private FunctionMapper fnMapper;

    private VariableMapper varMapper;

    private transient Node node;

    public ValueExpressionImpl() {

    }

    /**
     * 
     */
    public ValueExpressionImpl(String expr, Node node, FunctionMapper fnMapper,
            VariableMapper varMapper, Class expectedType) {
        this.expr = expr;
        this.node = node;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
        this.expectedType = expectedType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return (obj instanceof ValueExpressionImpl && obj.hashCode() == this
                .hashCode());
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#getExpectedType()
     */
    public Class getExpectedType() {
        return this.expectedType;
    }

    /**
     * Returns the type the result of the expression will be coerced to after
     * evaluation.
     * 
     * @return the <code>expectedType</code> passed to the
     *         <code>ExpressionFactory.createValueExpression</code> method
     *         that created this <code>ValueExpression</code>.
     * 
     * @see javax.el.Expression#getExpressionString()
     */
    public String getExpressionString() {
        return this.expr;
    }

    /**
     * @return
     * @throws ELException
     */
    private Node getNode() throws ELException {
        if (this.node == null) {
            this.node = ExpressionBuilder.createNode(this.expr);
        }
        return this.node;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#getType(javax.el.ELContext)
     */
    public Class getType(ELContext context) throws PropertyNotFoundException,
            ELException {
        EvaluationContext ctx = new EvaluationContext(context, this.fnMapper,
                this.varMapper, this);
        return this.getNode().getType(ctx);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#getValue(javax.el.ELContext)
     */
    public Object getValue(ELContext context) throws PropertyNotFoundException,
            ELException {
        EvaluationContext ctx = new EvaluationContext(context, this.fnMapper,
                this.varMapper, this);
        Object value = this.getNode().getValue(ctx);
        if (this.expectedType != null) {
            return ELSupport.coerceToType(value, this.expectedType);
        }
        return value;
    }
    
    public Result getResult(ELContext context) throws PropertyNotFoundException, 
            ELException {
        EvaluationContext ctx = new EvaluationContext(context, this.fnMapper,
                this.varMapper, this, true);
        Node node = this.getNode();
        Object value = coerce(node.getValue(ctx));
        List<Expression.ResolvedObject> resolvedObjects = ctx.getResolvedObjects();
        if (value != ELContext.INCOMPLETE_PATH_RESULT) {
            List resolvedList = null;
            // PENDING: make this a property
            int listThreshold = 10;
            for (Expression.ResolvedObject resolved : resolvedObjects) {
                if (resolved instanceof ResolvedList) {
                    if (resolvedList == null) {
                        resolvedList = ((ResolvedList)resolved).getSource();
                    } else if (resolvedList != ((ResolvedList)resolved).getSource()) {
                        // Too many lists, bale.
                        throw new ELException(
                                "Expression consists of multiple lists, can only resolve a single list");
                    }
                }
            }
            if (resolvedList != null && resolvedList.size() != 1) {
                // There's a List, need to repeatedly evaluate the expression to get
                // the values in the List
                int listSize = resolvedList.size();
                if (listSize < listThreshold) {
                    List<Object> values = new ArrayList<Object>(listSize);
                    values.add(value);
                    for (int i = 1; i < listSize; i++) {
                        ctx.reset();
                        ctx.setResolvingListIndex(i);
                        value = coerce(node.getValue(ctx));
                        if (value == ELContext.INCOMPLETE_PATH_RESULT) {
                            // Incomplete path for one of the elements, bale.
                            break;
                        } else {
                            values.add(value);
                        }
                    }
                    resolvedObjects = ctx.getResolvedObjects();
                    if (value != ELContext.INCOMPLETE_PATH_RESULT) {
                        return new Result(Result.Type.MULTI_LIST_VALUE, values,
                                resolvedObjects);
                    }
                    return new Result(Result.Type.INCOMPLETE_PATH, values,
                            resolvedObjects);
                } else {
                    return new Result(Result.Type.CAPPED_MULTI_LIST_VALUE,
                            Arrays.asList(value),
                            resolvedObjects);
                }
            }
            return new Result(Result.Type.SINGLE_VALUE, value, resolvedObjects);
        } else {
            return new Result(Result.Type.INCOMPLETE_PATH, null, resolvedObjects);
        }
    }
    
    private Object coerce(Object value) {
        if (expectedType != null) {
            if (value != ELContext.INCOMPLETE_PATH_RESULT) {
                return ELSupport.coerceToType(value, expectedType);
            }
        }
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.expr.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#isLiteralText()
     */
    public boolean isLiteralText() {
        try {
            return this.getNode() instanceof AstLiteralExpression;
        } catch (ELException ele) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#isReadOnly(javax.el.ELContext)
     */
    public boolean isReadOnly(ELContext context)
            throws PropertyNotFoundException, ELException {
        EvaluationContext ctx = new EvaluationContext(context, this.fnMapper,
                this.varMapper, this);
        return this.getNode().isReadOnly(ctx);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.expr = in.readUTF();
        String type = in.readUTF();
        if (!"".equals(type)) {
            this.expectedType = ReflectionUtil.forName(type);
        }
        this.fnMapper = (FunctionMapper) in.readObject();
        this.varMapper = (VariableMapper) in.readObject();
    }

    
    // PENDING: Notice how I'm not doing the following for isReadOnly and
    // others.
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.el.ValueExpression#setValue(javax.el.ELContext,
     *      java.lang.Object)
     */
    public void setValue(ELContext context, Object value)
            throws PropertyNotFoundException, PropertyNotWritableException,
            ELException {
        EvaluationContext ctx = new EvaluationContext(context, this.fnMapper,
                this.varMapper, this, true);
        this.getNode().setValue(ctx, value);
        List<Expression.ResolvedObject> resolvedObjects = ctx.getResolvedObjects();
        List resolvedList = null;
        // PENDING: make this a property
        int listThreshold = 10;
        for (Expression.ResolvedObject resolved : resolvedObjects) {
            if (resolved instanceof ResolvedList) {
                if (resolvedList == null) {
                    resolvedList = ((ResolvedList)resolved).getSource();
                } else if (resolvedList != ((ResolvedList)resolved).getSource()) {
                    // Too many lists, bale.
                    throw new ELException(
                            "Expression consists of multiple lists, can only resolve a single list");
                }
            }
        }
        if (resolvedList != null && resolvedList.size() != 1) {
            // There's a List, need to repeatedly evaluate the expression to get
            // the values in the List
            int listSize = resolvedList.size();
            for (int i = 1; i < listSize; i++) {
                ctx.reset();
                ctx.setResolvingListIndex(i);
                this.getNode().setValue(ctx, value);
            }
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.expr);
        out.writeUTF((this.expectedType != null) ? this.expectedType.getName()
                : "");
        out.writeObject(this.fnMapper);
        out.writeObject(this.varMapper);
    }

    public String toString() {
        return "ValueExpression["+this.expr+"]";
    }
}
