/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.List;
import java.util.ArrayList;

/**
 * @param <SS> the type of source object
 * @param <SV> the type of value that the source property represents
 * @param <TS> the type of target object
 * @param <SV> the type of value that the target property represents
 *
 * @author Shannon Hickey
 */
public class AutoBinding<SS, SV, TS, TV> extends Binding<SS, SV, TS, TV> {

    private UpdateStrategy strategy;

    /**
     * An enumeration representing the possible update strategies of an
     * {@code AutoBinding}. See the class level documentation of
     * {@code AutoBinding} for complete details on the sync behavior for
     * each possible update strategy.
     */
    public enum UpdateStrategy {
        
        /**
         * An update strategy where the {@code Binding} tries to keep the target
         * in sync by updating it in response to changes in the source.
         */
        READ_ONCE,

        /**
         * An update strategy where the {@code Binding} sets the target value
         * from the source only once, at bind time.
         */
        READ,

        /**
         * An update strategy where the {@code Binding} tries to keep the source
         * and target in sync by updating both in response to changes in the other.
         */
        READ_WRITE
    }

    /**
     * Create an instance of {@code AutoBinding} between two properties of two objects,
     * with the given update strategy.
     *
     * @param strategy the update strategy
     * @param sourceObject the source object
     * @param sourceProperty a property on the source object
     * @param targetObject the target object
     * @param targetProperty a property on the target object
     * @param name a name for the {@code Binding}
     * @throws IllegalArgumentException if the source property or target property is {@code null}
     */
    protected AutoBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        super(sourceObject, sourceProperty, targetObject, targetProperty, name);

        if (strategy == null) {
            throw new IllegalArgumentException("must provide update strategy");
        }

        this.strategy = strategy;
    }

    /**
     * Returns the {@code AutoBinding's} update strategy.
     *
     * @return the update strategy
     */
    public final UpdateStrategy getUpdateStrategy() {
        return strategy;
    }

    private final void tryRefreshThenSave() {
        SyncFailure refreshFailure = refresh();
        if (refreshFailure == null) {
            notifySynced();
        } else {
            SyncFailure saveFailure = save();
            if (saveFailure == null) {
                notifySynced();
            } else {
                notifySyncFailed(refreshFailure, saveFailure);
            }
        }
    }

    private final void trySaveThenRefresh() {
        SyncFailure saveFailure = save();
        if (saveFailure == null) {
            notifySynced();
        } else if (saveFailure.getType() == SyncFailureType.CONVERSION_FAILED || saveFailure.getType() == SyncFailureType.VALIDATION_FAILED) {
            notifySyncFailed(saveFailure);
        } else {
            SyncFailure refreshFailure = refresh();
            if (refreshFailure == null) {
                notifySynced();
            } else {
                notifySyncFailed(saveFailure, refreshFailure);
            }
        }
    }

    protected void bindImpl() {
        UpdateStrategy strat = getUpdateStrategy();

        if (strat == UpdateStrategy.READ_ONCE) {
            refreshAndNotify();
        } else if (strat == UpdateStrategy.READ) {
            refreshAndNotify();
        } else {
            tryRefreshThenSave();
        }
    }

    protected void unbindImpl() {}

    /**
     * Returns a string representation of the {@code AutoBinding}. This
     * method is intended to be used for debugging purposes only, and
     * the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representation of this {@code AutoBinding}
     */
    public String toString() {
        return getClass().getName() + " [" + paramString() + "]";
    }

    private String paramString() {
        return "name=" + getName() +
               ", sourceObject=" + getSourceObject() +
               ", sourceProperty=" + getSourceProperty() +
               ", targetObject=" + getTargetObject() +
               ", targetProperty" + getTargetProperty() +
               ", validator=" + getValidator() +
               ", converter=" + getConverter() +
               ", sourceNullValue=" + getSourceNullValue() +
               ", targetNullValue=" + getTargetNullValue() +
               ", sourceUnreadableValue=" + getSourceUnreadableValue() +
               ", hasChangedSource=" + getHasEditedSource() +
               ", hasChangedTarget=" + getHasEditedTarget() +
               ", bound=" + isBound() +
               ", updateStrategy=" + getUpdateStrategy();
    }

    protected void sourceChangedImpl(PropertyStateEvent pse) {
        if (strategy == UpdateStrategy.READ_ONCE) {
            // nothing to do - superclass already deals with edited
        } else if (strategy == UpdateStrategy.READ) {
            if (pse.getValueChanged()) {
                refreshAndNotify();
            }
        } else if (strategy == UpdateStrategy.READ_WRITE) {
            if (pse.getValueChanged()) {
                tryRefreshThenSave();
            } else if (pse.isWriteable()) {
                saveAndNotify();
            }
        }
    }

    protected void targetChangedImpl(PropertyStateEvent pse) {
        if (strategy == UpdateStrategy.READ_ONCE) {
            // nothing to do - superclass already deals with edited
        } else if (strategy == UpdateStrategy.READ) {
            if (pse.getWriteableChanged() && pse.isWriteable()) {
                refreshAndNotify();
            }
        } else if (strategy == UpdateStrategy.READ_WRITE) {
            if (pse.getWriteableChanged() && pse.isWriteable()) {
                tryRefreshThenSave();
            } else {
                trySaveThenRefresh();
            }
        }
    }

}
