/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.code.Type;
/**
 *
 * @author simpatico
 */
public class ReflectedAccessResult{
    public final JCExpression exp;
    public final Type expType;

    public ReflectedAccessResult(JCExpression exp, Type expType){
        this.exp = exp;
        this.expType = expType;

    }

}
