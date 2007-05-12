package ru.org.linux.site;

public class BadProfileException extends UserErrorException
{
	public BadProfileException(String info)
	{
		super(info);
	}
}