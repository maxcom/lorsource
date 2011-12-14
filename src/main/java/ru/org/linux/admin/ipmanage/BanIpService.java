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
   * Заблокировать доступ по IP-адресу.
   *
   * @param user      пользователь, осуществляющий блокировку
   * @param ipAddress блокируемый IP-адрес
   * @param reason    причина блокировки
   * @param timestamp время, на которое необходимо заблокировать IP-адрес
   * @throws UserErrorException
   */
  public void doBan(User user, String ipAddress, String reason, Timestamp timestamp)
    throws UserErrorException {

    user.checkCommit();

    ipBlockDao.blockIP(ipAddress, user, reason, timestamp);
  }

  /**
   * Вычисление времени блокировки IP-адреса согласно выбора модератора.
   *
   * @param banPeriodEnum период, на который необходимо заблокировать IP-адрес
   * @param days          количество дней (при варианте периода "CUSTOM")
   * @return время, до которого IP-адрес будет заблокирован (null - постоянно)
   * @throws UserErrorException
   */
  public Timestamp calculateTimestamp(BanPeriodEnum banPeriodEnum, int days)
    throws UserErrorException {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());

    switch (banPeriodEnum) {
      case PERMANENT:
        return null;
      case REMOVE:
        break;
      case CUSTOM:
        if (days <= 0 || days > 180) {
          throw new UserErrorException("Invalid days count");
        }
        calendar.add(Calendar.DAY_OF_MONTH, days);
        break;
      default:
        calendar.add(banPeriodEnum.getCalendarPeriod(), banPeriodEnum.getCalendarNumPeriods());
        break;
    }
    return new Timestamp(calendar.getTimeInMillis());
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
