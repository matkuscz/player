/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import util.access.FieldValue.EnumerableValue;

/**
 * Accessor which can list all its possible values.
 * {@link Accessor} implementing {@link EnumerableValue}
 */
public class AccessorEnum<T> extends Accessor<T> implements EnumerableValue<T> {
    
    private final Supplier<Collection<T>> valueEnumerator;
    
    public AccessorEnum(T val, Consumer<T> applier, Supplier<Collection<T>> enumerator) {
        super(val, applier);
        valueEnumerator = enumerator;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<T> enumerateValues() {
        return valueEnumerator.get();
    }
    
}
