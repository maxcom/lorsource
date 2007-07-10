package ru.org.linux.site.boxes;

import java.io.IOException;
import java.util.Properties;

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.util.ProfileHashtable;

public final class profile extends Boxlet
{
	public String getContentImpl(ProfileHashtable profile) throws IOException {
		StringBuffer out=new StringBuffer();

		out.append("<h2>Выбор профиля</h2>");
		if (profile.getString("ProfileName")==null)
			out.append("Используется профиль по-умолчанию<p>");
		else
			out.append("Используется профиль: <em>"+profile.getString("ProfileName")+"</em><p>");
		out.append("<br><a href=\"edit-profile.jsp\">настройки...</a>");

		out.append("<p><strong>Предустановки:</strong><br>");
		out.append("*<a href=\"edit-profile.jsp?mode=setup&amp;profile=\">по умолчанию</a><br>");
		out.append("*<a href=\"edit-profile.jsp?mode=setup&amp;profile=_white\">тема white</a><br>");
		out.append("*<a href=\"edit-profile.jsp?mode=setup&amp;profile=_white2\">тема white2</a><br>");

		return out.toString();
	}

	public String getInfo() { return "Выбор профиля"; }

	public String getVariantID(ProfileHashtable prof, Properties request) {
		if (prof.getString("ProfileName")==null)
			return "";
		else
			return "ProfileName="+prof.getString("ProfileName");
	}
}
