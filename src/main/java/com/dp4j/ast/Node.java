/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.ast;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Scope;


/**
 *
 * @author simpatico
 */
//@Decorator
public class Node implements Tree{
    public Scope scope;
    public Tree actual;
    public final CompilationUnitTree cut;

    public Node(Scope scope, Tree actual, CompilationUnitTree cut){
        this.scope = scope;
        this.actual = actual;
        this.cut = cut;
    }

    @Override
    public Kind getKind() {
        return actual.getKind();
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> tv, D d) {
        return actual.accept(tv, d);
    }

    public boolean isAccessible(){
        return false;//FIXME
    }

}
