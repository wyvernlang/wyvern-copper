/**
 * 
 */
package edu.umn.cs.melt.copper.compiletime.semantics.lalr1;

import java.util.HashSet;

import edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammar.GrammarName;
import edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammar.GrammarSource;
import edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammar.GrammarSymbol;
import edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammar.NonTerminal;
import edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammar.Production;
import edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammar.Terminal;
import edu.umn.cs.melt.copper.compiletime.abstractsyntax.intermediate.syntaxtranslator.MasterController;
import edu.umn.cs.melt.copper.compiletime.finiteautomaton.lalrengine.lalr1.LALR1DFA;
import edu.umn.cs.melt.copper.compiletime.finiteautomaton.lalrengine.lalr1.LALR1State;
import edu.umn.cs.melt.copper.compiletime.finiteautomaton.lalrengine.lalr1.LALR1StateItem;
import edu.umn.cs.melt.copper.compiletime.logging.CompilerLogMessageSort;
import edu.umn.cs.melt.copper.compiletime.logging.CompilerLogger;
import edu.umn.cs.melt.copper.runtime.auxiliary.internal.PrettyPrinter;
import edu.umn.cs.melt.copper.runtime.logging.CopperException;


/**
 * Checks the composability of grammars according to the
 * necessary constraints.
 * @author August Schwerdfeger &lt;<a href="mailto:schwerdf@cs.umn.edu">schwerdf@cs.umn.edu</a>&gt;
 *
 */
public class ComposabilityChecker
{
	/* 
	 * Note on the terminology generated by the methods in this class:
	 * 
	 * When a state/item in the composed DFA "has lookahead spillage," it means that the state
	 * is LR(0)-equivalent to some state in the host DFA M^H (excluding bridge items and marking
	 * terminal lookahead) but is not LR(1)-equivalent to that state, because the extension has 
	 * introduced ("spilled") new lookahead over into the state. Hence, there is a state that
	 * is not in the proper "host-owned" partition M^(E_i)_H and does not fit the form for the
	 * other two, so the analysis fails.
	 * 
	 * 
	 */
	public static boolean isWanted(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,GrammarSymbol gs)
	{
		if(gs instanceof Terminal) return wantedGrammars.contains(grammar.getOwner((Terminal) gs));
		else if(gs instanceof NonTerminal) return wantedGrammars.contains(grammar.getOwner((NonTerminal) gs));
		else return false;
	}
	public static boolean isWanted(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,Terminal t)
	{
		return wantedGrammars.contains(grammar.getOwner(t));
	}
	public static boolean isWanted(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,NonTerminal nt)
	{
		return wantedGrammars.contains(grammar.getOwner(nt));
	}
	public static boolean isWanted(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,Production p)
	{
		return wantedGrammars.contains(grammar.getOwner(p));
	}
	// This is only supposed to let through host-to-extension bridge productions.
	public static boolean isWanted(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,LALR1StateItem item)
	{
		return isWanted(grammar,wantedGrammars,item.getProd()) ||
		       isValidBridgeItem(grammar,wantedGrammars,item);
                 /*(isWanted(grammar,wantedGrammars,item.getProd().getLeft()) &&
                  //item.getProd().length() == 2 &&
                  !isWanted(grammar,wantedGrammars,item.getProd().getSymbol(0)) &&
                  item.getProd().getSymbol(0) instanceof Terminal &&
                  //!isWanted(grammar,wantedGrammars,item.getProd().getSymbol(1)) &&
                  item.isBeginning());*/
	}
	
	public static boolean isBridgeProd(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,Production p)
	{
		return !isWanted(grammar,wantedGrammars,p) &&
				isWanted(grammar,wantedGrammars,p.getLeft());
	}
	
	public static boolean isValidBridgeProd(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,Production p)
	{
		return isBridgeProd(grammar,wantedGrammars,p) &&
		   !isWanted(grammar,wantedGrammars,p.getSymbol(0)) &&
			p.length() >= 1 &&
			p.getSymbol(0) instanceof Terminal;
	}
	
	public static boolean isBridgeItem(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,LALR1StateItem item)
	{
		return isBridgeProd(grammar,wantedGrammars,item.getProd()) &&
			   item.isBeginning();
	}
	
