package com.satya.projectanalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.LinkOption;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class JavaUtils {
    static Logger LOG = LoggerFactory.getLogger(JavaUtils.class);
    public interface ThrowingSupplier<X> {
        X get() throws Throwable;
    }

    public interface ThrowingConsumer<X> {
        void accept(X x) throws Throwable;
    }

    public interface ThrowingFunction<X,Y> {
        Y apply(X x) throws Throwable;
    }

    public static <T> T wrapInRuntimeException(ThrowingSupplier<T> throwingSupplier){
        return wrapInRuntimeException(throwingSupplier, true);
    }

    public static <T> T wrapInRuntimeException(ThrowingSupplier<T> throwingSupplier, boolean throwException){
        try {
            return (T) throwingSupplier.get();
        } catch (Throwable throwable) {
            if(throwException) throw new RuntimeException(throwable);
            else {
//                throwable.printStackTrace();
                LOG.info("Error {}", throwable.getMessage());
                return null;
            }
        }
    }

    public static <X,Y> Function<X, Y> wrapInRuntimeException(ThrowingFunction<X, Y> throwingSupplier){
        return wrapInRuntimeException(throwingSupplier, true);
    }

    public static <X,Y> Function<X, Y> wrapInRuntimeException(ThrowingFunction<X, Y> throwingSupplier, boolean throwException){
        return (X x) -> {
            try {
                return throwingSupplier.apply(x);
            } catch (Throwable throwable) {
                if(throwException) throw new RuntimeException(throwable);
                else {
//                    throwable.printStackTrace();
                    LOG.info("Error {}", throwable.getMessage());
                    return null;
                }
            }
        };
    }

    public static <T> Consumer<T> wrapInRuntimeException(ThrowingConsumer<T> throwingConsumer){
        return wrapInRuntimeException(throwingConsumer, true);
    }

    public static <T> Consumer<T> wrapInRuntimeException(ThrowingConsumer<T> throwingConsumer, boolean throwException){
        return x -> {
            try {
                throwingConsumer.accept(x);
            } catch (Throwable throwable) {
               if(throwException) throw new RuntimeException(throwable);
               else {
//                   throwable.printStackTrace();
                   LOG.info("Error {}", throwable.getMessage());
               }
            }
        };
    }

    public static <X,Y> List<Tuple<X,Y>> zipLists(List<X> xList, List<Y> yList){
        int max = Math.max(xList.size(), yList.size());
        List<Tuple<X,Y>> zipped = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            zipped.add(Tuple.of(getFromListSafe(xList,i), getFromListSafe(yList,i)));
        }

        return zipped;
    }

    public static <X,Y> Tuple<X,Y>[] zipArrays(X[] xArray, Y[] yArray){
        int max = Math.max(xArray.length, yArray.length);

        List<X> xList = Arrays.asList(xArray);
        List<Y> yList = Arrays.asList(yArray);

        List<Tuple<X,Y>> zipped = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            zipped.add(Tuple.of(getFromListSafe(xList,i), getFromListSafe(yList,i)));
        }

        return zipped.toArray(new Tuple[0]);
    }

    private static <T> T getFromListSafe(List<T> list, int idx){
        if(idx < list.size()) return list.get(idx);
        return null;
    }

}
