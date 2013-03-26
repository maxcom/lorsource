package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.Configuration;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ImageInfo;
import ru.org.linux.util.ImageInfo2;
import ru.org.linux.util.StringUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;

@Service
public class UserService {
  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  public static final int MAX_USERPIC_FILESIZE = 35000;
  public static final int MIN_IMAGESIZE = 50;
  public static final int MAX_IMAGESIZE = 150;

  @Autowired
  private Configuration configuration;

  public static void checkUserpic(File file) throws UserErrorException, IOException, BadImageException {
    if (!file.isFile()) {
      throw new UserErrorException("Сбой загрузки изображения: не файл");
    }

    if (file.length() > MAX_USERPIC_FILESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: слишком большой файл");
    }

    String extension = ImageInfo.detectImageType(file);

    ImageInfo info = new ImageInfo(file.getPath(), extension);

    if (info.getHeight()<MIN_IMAGESIZE || info.getHeight() > MAX_IMAGESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии");
    }

    if (info.getWidth()<MIN_IMAGESIZE || info.getWidth() > MAX_IMAGESIZE) {
      throw new UserErrorException("Сбой загрузки изображения: недопустимые размеры фотографии");
    }

    ImageInfo2 ii = new ImageInfo2();
    InputStream is = null;
    try {
      is = new FileInputStream(file);

      ii.setInput(is);
      ii.setDetermineImageNumber(true);

      ii.check();

      if (ii.getNumberOfImages()>1) {
        throw new UserErrorException("Сбой загрузки изображения: анимация не допустима");
      }
    } finally {
      if (is!=null) {
        is.close();
      }
    }
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

  public Userpic getUserpic(User user, boolean secure, String avatarStyle) {
    if (user.getPhoto() != null) {
      try {
        ImageInfo info = new ImageInfo(configuration.getHTMLPathPrefix() + "/photos/" + user.getPhoto());

        return new Userpic(
            "/photos/" + user.getPhoto(),
            info.getWidth(),
            info.getHeight()
        );
      } catch (BadImageException e) {
        logger.warn("Bad userpic for {}", user.getNick());
      } catch (IOException e) {
        logger.warn("Bad userpic for {}", user.getNick());
      }
    }

    if (user.hasGravatar()) {
      return new Userpic(
          user.getGravatar(avatarStyle, 150, secure),
          150,
          150
      );
    }

    return new Userpic("/img/p.gif", 1, 1);
  }

  public static String getResetCode(String base, String nick, String email, Timestamp tm) {
    return StringUtil.md5hash(base + ':' + nick + ':' + email + ':' + Long.toString(tm.getTime()) + ":reset");
  }
}
