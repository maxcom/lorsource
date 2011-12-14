/*
 * Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.admin.ipmanage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.xbill.DNS.TextParseException;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.util.DNSBLClient;

import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Service
public class BanIpService {

  @Autowired
  private IPBlockDao ipBlockDao;

  /**
   * @param user
   * @param ip
   * @param reason
   * @param banPeriodEnum
   * @param days
   * @throws UserErrorException
   */
  public void doBan(User user, String ip, String reason, BanPeriodEnum banPeriodEnum, int days)
    throws UserErrorException {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());

    switch (banPeriodEnum) {
      case HOUR_1:
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        break;
      case DAY_1:
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        break;
      case MONTH_1:
        calendar.add(Calendar.MONTH, 1);
        break;
      case MONTH_3:
        calendar.add(Calendar.MONTH, 3);
        break;
      case MONTH_6:
        calendar.add(Calendar.MONTH, 6);
        break;
      case CUSTOM:
        if (days <= 0 || days > 180) {
          throw new UserErrorException("Invalid days count");
        }
        calendar.add(Calendar.DAY_OF_MONTH, days);
        break;
      default:
        break;
    }
    Timestamp ts = (BanPeriodEnum.PERMANENT.equals(banPeriodEnum))
      ? null
      : new Timestamp(calendar.getTimeInMillis());

    user.checkCommit();

    ipBlockDao.blockIP(ip, user, reason, ts);
  }

  public boolean getTor(String addr) throws TextParseException, UnknownHostException {
    DNSBLClient dnsbl = new DNSBLClient("tor.ahbl.org");
    return (dnsbl.checkIP(addr));
  }

  public void checkBlockIP(String addr, Errors errors) throws UnknownHostException, TextParseException {
    if (getTor(addr)) {
      errors.reject(null, "Постинг заблокирован: tor.ahbl.org");
    }

    IPBlockInfo block = ipBlockDao.getBlockInfo(addr);

    if (block == null) {
      return;
    }

    if (block.isBlocked()) {
      errors.reject(null, "Постинг заблокирован: " + block.getReason());
    }
  }

}
