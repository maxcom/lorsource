package ru.org.linux.site;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import ru.org.linux.util.UtilException;

public class CommentViewer {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  public static final int COMMENTS_INITIAL_BUFSIZE = 50;

  private final Template tmpl;
  private final CommentList comments;
  private final boolean expired;

  private final String user;

  public CommentViewer(Template t, CommentList comments, String user, boolean expired) {
    tmpl=t;
    this.comments = comments;
    this.user=user;
    this.expired = expired;
  }

  private void showCommentList(StringBuffer buf, List<Comment> comments, boolean reverse, int offset, int limit, Set<Integer> hideSet)
      throws IOException, UtilException {
    int shown = 0;

    for (ListIterator<Comment> i = comments.listIterator(reverse?comments.size():0); reverse?i.hasPrevious():i.hasNext();) {
      int index = reverse?(comments.size()-i.previousIndex()):i.nextIndex();

      Comment comment = reverse?i.previous():i.next();

      if (index<offset || (limit!=0 && index>=offset+limit)) {
        continue;
      }

      if (hideSet==null || !hideSet.contains(comment.getMessageId())) {
        shown++;
        buf.append(comment.printMessage(tmpl, this.comments, true, tmpl.isModeratorSession(), user, expired));
      }
    }

    logger.fine("Showing list size="+comments.size()+" shown="+shown);    
  }

  public String showAll(boolean reverse, int offset, int limit) throws IOException, UtilException {
    StringBuffer buf=new StringBuffer();

    showCommentList(buf, comments.getList(), reverse, offset, limit,  null);

    return buf.toString();
  }

  public String showFiltered(boolean reverse, int offset, int limit) throws IOException, UtilException {
    StringBuffer buf=new StringBuffer();
    Set<Integer> hideSet = new HashSet<Integer>();

    /* hide anonymous */
    comments.getRoot().hideAnonymous(hideSet);

    /* display comments */
    showCommentList(buf, comments.getList(), reverse, offset, limit, hideSet);

    return buf.toString();
  }

  public String showSubtree(int parentId) throws IOException, UtilException, MessageNotFoundException {
    StringBuffer buf=new StringBuffer();

    CommentNode parentNode = comments.getNode(parentId);

    if (parentNode==null) {
      throw new MessageNotFoundException(parentId);
    }

    List<Comment> parentList = new ArrayList<Comment>();
    parentNode.buildList(parentList);

    /* display comments */
    showCommentList(buf, parentList, false, 0, 0, null);

    return buf.toString();
  }
}
