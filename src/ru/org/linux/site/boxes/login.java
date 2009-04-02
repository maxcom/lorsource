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

import ru.org.linux.boxlet.Boxlet;
import ru.org.linux.util.ProfileHashtable;

public final class login extends Boxlet
{
	public String getContentImpl(ProfileHashtable profile)
	{
		StringBuffer out=new StringBuffer();

		out.append("<h2>Login</h2>"); 
		out.append("<form method=POST action=\"login.jsp\"><table><tr><td>Nick:</td><td><input type=text name=nick size=9></td></tr><tr><td>Пароль:</td><td><input type=password name=passwd size=9></td></tr></table><input type=checkbox name=profile checked> востановить профиль<br><input type=submit value=\"Login\"></form><br><form method=POST action=\"logout.jsp\"><input type=submit value=\"Logout\"></form><a href=\"register.jsp\">регистрация...</a>");
		return out.toString();

	}
	
	public String getInfo() { return "Выбор профиля"; }
}
