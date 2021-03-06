package com.massivecraft.massivecore.cmd.arg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;

import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.util.Txt;

public abstract class ARAbstractSelect<T> extends ArgReaderAbstract<T>
{
	// -------------------------------------------- //
	// CONSTANT
	// -------------------------------------------- //
	
	public static final int LIST_COUNT_MAX = 50;
	
	// -------------------------------------------- //
	// ABSTRACT
	// -------------------------------------------- //
	
	public abstract String typename();
	public abstract T select(String str, CommandSender sender) throws MassiveException;
	public abstract Collection<String> altNames(CommandSender sender);
	public boolean canList(CommandSender sender) { return true; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public T read(String arg, CommandSender sender) throws MassiveException
	{
		T result = this.select(arg, sender);
		
		if (result != null) return result;
		
		MassiveException exception = new MassiveException();
		exception.addMsg("<b>No %s matches \"<h>%s<b>\".", this.typename(), arg);
		
		if (this.canList(sender))
		{			
			Collection<String> names = this.altNames(sender);
			
			// Try Levenshtein
			List<String> matches = this.getMatchingAltNames(arg, sender, this.getMaxLevenshteinDistanceForArg(arg));
			
			if (names.isEmpty())
			{
				exception.addMsg("<i>Note: There is no %s available.", this.typename());
			}
			else if ( ! matches.isEmpty() && matches.size() < LIST_COUNT_MAX)
			{
				// For some reason the arguments doesn't get parsed.
				String suggest = Txt.parse(Txt.implodeCommaAnd(matches, "<i>, <h>", " <i>or <h>"));
				exception.addMsg("<i>Did you mean <h>%s<i>?", suggest);
			}
			else if (names.size() > LIST_COUNT_MAX)
			{
				exception.addMsg("<i>More than %d alternatives available.", LIST_COUNT_MAX);
			}
			else
			{
				String format = Txt.parse("<h>%s");
				String comma = Txt.parse("<i>, ");
				String and = Txt.parse(" <i>or ");
				String dot = Txt.parse("<i>.");
				exception.addMsg("<i>Use %s", Txt.implodeCommaAndDot(names, format, comma, and, dot));
			}
		}
			
		throw exception;
	}
	
	public List<String> getMatchingAltNames(String arg, CommandSender sender, int maxLevenshteinDistance)
	{
		arg = arg.toLowerCase();
		
		// Try Levenshtein
		List<String> matches = new ArrayList<String>();
		
		for (String alias : this.altNames(sender))
		{
			String aliaslc = alias.toLowerCase();
			int distance = StringUtils.getLevenshteinDistance(arg, aliaslc);
			if (distance > maxLevenshteinDistance) continue;
			matches.add(alias);
		}
		return matches;
	}
	
	public int getMaxLevenshteinDistanceForArg(String arg)
	{
		if (arg.length() <= 1) return 0; // When dealing with 1 character aliases, there is way too many options.
		if (arg.length() < 8) return 1; // 1 is default.
		
		return 2;  // If it were 8 characters or more, we end up here. Because many characters allow for more typos.
	}
	
}
