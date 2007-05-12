package ru.org.linux.storage;

public class StorageExistsException extends StorageException
{
	public StorageExistsException(String domain, int msgid)
	{
		super("Объект "+domain+ ':' +msgid+" уже существует");
	}

	public StorageExistsException(String domain, String msgid)
	{
		super("Объект "+domain+ ':' +msgid+" уже существует");
	}
}