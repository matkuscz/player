/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.dev;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/**
 * Annotates code that depends on or is depended on by other code. Marks and 
 * documents inflexible code, that should not be changed arbitrarily.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Dependencies.class)
@Target({METHOD, CONSTRUCTOR, FIELD, TYPE})
public @interface Dependency {
    String value() default "";
}
