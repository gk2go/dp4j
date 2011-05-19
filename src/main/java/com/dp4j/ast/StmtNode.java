/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.ast;

import com.dp4j.processors.core.PrivateAccessProcessor;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;

/**
 *
 * @author simpatico
 */
public class StmtNode extends Node{
    public StatementTree actual;
    protected final PrivateAccessProcessor pap;

    public StmtNode(Scope scope, StatementTree actual, CompilationUnitTree cut, PrivateAccessProcessor pap){
        super(scope, actual, cut);
        this.pap = pap;
    }

    public void process(JCBlock encBlock){

    }


}
