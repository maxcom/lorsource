package ru.org.linux.boxlet;

public class BoxletLoadException extends BoxletException
{
	BoxletLoadException (Throwable e)
	{
		super("can't load boxlet: "+e.toString());
	}

}
