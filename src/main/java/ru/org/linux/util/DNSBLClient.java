/*
 * Copyright 1998-2014 Linux.org.ru
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

package ru.org.linux.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class DNSBLClient {
  public static final int DNSBL_TIMEOUT_SEC = 10;

  private static final Logger logger = LoggerFactory.getLogger(DNSBLClient.class);

  private final Resolver resolver;
  private final String zone;

  public DNSBLClient(String zone) throws UnknownHostException {
    resolver = new ExtendedResolver();
    resolver.setTimeout(DNSBL_TIMEOUT_SEC);
    this.zone = zone;
  }

  public boolean checkIP(String addr) throws TextParseException {
    if (addr.equals("127.0.0.1")) {
      return false;
    }

    String query = invertIPAddress(addr)+ '.' +zone;

    logger.debug("Looking for "+query);

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
      inverted = t.nextToken() + '.' + inverted;
    }

    return inverted;
  }

}

