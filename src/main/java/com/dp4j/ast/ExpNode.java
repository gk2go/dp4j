/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.ast;

import com.dp4j.processors.core.PrivateAccessProcessor;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree.JCExpression;

/**
 *
 * @author simpatico
 */
public class ExpNode extends Node{
    public final StmtNode parent;
    public final ExpressionTree actual;
    protected final PrivateAccessProcessor pap;

    public ExpNode( ExpressionTree actual, StmtNode parent){
        super(parent.scope, actual, parent.cut);
        this.parent = parent;
        this.actual = actual;
        pap = parent.pap;
    }

    @Override
    public boolean isAccessible(){
        return pap.isAccessible((JCExpression) actual, cut, this);
    }

}
