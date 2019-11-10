/*
 * Copyright 2019 Aleksey Fomkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package korolev.garcon

sealed trait Demand[+Q, +R, +E]

object Demand {
  case class Start[+Q, +R](query: Q, prev: Option[R]) extends Demand[Q, R, Nothing]
  case class Eventually[+R](prev: Option[R]) extends Demand[Nothing, R, Nothing]
  case class Ready[+R](result: R) extends Demand[Nothing, R, Nothing]
  case class Error[E](error: E) extends Demand[Nothing, Nothing, E]
}