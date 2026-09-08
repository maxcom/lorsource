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
package ru.org.linux.test

import munit.FunSuite
import org.springframework.transaction.annotation.Transactional

import java.lang.reflect.Method

/** Аналог @Transactional для munit-тестов: каждый тест выполняется внутри Spring-транзакции, которая откатывается после
  * теста (TransactionalTestExecutionListener из spring-test).
  *
  * Транзакция привязана к потоку через ThreadLocal, поэтому тела тестов должны быть синхронными: munit выполняет
  * beforeEach/тело/afterEach синхронного теста в одном потоке.
  *
  * Внутри теста можно использовать org.springframework.test.context.transaction.TestTransaction (например,
  * flagForCommit()).
  *
  * ВНИМАНИЕ: если тест переопределяет beforeEach/afterEach, он обязан вызвать super.beforeEach(context) /
  * super.afterEach(context) — иначе тест молча выполнится без транзакции (каждый localTx закоммитится отдельно,
  * данные утекут между тестами).
  */
@Transactional
trait TransactionalTestSupport extends SpringTestSupport:
  self: FunSuite =>

  // munit-тесты — лямбды, а не методы; TransactionalTestExecutionListener ищет @Transactional
  // от метода к его declaring class, поэтому аннотация стоит на этом трейте, а слушателю
  // передаётся метод, объявленный в нём
  def munitTxLifecycle(): Unit = ()

  private val TxMethod: Method = classOf[TransactionalTestSupport].getDeclaredMethod("munitTxLifecycle")

  override def beforeEach(context: BeforeEach): Unit = testContextManager.beforeTestMethod(this, TxMethod)

  override def afterEach(context: AfterEach): Unit = testContextManager.afterTestMethod(this, TxMethod, null)

end TransactionalTestSupport
