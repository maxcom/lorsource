package ru.org.linux.util;

import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.xbill.DNS.*;

public class DNSBLClient {
  public final int DNSBL_TIMEOUT_SEC = 10;

  private final static Logger logger = Logger.getLogger("ru.org.linux");

  private final Resolver resolver;
  private final String zone;

  public DNSBLClient(String zone) throws UnknownHostException {
    resolver = new ExtendedResolver();
    resolver.setTimeout(DNSBL_TIMEOUT_SEC);
    this.zone = zone;
  }

  public boolean checkIP(String addr) throws TextParseException {
    String query = invertIPAddress(addr)+"."+zone;

    logger.fine("Looking for "+query);

    Lookup lookup = new Lookup(query, Type.A);
    lookup.setResolver(resolver);

    Record[] r = lookup.run();

    if (r==null || r.length==0) {
      return false;
    }

    logger.info("DNSBL: found "+addr+" in DNSBL: "+r[0]);

    // TODO: check what we really got ;-)
    return true;
  }

  private static String invertIPAddress(String originalIPAddress) {
    StringTokenizer t = new StringTokenizer(originalIPAddress, ".");
    String inverted = t.nextToken();

    while (t.hasMoreTokens()) {
      inverted = t.nextToken() + "." + inverted;
    }

    return inverted;
  }

}

