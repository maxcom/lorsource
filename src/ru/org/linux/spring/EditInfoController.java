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

package ru.org.linux.spring;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import ru.org.linux.site.EditInfoDTO;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Message;
import ru.org.linux.site.PreparedEditInfo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class EditInfoController {

  @RequestMapping({
    "/news/{group}/{id}/history",
    "/forum/{group}/{id}/history",
    "/gallery/{group}/{id}/history",
    "/polls/{group}/{id}/history"
})
  public ModelAndView showEditInfo(
    @PathVariable("id") int msgid
  ) throws Exception {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      ModelAndView mv = new ModelAndView("history");

      Message message = new Message(db, msgid);

      List<EditInfoDTO> editInfoDTOs = message.loadEditInfo(db);
      List<PreparedEditInfo> editInfos = new ArrayList<PreparedEditInfo>(editInfoDTOs.size());

      String currentMessage = message.getMessage();
      String currentTitle = message.getTitle();

      for (int i = 0; i<editInfoDTOs.size(); i++) {
        EditInfoDTO dto = editInfoDTOs.get(i);

        editInfos.add(
          new PreparedEditInfo(
            db,
            dto,
            dto.getOldmessage()!=null ? currentMessage : null,
            dto.getOldtitle()!=null ? currentTitle : null,
            i==0,
            false
          )
        );

        if (dto.getOldmessage() !=null) {
          currentMessage = dto.getOldmessage();
        }

        if (dto.getOldtitle() != null) {
          currentTitle = dto.getOldtitle();
        }
      }

      if (!editInfoDTOs.isEmpty()) {
        EditInfoDTO current = new EditInfoDTO();
        current.setOldmessage(message.getMessage());
        current.setEditdate(message.getPostdate());
        current.setEditor(message.getUid());
        current.setMsgid(message.getMessageId());

        editInfos.add(new PreparedEditInfo(db, current, currentMessage, currentTitle, false, true));
      }

      mv.getModel().put("message", message);
      mv.getModel().put("editInfos", editInfos);

      return mv;
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

}
