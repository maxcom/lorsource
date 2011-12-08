/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import ru.org.linux.dto.SectionDto;
import ru.org.linux.exception.SectionNotFoundException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class SectionDao {
  private final ImmutableMap<Integer, SectionDto> sections;
  private final ImmutableList<SectionDto> sectionsList;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public SectionDao(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);

    final ImmutableMap.Builder<Integer, SectionDto> sections = ImmutableMap.builder();
    final ImmutableList.Builder<SectionDto> sectionsList = ImmutableList.builder();

    jdbcTemplate.query("SELECT id, name, imagepost, vote, moderate FROM sections ORDER BY id", new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        SectionDto sectionDto = new SectionDto(rs);

        sections.put(sectionDto.getId(), sectionDto);
        sectionsList.add(sectionDto);
      }
    });

    this.sections = sections.build();
    this.sectionsList = sectionsList.build();
  }

  /**
   * Получить объект секции по идентификатору секции.
   *
   * @param id идентификатор секции
   * @return объект секции
   * @throws SectionNotFoundException если секция не найдена
   */
  public SectionDto getSection(int id) throws SectionNotFoundException {
    SectionDto sectionDto = sections.get(id);

    if (sectionDto == null) {
      throw new SectionNotFoundException(id);
    }

    return sectionDto;
  }

  /**
   * получить список секций.
   *
   * @return список секций
   */
  public ImmutableList<SectionDto> getSectionsList() {
    return sectionsList;
  }

  /**
   * Получить расширенную информацию о секции по идентификатору секции.
   *
   * @param id идентификатор секции
   * @return расширеннуя информация о секции
   */
  public String getAddInfo(int id) {
    List<String> infos = jdbcTemplate.queryForList("select add_info from sections where id=?", String.class, id);

    if (infos.isEmpty()) {
      return null;
    } else {
      return infos.get(0);
    }
  }
}
