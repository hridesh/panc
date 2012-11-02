package com.sun.source.tree;

public interface ForAllTree extends ExpressionTree{
	VariableTree getVariable();
	ExpressionTree getExpression();
	StatementTree getStatement();
}
