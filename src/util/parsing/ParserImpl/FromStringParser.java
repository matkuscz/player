/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.parsing.ParserImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import util.parsing.ObjectStringParser;

/**
 *
 * @author Plutonium_
 */
public class FromStringParser implements ObjectStringParser {

    @Override
    public boolean supports(Class type) {
        try {
            Method m = type.getDeclaredMethod("fromString", String.class);
//            if (m.getReturnType().equals(type)) throw new NoSuchMethodException();
            return true;
        } catch ( NoSuchMethodException ex) {
            return false;
        }
    }

    @Override
    public Object fromS(Class type, String source) {        
        //try parsing unknown types with fromString(String) method if available
        try {
            Method m = type.getDeclaredMethod("fromString", String.class);
            return m.invoke(null, source);
        } catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public String toS(Object object) {
        return object.toString();
    }
    
}