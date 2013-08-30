/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.image.ImageInfo;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.image.ImageParam;
import ru.org.linux.util.image.ImageUtil;

import javax.annotation.Nullable;
import java.io.*;
import java.sql.Timestamp;

@Service
public class UserService {
  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  public static final int MAX_USERPIC_FILESIZE = 35000;
  public static final int MIN_IMAGESIZE = 50;
  public static final int MAX_IMAGESIZE = 150;

  @Autowired
  private Configuration configuration;

  public ImageParam checkUserPic(File file) throws UserErrorException, IOException, BadImageException {
    if (!file.isFile()) {
      throw new UserErrorException("Сбой загрузки изображения: не файл");
    }

    ImageParam param = ImageUtil.imageCheck(file);

    if(param.isAnimated()) {
      throw new UserErrorException("Сбой загрузки изображения: анимация не допустима");
    }

    if(param.getSize() > MAX_USERPIC_FILESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: слишком большой файл");
    }


    if (param.getHeight()<MIN_IMAGESIZE || param.getHeight() > MAX_IMAGESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии");
    }

    if (param.getWidth()<MIN_IMAGESIZE || param.getWidth() > MAX_IMAGESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии");
    }

    return param;
  }

  private ImmutableList<Boolean> getStars(User user) {
    ImmutableList.Builder<Boolean> builder = ImmutableList.builder();

    for (int i=0; i<user.getGreenStars(); i++) {
      builder.add(true);
    }

    for (int i=0; i<user.getGreyStars(); i++) {
      builder.add(false);
    }

    return builder.build();
  }

  public ApiUserRef ref(User user, @Nullable User requestUser) {
    Integer score = null;
    Integer maxScore = null;

    if (requestUser!=null && requestUser.isModerator() && !user.isAnonymous()) {
      score = user.getScore();
      maxScore = user.getMaxScore();
    }

    return new ApiUserRef(
            user.getNick(),
            user.isBlocked(),
            user.isAnonymous(),
            getStars(user),
            score,
            maxScore
    );
  }

  public Userpic getUserpic(User user, boolean secure, String avatarStyle, boolean misteryMan) {
    String avatarMode = avatarStyle;

    if (misteryMan && "empty".equals(avatarMode)) {
      avatarMode = "mm";
    }


    if (user.getPhoto() != null) {
      try {
        ImageInfo info = new ImageInfo(configuration.getHTMLPathPrefix() + "/photos/" + user.getPhoto());

        return new Userpic(
            "/photos/" + user.getPhoto(),
            info.getWidth(),
            info.getHeight()
        );
      } catch (FileNotFoundException e) {
        logger.warn("Userpic not found for {}: {}", user.getNick(), e.getMessage());
      } catch (BadImageException | IOException e) {
        logger.warn("Bad userpic for {}", user.getNick(), e);
      }
    }

    if (user.hasGravatar()) {
      return new Userpic(
          user.getGravatar(avatarMode, 150, secure),
          150,
          150
      );
    }

    return new Userpic("/img/p.gif", 1, 1);
  }

  public String getResetCode(String nick, String email, Timestamp tm) {
    String base = configuration.getSecret();

    return StringUtil.md5hash(base + ':' + nick + ':' + email + ':' + Long.toString(tm.getTime()) + ":reset");
  }
}
