/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 *
 *//* Generated By:JJTree: Do not edit this line. AstAnd.java */

package com.sun.el.parser;

import javax.el.ELContext;
import javax.el.ELException;

import com.sun.el.lang.EvaluationContext;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class AstAnd extends BooleanNode {
    public AstAnd(int id) {
        super(id);
    }

    public Object getValue(EvaluationContext ctx)
            throws ELException {
        Object obj = children[0].getValue(ctx);
        if (obj == ELContext.INCOMPLETE_PATH_RESULT) {
            return ELContext.INCOMPLETE_PATH_RESULT;
        }
        Boolean b = coerceToBoolean(obj);
        if (!b.booleanValue()) {
            return b;
        }
        obj = children[1].getValue(ctx);
        if (obj == ELContext.INCOMPLETE_PATH_RESULT) {
            return ELContext.INCOMPLETE_PATH_RESULT;
        }
        b = coerceToBoolean(obj);
        return b;
    }
}