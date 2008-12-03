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
