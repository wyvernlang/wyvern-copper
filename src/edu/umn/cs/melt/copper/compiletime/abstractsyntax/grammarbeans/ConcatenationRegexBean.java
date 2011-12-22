package edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammarbeans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umn.cs.melt.copper.compiletime.abstractsyntax.grammarbeans.visitors.RegexBeanVisitor;

/**
 * Holds a concatenation of regexes.
 * @author August Schwerdfeger &lt;<a href="mailto:schwerdf@cs.umn.edu">schwerdf@cs.umn.edu</a>&gt;
 *
 */
public class ConcatenationRegexBean extends RegexBean
{
	/** The regex's constituents. */
	private List<RegexBean> subexps;
	
	/**
	 * Constructs an initially empty concatenation regex. N.B.: A regex must contain at least one element
	 * before it can be included in a terminal.
	 */
	public ConcatenationRegexBean()
	{
		subexps = new ArrayList<RegexBean>();
	}
	
	/**
	 * Constructs a concatenation regex complete with constituents.
	 * @param subexps The regex's constituents.
	 */
	public ConcatenationRegexBean(RegexBean... subexps)
	{
		this.subexps = new ArrayList<RegexBean>(subexps.length);
		for(int i = 0;i < subexps.length;i++) this.subexps.add(subexps[i]);
	}
	
	/**
	 * @see #subexps
	 */
	public List<RegexBean> getSubexps()
	{
		return subexps;
	}

	/**
	 * Alters this concatenation regex to add a sub-expression at the end.
	 * @return {@code this} (to enable chaining of mutator calls).
	 */
	public ConcatenationRegexBean addSubexp(RegexBean subexp)
	{
		subexps.add(subexp);
		return this;
	}
	
	/**
	 * @see #subexps
	 */
	public void setSubexps(List<RegexBean> subexps)
	{
		this.subexps = subexps;
	}
	
	
	@Override
	public boolean isComplete()
	{
		return subexps != null && !subexps.isEmpty();
	}

	@Override
	public Set<String> whatIsMissing()
	{
		Set<String> rv = new HashSet<String>();
		if(subexps == null || subexps.isEmpty()) rv.add("subexps");
		return rv;
	}

	@Override
	public <RT, E extends Exception> RT acceptVisitor(RegexBeanVisitor<RT,E> visitor)
	throws E
	{
		return visitor.visitConcatenationRegex(this);
	}

}
