/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dp4j.ast;

import com.dp4j.processors.core.PrivateAccessProcessor;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.code.Type;

/**
 *
 * @author simpatico
 */
public class VarDecl extends StmtNode {

    public final VariableTree actual;
    private final Resolver rs;

    public VarDecl(Scope scope, VariableTree actual, CompilationUnitTree cut, final Resolver rs, PrivateAccessProcessor pap) {
        super(scope, actual, cut, pap);
        this.actual = actual;
        this.rs = rs;
    }

    //TODO: strategy is to decorate the Tree elements and to ovverride their visit, and then force our decorators at process time through pointers
    @Override
    public void process(JCBlock encBlock) {
        if (!isAccessible()) {

            if (!isAccessible()) {
                final ExpressionTree init = actual.getInitializer();
                Symbol s = rs.getSymbol((JCExpression) init, cut, this);
                final Type t = rs.getType(s);
                ((JCVariableDecl) actual).init = pap.processCond((JCExpression) init, cut, new ExpNode(init, this), encBlock);
                //FIXME:
                //                varDec.sym = (VarSymbol) rs.getSymbol(cut, this, null, actual.getName(), null);
//                varDec.type = varDec.sym.type;
//                if (varDec.init.type == null) {
//                    varDec.init.type = varDec.sym.type;
//                }
//                if (differentArg(t, varDec.sym.type)) {
//                    varDec.init = tm.TypeCast(rs.getBoxedType(varDec.sym), varDec.init);
//                }
            }
        }
    }

    public boolean isAccessible() {
        final ExpressionTree init = actual.getInitializer();
        if (init == null) {
            return true; //just a declaration is always accessible
        }
        return new ExpNode(init, this).isAccessible();
    }
}
