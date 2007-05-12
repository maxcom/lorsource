package ru.org.linux.util;

import javax.servlet.ServletRequest;
import gnu.regexp.RE;
import gnu.regexp.REException;

public class ServletParameterParser {
  private static final RE ipRE;

  static {
    try {
      ipRE = new RE("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");
    } catch (REException ex) {
      throw new RuntimeException(ex);
    }
  }

	private final ServletRequest rq;

	public ServletParameterParser(ServletRequest request) {
		rq=request;
	}

	public String getString(String name) throws ServletParameterException {
		if (name==null)
			throw new ServletParameterBadNameException(name);

		String value=rq.getParameter(name);

		if (value==null)
			throw new ServletParameterMissingException(name);

		return value;
	}

	public int getInt(String name) throws ServletParameterException {
		if (name==null)
			throw new ServletParameterBadNameException(name);

		String svalue=rq.getParameter(name);

		if (svalue==null)
			throw new ServletParameterMissingException(name);
		
		int value;
		
		try {
			value=Integer.parseInt(svalue);
		} catch (NumberFormatException e) {
			throw new ServletParameterBadValueException(name, e);
		}

		return value;
	}

	public boolean getBoolean(String name) throws ServletParameterException {
		int value=getInt(name);

		switch (value) {
			case 0: return false;
			case 1: return true;
			default: throw new ServletParameterBadValueException(name, "not boolean");
		}
	}

  public String getIP(String name) throws ServletParameterException {
    String ip = getString(name);

    if (ipRE.getMatch(ip)==null) {
      throw new ServletParameterBadValueException(name, "not ip");
    }

    return ip;
  }
}