	public static boolean isValidBridgeItem(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,LALR1StateItem item)
	{
		return isValidBridgeProd(grammar,wantedGrammars,item.getProd()) &&
		   item.isBeginning();
		//if(!isBridgeItem(grammar,wantedGrammars,item)) return false;
		//if(item.getProd().length() == 0) return false;
		//if(item.getProd().getSymbol(0) instanceof NonTerminal) return false;
		//if(isWanted(grammar,wantedGrammars,item.getProd().getSymbol(0))) return false;
		//if(!item.isBeginning()) return false;
		//return true;
	}
	
	public static boolean isInvalidBridgeItem(GrammarSource grammar,HashSet<GrammarName> wantedGrammars,LALR1StateItem item)
	{
		return isBridgeItem(grammar,wantedGrammars,item) && !isValidBridgeItem(grammar,wantedGrammars,item);
		/*if(!isBridgeItem(grammar,wantedGrammars,item)) return false;
		if(item.getProd().length() == 0) return true;
		if(item.getProd().getSymbol(0) instanceof NonTerminal) return true;
		if(isWanted(grammar,wantedGrammars,item.getProd().getSymbol(0))) return true;
		return false;*/
	}
	
	public static GrammarSource extractWantedGrammars(GrammarSource grammar,HashSet<GrammarName> wantedGrammars)
	{
		GrammarSource newGS = new GrammarSource();
		for(NonTerminal nt : grammar.getNT())
		{
			if(isWanted(grammar,wantedGrammars,nt))
			{
				newGS.addToNT(nt);
				if(grammar.hasNTAttributes(nt)) newGS.addNTAttributes(nt,grammar.getNTAttributes(nt));
		    	if(grammar.pContains(nt))
				{
					for(Production p : grammar.getP(nt))
					{
						if(isWanted(grammar,wantedGrammars,p))
						{
							newGS.addToP(p);
							if(grammar.hasProductionAttributes(p)) newGS.addProductionAttributes(p,grammar.getProductionAttributes(p));
						}
						if(isValidBridgeProd(grammar,wantedGrammars,p))
						{
							newGS.addToT((Terminal) p.getSymbol(0));
							newGS.addToP(p);
							if(grammar.hasProductionAttributes(p)) newGS.addProductionAttributes(p,grammar.getProductionAttributes(p));
						}
					}
				}
			}
		}
		for(Terminal t : grammar.getT())
		{
			if(isWanted(grammar,wantedGrammars,t))
			{
				newGS.addToT(t);
				if(grammar.hasRegex(t)) newGS.addRegex(t,grammar.getRegex(t));
				if(grammar.hasLexicalAttributes(t)) newGS.addLexicalAttributes(t,grammar.getLexicalAttributes(t));
				if(grammar.hasOperatorAttributes(t)) newGS.addOperatorAttributes(t,grammar.getOperatorAttributes(t));
			}
		}
		// Terminal classes.
		newGS.constructPrecedenceRelationsGraph();
		for(Terminal t : grammar.getT())
		{
			for(Terminal u : grammar.getT())
			{
				if(grammar.getPrecedenceRelationsGraph().hasEdge(t,u)) newGS.addStaticPrecedenceRelation(t,u);
			}
		}
		// TODO Add other components of GrammarSource as necessary.
		// Parser attributes.
		// Disambiguation groups.
		if(isWanted(grammar,wantedGrammars,grammar.getStartSym())) newGS.setStartSym(grammar.getStartSym());
		// Start layout.
		// Grammar layout.
		for(GrammarName n : wantedGrammars) newGS.addContainedGrammar(n);
		// Default terminal code.
		// Default production code.
		// Parser sources.
		// Count of useless nonterminals.
		// Context sets.
		return newGS;
	}

	public CompilerLogger logger;
	
	public ComposabilityChecker(CompilerLogger logger)
	{
		this.logger = logger;
	}
	
