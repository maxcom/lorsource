/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site.boxes;

import java.io.IOException;

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
                  out.append("Используется профиль: <em>").append(profile.getString("ProfileName")).append("</em><p>");
		out.append("<br><a href=\"edit-profile.jsp\">настройки...</a>");

		out.append("<p><strong>Предустановки:</strong><br>");
		out.append("*<a href=\"edit-profile.jsp?mode=setup&amp;profile=\">по умолчанию</a><br>");
		out.append("*<a href=\"edit-profile.jsp?mode=setup&amp;profile=_white\">тема white</a><br>");
		out.append("*<a href=\"edit-profile.jsp?mode=setup&amp;profile=_white2\">тема white2</a><br>");

		return out.toString();
	}

	public String getInfo() { return "Выбор профиля"; }

	public String getVariantID(ProfileHashtable prof) {
		if (prof.getString("ProfileName")==null)
			return "";
		else
			return "ProfileName="+prof.getString("ProfileName");
	}
}
