/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.beans.binding.ext.*;
import javax.swing.*;
import com.sun.java.swing.binding.*;
import javax.swing.text.*;

/**
 * @author Shannon Hickey
 */
public final class SwingPropertyConfig {

    private SwingPropertyConfig() {}
    
    public static final BeanDelegateProvider TEXT_TEXT_ON_TYPE =
            new JTextComponentDelegateProvider(JTextComponentDelegateProvider.ChangeStrategy.ON_TYPE);

    public static final BeanDelegateProvider TEXT_TEXT_ON_FOCUS_LOST =
            new JTextComponentDelegateProvider(JTextComponentDelegateProvider.ChangeStrategy.ON_FOCUS_LOST);

    public static final BeanDelegateProvider TEXT_TEXT_ON_ACTION_OR_FOCUS_LOST =
            new JTextComponentDelegateProvider(JTextComponentDelegateProvider.ChangeStrategy.ON_ACTION_OR_FOCUS_LOST);

}
