package ru.org.linux.poll;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.site.Message;
import ru.org.linux.spring.dao.PollDao;

@Service
public class PollPrepareService {
  @Autowired
  private PollDao pollDao;

  /**
   * Функция подготовки голосования
   * @param poll голосование
   * @return подготовленное голосование
   */
  public PreparedPoll preparePoll(Poll poll) {
    return new PreparedPoll(
            poll,
            pollDao.getMaxVote(poll),
            pollDao.getCountUsers(poll),
            pollDao.getPollVariants(poll, Poll.ORDER_VOTES)
    );
  }

  /**
   * Функция подготовки голосования
   * @param topic голосование
   * @return подготовленное голосование
   */
  public PreparedPoll preparePoll(Message topic) throws PollNotFoundException {
    Poll poll = pollDao.getPollByTopicId(topic.getId());

    return new PreparedPoll(
            poll,
            pollDao.getMaxVote(poll),
            pollDao.getCountUsers(poll),
            pollDao.getPollVariants(poll, Poll.ORDER_VOTES)
    );
  }
}
