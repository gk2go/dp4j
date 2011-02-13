/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author simpatico
 */
public class Data {

    final Map<String, JCExpression> vars;
    final CompilationUnitTree cut;
    final Object packageName; com.sun.source.tree.Scope scope;
    final JCStatement stmt;
    final Collection<Symbol> varSyms;

    public Data(Map<String, JCExpression> vars, CompilationUnitTree cut, Object packageName, Scope scope, JCStatement stmt, Collection<Symbol> varSyms) {
        this.vars = vars;
        this.cut = cut;
        this.packageName = packageName;
        this.scope = scope;
        this.stmt = stmt;
        this.varSyms = varSyms;
    }

}
