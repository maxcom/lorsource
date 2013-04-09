/*
 * Copyright 1998-2012 Linux.org.ru
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

import edu.vt.middleware.dictionary.ArrayWordList;
import edu.vt.middleware.dictionary.WordListDictionary;
import edu.vt.middleware.dictionary.WordLists;
import edu.vt.middleware.dictionary.sort.ArraysSort;
import edu.vt.middleware.password.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Service
public class PasswordVerify {
  private static final Log logger = LogFactory.getLog(PasswordVerify.class);
  private static final int MIN_PASSWORD_LEN = 4;

  private PasswordValidator validator;

  @Autowired
  private Configuration configuration;

  @PostConstruct
  public void init() {
    DictionarySubstringRule dictRule = null;
    try {
      if(configuration.getCrackDictionaryPath() != null) {
        FileReader reader[] = {new FileReader(configuration.getCrackDictionaryPath())};
        ArrayWordList awl = WordLists.createFromReader(reader, true, new ArraysSort());
        WordListDictionary dict = new WordListDictionary(awl);
        dictRule = new DictionarySubstringRule(dict);
        dictRule.setWordLength(4);
        dictRule.setMatchBackwards(true);
      }
    } catch (FileNotFoundException e) {
      logger.warn("File not found:" + configuration.getCrackDictionaryPath());
    } catch (IOException e) {
      logger.warn("Error read:" + configuration.getCrackDictionaryPath());
    }

    // password must be between 5 and 100 chars long
    LengthRule lengthRule = new LengthRule(MIN_PASSWORD_LEN, 100);
    // don't allow whitespace
    WhitespaceRule whitespaceRule = new WhitespaceRule();
    // control allowed characters
    CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
    // require at least 1 digit in passwords
    charRule.getRules().add(new DigitCharacterRule(1));
    // require at least 1 non-alphanumeric char
    charRule.getRules().add(new NonAlphanumericCharacterRule(1));
    // require at least 1 upper case char
    charRule.getRules().add(new UppercaseCharacterRule(1));
    // require at least 1 lower case char
    charRule.getRules().add(new LowercaseCharacterRule(1));
    // require at least 3 of the previous rules be met
    charRule.setNumberOfCharacteristics(3);
    // don't allow qwerty sequences
    QwertySequenceRule qwertySeqRule = new QwertySequenceRule();


    List<Rule> ruleList = new ArrayList<Rule>();
    ruleList.add(lengthRule);
    ruleList.add(whitespaceRule);
    ruleList.add(charRule);
    ruleList.add(qwertySeqRule);
    if(dictRule != null) {
      ruleList.add(dictRule);
    }
    validator = new PasswordValidator(ruleList);
  }

  public RuleResult validate(String password) {
    return validator.validate(new PasswordData(new Password(password)));
  }

  public List<String> getMessages(RuleResult result) {
    return validator.getMessages(result);
  }
}
