package edu.umn.cs.melt.copper.compiletime.logging.messages;

import java.util.BitSet;

import edu.umn.cs.melt.copper.compiletime.auxiliary.SymbolTable;
import edu.umn.cs.melt.copper.compiletime.logging.CompilerLevel;
import edu.umn.cs.melt.copper.compiletime.logging.CompilerLogMessage;
import edu.umn.cs.melt.copper.compiletime.logging.CompilerLogMessageType;
import edu.umn.cs.melt.copper.compiletime.spec.grammarbeans.CopperASTBean;
import edu.umn.cs.melt.copper.compiletime.spec.numeric.PSSymbolTable;

public class CyclicPrecedenceRelationMessage implements CompilerLogMessage
{
	private SymbolTable<CopperASTBean> symbolTable;
	private BitSet cycle;
	
	
	public CyclicPrecedenceRelationMessage(SymbolTable<CopperASTBean> symbolTable, BitSet cycle)
	{
		this.symbolTable = symbolTable;
		this.cycle = cycle;
	}

	@Override
	public CompilerLevel getLevel()
	{
		return CompilerLevel.QUIET;
	}

	@Override
	public int getType()
	{
		return CompilerLogMessageType.CYCLIC_PRECEDENCE;
	}

	@Override
	public boolean isError()
	{
		return true;
	}

	@Override
	public boolean isFatalError()
	{
		return false;
	}

	public String toString()
	{
		return "Cyclic precedence relation involving terminals\n" + PSSymbolTable.bitSetPrettyPrint(cycle,symbolTable,"  ",80);
	}
}
