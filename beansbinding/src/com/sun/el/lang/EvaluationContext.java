/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.el.lang;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.Expression;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

public final class EvaluationContext extends ELContext {

    private final ELContext elContext;

    private final FunctionMapper fnMapper;

    private final VariableMapper varMapper;
    
    private final Expression expression;
    
    private final Set<Expression.ResolvedObject> resolvedObjects;
    
    public int listIndex;

    private boolean incompletePath;

    public EvaluationContext(ELContext elContext, FunctionMapper fnMapper,
            VariableMapper varMapper, Expression expression) {
        this(elContext, fnMapper, varMapper, expression, false);
    }

    public EvaluationContext(ELContext elContext, FunctionMapper fnMapper,
            VariableMapper varMapper, Expression expression, 
            boolean trackResolvedObjects) {
        this.elContext = elContext;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
        this.expression = expression;
        Set<Expression.ResolvedObject> resolvedObjects;
        if (trackResolvedObjects) {
            resolvedObjects = new LinkedHashSet<Expression.ResolvedObject>(1);
        } else {
            resolvedObjects = null;
        }
        this.resolvedObjects = resolvedObjects;
        listIndex = 0;
    }
    
    public Expression getExpression() {
        return expression;
    }

    public ELContext getELContext() {
        return this.elContext;
    }

    public FunctionMapper getFunctionMapper() {
        return this.fnMapper;
    }

    public VariableMapper getVariableMapper() {
        return this.varMapper;
    }

    public Object getContext(Class key) {
        return this.elContext.getContext(key);
    }

    public ELResolver getELResolver() {
        return this.elContext.getELResolver();
    }

    public boolean isPropertyResolved() {
        return this.elContext.isPropertyResolved();
    }

    public void putContext(Class key, Object contextObject) {
        this.elContext.putContext(key, contextObject);
    }

    public void setPropertyResolved(boolean resolved) {
        this.elContext.setPropertyResolved(resolved);
    }
    
    public void setIncompletePath(boolean incompletePath) {
        this.incompletePath = incompletePath;
    }
    
    public boolean getIncompletePath() {
        return incompletePath;
    }

    public void resolvingList(List list) {
        if (resolvedObjects != null) {
            resolvedObjects.add(new Expression.ResolvedList(list));
        }
    }

    public void resolvingProperty(Object base, Object property) {
        if (resolvedObjects != null) {
            resolvedObjects.add(new Expression.ResolvedProperty(base, property));
        }
    }
    
    public List<Expression.ResolvedObject> getResolvedObjects() {
        if (resolvedObjects != null) {
            return new ArrayList<Expression.ResolvedObject>(resolvedObjects);
        }
        return null;
    }
    
    public void setResolvingListIndex(int listIndex) {
        this.listIndex = listIndex;
    }

    public int getResolvingListIndex() {
        return listIndex;
    }

    public void reset() {
        listIndex = 0;
        incompletePath = false;
    }
    
    

}
