package ru.org.linux.site.boxes;

import java.io.IOException;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.util.ProfileHashtable;

public final class projects extends Boxlet
{
	public String getContentImpl(ProfileHashtable profile) throws IOException
	{
		return ("<h2>Проекты</h2> <h3>Наши проекты</h3> * <a href=\"/gnome\">gnome</a><br> * <a href=\"/rc5\">rc5</a><br>");
	}

	public String getInfo() { return "Наши проекты"; }
}
