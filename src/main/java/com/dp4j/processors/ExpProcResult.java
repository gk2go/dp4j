/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dp4j.processors;

import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;

/**
 *
 * @author simpatico
 */
public class ExpProcResult {
    private final com.sun.tools.javac.util.List<JCStatement> stats;
    private final JCExpression exp;

    public ExpProcResult(com.sun.tools.javac.util.List<JCStatement> stats, JCExpression exp){
        this.stats = stats;
        this.exp = exp;

    }

    public com.sun.tools.javac.util.List<JCStatement> getStats(){
        return stats;
    }

    public JCExpression getExp(){
        return exp;
    }
}
