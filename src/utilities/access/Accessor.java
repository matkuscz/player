/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.access;

import java.util.function.Consumer;

/**
 * Simple object wrapper similar to {@link javafx.beans.property.Property}, but
 * simpler and with the ability to apply value change.
 * 
 * @author Plutonium_
 */
public class Accessor<T> implements ApplicableValue<T> {
    
    private T value;
    private Consumer<T> applier;
    
    public Accessor(T val) {
        value = val;
    }
    
    public Accessor(T val, Consumer<T> applier) {
        value = val;
        this.applier = applier;
    }
    
    /** {@inheritDoc} */
    @Override
    public T getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(T val) {
        value = val;
    }
    
    /** {@inheritDoc} */
    @Override
    public void applyValue() {
        if (applier != null)  applier.accept(value);
    }
    
    /**
     * Same as {@link #setValue()}, but (if and only) if value changed and applier is 
     * available, executes the applier.
     * <p>
     * Always use {@link #setValue()} instead of this method inside applier to 
     * prevent infinite recursion.
     */
    @Override
    public void setNapplyValue(T val) {
        if (applier == null) {
            value = val;
        } else {
            boolean changed = value.equals(val);
            value = val;
            if (changed) applier.accept(val);
        }
    }
    
    /** Sets applier. Applier is a code that applies the value in any way. */
    public void setApplier(Consumer<T> applier) {
        this.applier = applier;
    }
    
    /** Gets applier. Applier is a code that applies the value. It can do anything. */
    public Consumer<T> getApplier() {
        return applier;
    }
    
}
