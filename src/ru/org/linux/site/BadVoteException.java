package ru.org.linux.site;

public class BadVoteException extends ScriptErrorException
{
	public BadVoteException(String info)
	{
		super(info);
	}

	public BadVoteException(int id, int vote)
	{
		super("Неверный id опроса");
	}
}