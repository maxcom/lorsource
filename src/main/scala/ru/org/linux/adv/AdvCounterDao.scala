/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.adv

import org.springframework.stereotype.Repository
import ru.org.linux.scalikejdbc.SpringDB
import scalikejdbc.*

@Repository
class AdvCounterDao(springDB: SpringDB):
  def count(path: String, increment: Long): Unit =
    springDB.run:
      sql"""INSERT INTO adv_counts (path, day, counter) VALUES ($path, CURRENT_DATE, $increment)
            ON CONFLICT (path, day) DO UPDATE SET counter = adv_counts.counter + excluded.counter""".update.apply()
