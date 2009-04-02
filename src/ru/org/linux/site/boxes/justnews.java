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
