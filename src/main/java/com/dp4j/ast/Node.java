/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.ast;

import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Scope;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 *
 * @author simpatico
 */
//@Decorator
public class Node implements Tree{
    public Scope scope;
    public Tree actual;
    public Set<String> exceptions = new HashSet<String>();

    public Node(Scope scope, Tree actual){
        this.scope = scope;
        this.actual = actual;
    }

    @Override
    public Kind getKind() {
        return actual.getKind();
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> tv, D d) {
        return actual.accept(tv, d);
    }



}
