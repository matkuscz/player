package util.collections;

import java.util.Objects;
import static util.collections.Tuples.tuple;
import unused.TetraConsumer;
import unused.TetraFunction;
import unused.TetraPredicate;

public class Tuple4<A, B, C, D> {
    public final A _1;
    public final B _2;
    public final C _3;
    public final D _4;

    Tuple4(A a, B b, C c, D d) {
        _1 = a;
        _2 = b;
        _3 = c;
        _4 = d;
    }

    public Tuple4<A, B, C, D> update1(A a) {
        return tuple(a, _2, _3, _4);
    }

    public Tuple4<A, B, C, D> update2(B b) {
        return tuple(_1, b, _3, _4);
    }

    public Tuple4<A, B, C, D> update3(C c) {
        return tuple(_1, _2, c, _4);
    }

    public Tuple4<A, B, C, D> update4(D d) {
        return tuple(_1, _2, _3, d);
    }

    public <T> T map(TetraFunction<? super A, ? super B, ? super C, ? super D, ? extends T> f) {
        return f.apply(_1, _2, _3, _4);
    }

    public boolean test(TetraPredicate<? super A, ? super B, ? super C, ? super D> f) {
        return f.test(_1, _2, _3, _4);
    }

    public void exec(TetraConsumer<? super A, ? super B, ? super C, ? super D> f) {
        f.accept(_1, _2, _3, _4);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Tuple4) {
            Tuple4<?, ?, ?, ?> that = (Tuple4<?, ?, ?, ?>) other;
            return Objects.equals(this._1, that._1)
                    && Objects.equals(this._2, that._2)
                    && Objects.equals(this._3, that._3)
                    && Objects.equals(this._4, that._4);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4);
    }

    @Override
    public String toString() {
        return "("
                + Objects.toString(_1) + ", "
                + Objects.toString(_2) + ", "
                + Objects.toString(_3) + ", "
                + Objects.toString(_4)
                + ")";
    }
}
