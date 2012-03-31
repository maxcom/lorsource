package ru.org.linux.marks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.Comment;
import ru.org.linux.user.User;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

@Service
public class MessageMarksService {
  @Autowired
  private MessageMarksDao marksDao;
  
  public SortedMap<MessageMark, Integer> getMarks(Comment comment) {
    Map<MessageMark,Integer> rawMarks = marksDao.getMessageMarks(comment.getId());

    ImmutableSortedMap.Builder<MessageMark, Integer> builder = ImmutableSortedMap.naturalOrder();

    for (MessageMark mark : MessageMark.values()) {
      Integer count = rawMarks.get(mark);

      if (count==null) {
        builder.put(mark, 0);
      } else {
        builder.put(mark, count);
      }
    }

    return builder.build();
  }
  
  public List<MessageMark> getMarks(Comment comment, User currentUser) {
    if (currentUser==null || currentUser.isAnonymous()) {
      return ImmutableList.of();
    }

    return ImmutableList.copyOf(marksDao.getMarks(currentUser.getId(), comment.getId()));
  }
}
