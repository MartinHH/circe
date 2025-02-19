/*
 * Copyright 2023 circe
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

package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{ Decoder, DecodingFailure, HCursor }

trait ConfiguredEnumDecoder[A] extends Decoder[A]
object ConfiguredEnumDecoder:
  inline final def derived[A](using conf: Configuration)(using mirror: Mirror.SumOf[A]): ConfiguredEnumDecoder[A] =
    val cases = summonSingletonCases[mirror.MirroredElemTypes, A](constValue[mirror.MirroredLabel])
    val labels = summonLabels[mirror.MirroredElemLabels].toArray.map(conf.transformConstructorNames)
    new ConfiguredEnumDecoder[A]:
      def apply(c: HCursor): Decoder.Result[A] =
        c.as[String].flatMap { caseName =>
          labels.indexOf(caseName) match
            case -1 =>
              Left(
                DecodingFailure(s"enum ${constValue[mirror.MirroredLabel]} does not contain case: $caseName", c.history)
              )
            case index => Right(cases(index))
        }

  inline final def derive[R: Mirror.SumOf](
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames
  ): Decoder[R] =
    derived[R](using Configuration.default.withTransformConstructorNames(transformConstructorNames))