	public boolean checkComposability(GrammarSource grammarHost,GrammarSource grammarHostAndExt,LALR1DFA host,LALR1DFA hostAndExt)
	throws CopperException
	{
		boolean passed = true;
		//System.err.println(host);
		for(NonTerminal nt : grammarHost.getNT())
		{
			if(logger.isLoggable(CompilerLogMessageSort.TICK)) logger.logTick(MasterController.DOT_WINDOW,".");
			HashSet<GrammarSymbol> hostAndExtFollow = new HashSet<GrammarSymbol>(grammarHostAndExt.getContextSets().getFollow(nt));
			hostAndExtFollow.removeAll(grammarHost.getContextSets().getFollow(nt));
			if(!grammarHost.getContextSets().getFollow(nt).containsAll(grammarHostAndExt.getContextSets().getFollow(nt)))
			{
				passed = false;
				logger.logMessage(CompilerLogMessageSort.ERROR,null,"Nonterminal " + nt + " has follow spillage of\n" + PrettyPrinter.iterablePrettyPrint(hostAndExtFollow,"   ",1) + "\n   from\n" + PrettyPrinter.iterablePrettyPrint(grammarHost.getContextSets().getFollow(nt),"   ",1) + "\n   to\n" + PrettyPrinter.iterablePrettyPrint(grammarHostAndExt.getContextSets().getFollow(nt),"   ",1));				
			}
		}
		for(LALR1State state : hostAndExt.getStates())
		{
			if(logger.isLoggable(CompilerLogMessageSort.TICK)) logger.logTick(MasterController.DOT_WINDOW,".");
			if(host.getLabel(state) != -1)
			{
				for(LALR1StateItem item : state.getItems())
				{
					if(!host.getLookahead(state,item).containsAll(hostAndExt.getLookahead(state,item)))
					{
						passed = false;
						if(logger.isLoggable(CompilerLogMessageSort.ERROR))
						{
							HashSet<Terminal> hostAndExtLA = new HashSet<Terminal>(hostAndExt.getLookahead(state,item));
							hostAndExtLA.removeAll(host.getLookahead(state,item));
							logger.logMessage(CompilerLogMessageSort.ERROR,null,"DFA state " + hostAndExt.getLabel(state)+ ", item " + item + " has lookahead spillage of\n" + PrettyPrinter.iterablePrettyPrint(hostAndExtLA,"   ",1) + "\n   from\n" + PrettyPrinter.iterablePrettyPrint(host.getLookahead(state,item),"   ",1) + "\n   to\n" + PrettyPrinter.iterablePrettyPrint(hostAndExt.getLookahead(state,item),"   ",1));
						}
					}
				}
			}
			else
			{
				boolean hasExtItem = false;
				GrammarName hostname = grammarHost.getHostGrammarName();
				HashSet<GrammarName> hostnameS = new HashSet<GrammarName>();
				hostnameS.add(hostname);
				for(LALR1StateItem item : state.getItems())
				{
					if(!isWanted(grammarHostAndExt,hostnameS,item))
					{
						hasExtItem = true;
						//break;
					}
					if(isInvalidBridgeItem(grammarHostAndExt,hostnameS,item))
					{
						passed = false;
						if(logger.isLoggable(CompilerLogMessageSort.ERROR)) logger.logMessage(CompilerLogMessageSort.ERROR,null,"DFA state " + hostAndExt.getLabel(state) + "has invalid bridge item " + item);
					}
				}
				if(!hasExtItem)
				{
					boolean isILSubset = false;
					for(LALR1State hstate : host.getStates())
					{
						if(state.isISubset(hstate))
						{
							if(!LALR1DFA.isILSubset(state,hostAndExt.getLookaheadTables(state),hstate,hostAndExt.getLookaheadTables(state)))
							{
								passed = false;
								if(logger.isLoggable(CompilerLogMessageSort.ERROR)) logger.logMessage(CompilerLogMessageSort.ERROR,null,"DFA state " + hostAndExt.getLabel(state) + " is a new-host state and is an I-subset but not an IL-subset of state " + hostAndExt.getLabel(hstate));
								break;
							}
							else isILSubset = true;

						}
					}
					if(!isILSubset)
					{
						passed = false;
						if(logger.isLoggable(CompilerLogMessageSort.ERROR)) logger.logMessage(CompilerLogMessageSort.ERROR,null,"DFA state " + hostAndExt.getLabel(state) + " is a new-host state and not an IL-subset");
					}
				}
			}
		}
		return passed;
	}
}
