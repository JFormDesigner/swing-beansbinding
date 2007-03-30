/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 *
 *//* Generated By:JJTree: Do not edit this line. AstMinus.java */

package com.sun.el.parser;

import javax.el.ELContext;
import javax.el.ELException;

import com.sun.el.lang.ELArithmetic;
import com.sun.el.lang.EvaluationContext;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class AstMinus extends ArithmeticNode {
    public AstMinus(int id) {
        super(id);
    }

    public Object getValue(EvaluationContext ctx)
            throws ELException {
        Object obj0 = this.children[0].getValue(ctx);
        if (obj0 == ELContext.INCOMPLETE_PATH_RESULT) {
            return ELContext.INCOMPLETE_PATH_RESULT;
        }
        Object obj1 = this.children[1].getValue(ctx);
        if (obj1 == ELContext.INCOMPLETE_PATH_RESULT) {
            return ELContext.INCOMPLETE_PATH_RESULT;
        }
        return ELArithmetic.subtract(obj0, obj1);
    }
}
