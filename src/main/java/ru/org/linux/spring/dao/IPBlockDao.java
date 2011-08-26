package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.validation.Errors;
import org.xbill.DNS.TextParseException;
import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.IPBlockInfo;
import ru.org.linux.util.DNSBLClient;

import javax.sql.DataSource;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class IPBlockDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  public IPBlockInfo getBlockInfo(String addr) {
    List<IPBlockInfo> list = jdbcTemplate.query(
            "SELECT reason, ban_date, date, mod_id FROM b_ips WHERE ip = ?::inet",
            new RowMapper<IPBlockInfo>() {
              @Override
              public IPBlockInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new IPBlockInfo(rs);
              }
            },
            addr
    );

    if (list.isEmpty()) {
      return null;
    } else {
      return list.get(0);
    }
  }

  public static boolean getTor(String addr) throws TextParseException, UnknownHostException {
    DNSBLClient dnsbl = new DNSBLClient("tor.ahbl.org");
    return (dnsbl.checkIP(addr));
  }

  @Deprecated
  public void checkBlockIP(String addr) throws AccessViolationException, UnknownHostException, TextParseException {
    if (getTor(addr)) {
      throw new AccessViolationException("Постинг заблокирован: tor.ahbl.org");
    }

    IPBlockInfo block = getBlockInfo(addr);

    if (block == null) {
      return;
    }

    block.checkBlock();
  }

  public void checkBlockIP(String addr, Errors errors) throws UnknownHostException, TextParseException {
    if (getTor(addr)) {
      errors.reject(null, "Постинг заблокирован: tor.ahbl.org");
    }

    IPBlockInfo block = getBlockInfo(addr);

    if (block == null) {
      return;
    }

    if (block.isBlocked()) {
      errors.reject(null, "Постинг заблокирован: "+block.getReason());
    }
  }
}
