/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 *
 *//* Generated By:JJTree: Do not edit this line. AstFalse.java */

package com.sun.el.parser;

import org.jdesktop.el.ELException;

import com.sun.el.lang.EvaluationContext;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class AstFalse extends BooleanNode {
    public AstFalse(int id) {
        super(id);
    }

    public Object getValue(EvaluationContext ctx)
            throws ELException {
        return Boolean.FALSE;
    }
}
