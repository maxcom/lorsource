package ru.org.linux.boxlet;

public class BoxletBadNameException extends BoxletException {
	public BoxletBadNameException(String name)
	{
		super("bad boxlet name: " + name);
	}
}
