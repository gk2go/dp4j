/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;

import com.sun.tools.javac.code.Type;

/**
 *
 * @author simpatico
 */
public class VarArgType extends com.sun.tools.javac.code.Type{
    public final Type t;

    private VarArgType(com.sun.tools.javac.code.Type t){
        super(t.tag, t.tsym);
        this.t = (Type) ((javax.lang.model.type.ArrayType) t).getComponentType();
    }

    public VarArgType(javax.lang.model.type.ArrayType arrayType) {
        this((com.sun.tools.javac.code.Type) arrayType);
    }
}
