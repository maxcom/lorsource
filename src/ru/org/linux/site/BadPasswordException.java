package ru.org.linux.site;

public class BadPasswordException extends UserErrorException
{
	public BadPasswordException(String name)
	{
		super("Пароль для пользователя \""+name+"\" задан неверно");
	}
}