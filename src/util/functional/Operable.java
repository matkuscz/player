/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Provides functional methods for object.
 * 
 * @param <O> Object - self type: {@code X extends Operable<X> }
 */
public interface Operable<O> {
    
    public default O apply(UnaryOperator<O> op) {
        return op.apply((O)this);
    }
    
    public default <R> R apply(Function<O,R> op) {
        return op.apply((O)this);
    }
    
    public default O apply(O e, BinaryOperator<O> op) {
        return op.apply((O)this, e);
    }
    
    public default void use(Consumer<O> op) {
        op.accept((O)this);
    }

    public default O useAnd(Consumer<O> op) {
        op.accept((O)this);
        return (O) this;
    }
    
    public default boolean isIn(O os) {
        return Stream.of(os).anyMatch(o->o==this);
    }
    
    
}
