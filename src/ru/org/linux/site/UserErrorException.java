package ru.org.linux.site;

public class UserErrorException extends Exception
{
	public UserErrorException()
	{
		super("неизвестная пользовательская ошибка");
	}

	public UserErrorException(String info)
	{
		super(info);
	}

}