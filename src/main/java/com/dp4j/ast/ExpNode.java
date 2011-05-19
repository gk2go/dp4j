/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.ast;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;

/**
 *
 * @author simpatico
 */
public class ExpNode extends Node{

    public ExpNode(Scope scope, ExpressionTree actual){
        super(scope, actual);
    }

    

}
