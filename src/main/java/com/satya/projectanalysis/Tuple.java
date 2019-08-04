package com.satya.projectanalysis;

public class Tuple<X,Y> {
    X _1;
    Y _2;

    public Tuple(X _1, Y _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public static <X,Y>  Tuple<X,Y> of(X x, Y y){
        return new Tuple<>(x,y);
    }
}
