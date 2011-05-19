/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.ast;

import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;

/**
 *
 * @author simpatico
 */
public class StmtNode extends Node{

    public StmtNode(Scope scope, StatementTree actual){
        super(scope, actual);
    }
}
