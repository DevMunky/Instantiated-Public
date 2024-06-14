package dev.munky.instantiated.util;

import java.lang.reflect.Method;

public class Debugging {
    public static String getCallerName(){
        return getCallersNames(1)[0];
    }
    public static String[] getCallersNames(int back){
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return walker.walk(frames -> frames
                .map(frame-> frame.getClassName() + "#" + frame.getMethodName())
                .skip(2)
                .toList().subList(0,back)
                .toArray(String[]::new));
    }
}
