package ru.org.linux.util;

public class UtilBadURLException extends UtilException {
	UtilBadURLException() {
		super("Некорректный URL");
	}

	UtilBadURLException(String URL) {
		super("Некорректный URL: "+URL);
	}
}
