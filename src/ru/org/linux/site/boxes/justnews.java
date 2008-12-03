package ru.org.linux.site.boxes;

import java.io.IOException;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.site.config.PropertiesConfig;
import ru.org.linux.util.FileUtils;
import ru.org.linux.util.ProfileHashtable;

public final class justnews extends Boxlet
{
	public String getContentImpl(ProfileHashtable profile) throws IOException
	{
		StringBuffer out=new StringBuffer();

		out.append("<h2><a href=\"http://justnews.ru/\">JustNews</a></h2>");

		out.append(FileUtils.readfile(((PropertiesConfig) config).getProperties().getProperty("PathPrefix")+"linux-fetch/headline-justnews.html"));

		return out.toString();
	}
	
	public String getInfo() { return "Заголовки новостей \"JustNews\""; }
}
