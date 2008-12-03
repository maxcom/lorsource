package ru.org.linux.site;

public class BadInputException extends UserErrorException
{
	public BadInputException(String info)
	{
		super(info);
	}

	public BadInputException(Throwable e)
	{
		super("Некорректный ввод: "+e.getMessage());
	}
}