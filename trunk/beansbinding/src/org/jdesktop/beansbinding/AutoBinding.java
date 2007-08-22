/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Shannon Hickey
 */
public class AutoBinding<SS, SV, TS, TV> extends Binding<SS, SV, TS, TV> {

    private UpdateStrategy strategy;

    public enum UpdateStrategy {
        READ,
        READ_ONCE,
        READ_WRITE
    }

    protected AutoBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        super(sourceObject, sourceProperty, targetObject, targetProperty, name);

        if (strategy == null) {
            throw new IllegalArgumentException("must provide update strategy");
        }

        this.strategy = strategy;
    }

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
            } else if (pse.getWriteableChanged() && pse.isWriteable()) {
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
