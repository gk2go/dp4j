/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.processors;

import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.code.Type;

/**
 *
 * @author simpatico
 */
public class ExpProcResult {

    public final com.sun.tools.javac.util.List<JCStatement> stats;
    public final JCExpression exp;
    public final Type type;

    public ExpProcResult(com.sun.tools.javac.util.List<JCStatement> stats, JCExpression exp, Type type) {
        this.stats = stats;
        this.exp = exp;
        this.type = type;

    }
}

